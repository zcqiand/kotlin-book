package lab.ch23

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.deleteAll
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update

/**
 * ch23 实测主程序：五个子命令各演示一件事。
 *   init-db  建（若缺）章级验证库 kotlin_book_ch23_verify
 *   schema   打印建表 DDL 并执行 SchemaUtils.create
 *   crud     insert → select → update → delete 四连，逐行打印结果
 *   tx       事务边界：回滚 vs 提交对照
 *   reverse  故意触发唯一约束违反（复现反例用）
 *   sqlconcat SQL 拼接字符串演示：正常 / 注入 / 断链三发（§23.2 补测）
 *   no-tx    事务外执行 DSL：复现 IllegalStateException（§23.2 补测核查）
 *   create-db-tx 事务内 CREATE DATABASE：复现 PG 25001 拒绝（§23.2 补测核查）
 */
fun main(args: Array<String>) = runBlocking {
  when (args.firstOrNull()) {
    "init-db" -> initDb()
    "schema" -> createSchema()
    "crud" -> crudDemo()
    "tx" -> txDemo()
    "reverse" -> reverseDemo()
    "sqlconcat" -> sqlConcatEntry()
    "no-tx" -> noTxDemo()
    "create-db-tx" -> createDbInTxDemo()
    else -> error("用法: run <init-db|schema|crud|tx|reverse|sqlconcat|no-tx|create-db-tx>")
  }
}

private suspend fun initDb() {
  val result = ensureVerifyDatabase()
  println(if (result == "created") "数据库 $PG_VERIFY_DB 已创建" else "数据库 $PG_VERIFY_DB 已存在，跳过创建")
}

private suspend fun createSchema() {
  val db = connectVerify()
  suspendTransaction(db) {
    println("建表 DDL: " + SchemaUtils.createStatements(BooksTable).joinToString(" "))
    SchemaUtils.create(BooksTable)
    println("books 表已建立（已存在则幂等跳过）")
  }
}

private suspend fun crudDemo() {
  val db = connectVerify()
  suspendTransaction(db) {
    // 先清场，让输出可复现
    BooksTable.deleteAll()
    println("== insert ==")
    val kotlinId = BooksTable.insert {
      it[title] = "Kotlin 实战"
      it[author] = "林牧"
      it[year] = 2024
    } get BooksTable.id
    val ktorId = BooksTable.insert {
      it[title] = "Ktor 服务端开发"
      it[author] = "沈砚"
      it[year] = 2025
    } get BooksTable.id
    BooksTable.insert {
      it[title] = "数据访问模式"
      it[author] = "周临"
      it[year] = 2020
    }
    println("写入 3 行，自增 id: $kotlinId, $ktorId")

    println("== select (year >= 2023) ==")
    BooksTable.selectAll()
        .where { BooksTable.year greaterEq 2023 }
        .toList()
        .forEach { row ->
          println("  ${row[BooksTable.id]}: ${row[BooksTable.title]} / ${row[BooksTable.author]} / ${row[BooksTable.year]}")
        }

    println("== update (2020 → 2022) ==")
    val updated = BooksTable.update({ BooksTable.title eq "数据访问模式" }) {
      it[year] = 2022
    }
    println("更新 $updated 行")

    println("== delete (Ktor 服务端开发) ==")
    val deleted = BooksTable.deleteWhere { BooksTable.title eq "Ktor 服务端开发" }
    println("删除 $deleted 行")
    println("剩余 ${BooksTable.selectAll().toList().size} 行: " +
        BooksTable.selectAll().toList().joinToString { it[BooksTable.title] })
  }
}

/** 回滚分支：事务内抛异常 → 整块不落库；正常分支：无异常 → 落库。 */
private suspend fun txDemo() {
  val db = connectVerify()
  suspendTransaction(db) { BooksTable.deleteAll() }

  runCatching {
    suspendTransaction(db) {
      BooksTable.insert {
        it[title] = "本该回滚的书"
        it[author] = "佚名"
        it[year] = 1900
      }
      error("事务中途炸了：这行不该留下来")
    }
  }
  val afterRollback = suspendTransaction(db) { BooksTable.selectAll().toList().size }
  println("回滚分支：事务内写入后抛异常，表内行数 = $afterRollback（期望 0）")

  suspendTransaction(db) {
    BooksTable.insert {
      it[title] = "正常落库的书"
      it[author] = "林牧"
      it[year] = 2024
    }
  }
  val afterCommit = suspendTransaction(db) { BooksTable.selectAll().toList().size }
  println("提交分支：无异常正常退出事务，表内行数 = $afterCommit（期望 1）")
}

/** §23.2 补测入口：先幂等播种「Kotlin 实战」，再跑拼接三发。 */
private suspend fun sqlConcatEntry() {
  val db = connectVerify()
  suspendTransaction(db) {
    val seeded = BooksTable.selectAll()
        .where { BooksTable.title eq "Kotlin 实战" }
        .toList()
        .isNotEmpty()
    if (!seeded) {
      BooksTable.insert {
        it[title] = "Kotlin 实战"
        it[author] = "林牧"
        it[year] = 2024
      }
      println("播种：已插入种子行「Kotlin 实战」")
    } else {
      println("播种：种子行「Kotlin 实战」已在，跳过")
    }
    println("books 表当前 ${BooksTable.selectAll().toList().size} 行")
  }
  sqlConcatDemo(db)
}

/** 顺带核查 1：事务外执行 DSL——复现 IllegalStateException: No transaction in context. */
private suspend fun noTxDemo() {
  val db = connectVerify()
  try {
    // 故意不包 suspendTransaction，直接在裸协程上下文里调 DSL
    BooksTable.deleteAll()
    println("不应到达：事务外执行不该成功")
  } catch (e: Exception) {
    println("异常类型: ${e::class.qualifiedName}")
    println("异常消息: ${e.message}")
  }
}

/** 顺带核查 2：事务内 CREATE DATABASE——复现 PG [25001] cannot run inside a transaction block。 */
private suspend fun createDbInTxDemo() {
  val db = connectVerify()
  try {
    suspendTransaction(db) {
      exec("CREATE DATABASE $PG_VERIFY_DB")
    }
    println("不应到达：事务内 CREATE DATABASE 不该成功")
  } catch (e: Exception) {
    println("异常类型: ${e::class.qualifiedName}")
    println("异常消息: ${e.message}")
  }
}

/** 复绿版：插入前查重，同题不再触发约束违反。 */
private suspend fun reverseDemo() {
  val db = connectVerify()
  suspendTransaction(db) {
    val exists = BooksTable.selectAll()
        .where { BooksTable.title eq "唯一性反例" }
        .toList()
        .isNotEmpty()
    if (!exists) {
      BooksTable.insert {
        it[title] = "唯一性反例"
        it[author] = "林牧"
        it[year] = 2024
      }
      println("首次插入成功")
    } else {
      println("同题已存在，跳过插入（反例代码已移出，恢复绿态）")
    }
  }
}
