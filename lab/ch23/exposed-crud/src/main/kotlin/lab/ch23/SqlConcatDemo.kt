package lab.ch23

import io.r2dbc.spi.Row
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

/**
 * §23.2 现场复盘：字符串拼接 SQL 的注入与断链风险（只读演示，三发全是 SELECT）。
 * exec 是 R2dbcTransaction 的事务内原生执行通道：传 SQL 字符串 + Row 映射器，
 * 返回 Flow<T>。它与 DSL 的本质差别在于——SQL 里是什么就执行什么，
 * 这串字符是谁拼出来的、用户输入有没有混进去，它一概不问。
 */
suspend fun sqlConcatDemo(db: R2dbcDatabase) {
  println("== 发 1｜正常输入：按书名精确匹配 ==")
  // 硬编码模拟「用户输入」：真实场景里这个值来自表单、URL 参数或请求体
  val normalInput = "Kotlin 实战"
  val sql1 = "SELECT id, title, author FROM books WHERE title = '$normalInput'"
  println("拼接 SQL: $sql1")
  suspendTransaction(db) {
    val matched = exec(sql1) { row -> rowToLine(row) }!!.toList()
    println("返回 ${matched.size} 行:")
    matched.forEach { println("  $it") }
  }

  println("== 发 2｜注入输入：恒真条件把整表拉出 ==")
  // 攻击载荷：用单引号闭合书名引号，再接 OR '1'='1' 恒真——书名框里塞进来的是 SQL
  val injectedInput = "x' OR '1'='1"
  val sql2 = "SELECT id, title, author FROM books WHERE title = '$injectedInput'"
  println("拼接 SQL: $sql2")
  suspendTransaction(db) {
    val leaked = exec(sql2) { row -> rowToLine(row) }!!.toList()
    println("返回 ${leaked.size} 行（整表被拉出）:")
    leaked.forEach { println("  $it") }
  }

  println("== 发 3｜断链：拼接引用不存在的列（列已改名、拼接代码没人改） ==")
  // books 表的作者列叫 author；此处故意拼一个不存在的 name 列，模拟改名后断链。
  // 发 3 单独开事务：语句一失败 PG 即判事务中止，整块回滚（失败本身就是教学点），
  // 异常抛到块外捕获打印，保证演示可复跑；若在事务内捕获，中止态会拖到 commit 才炸。
  val sql3 = "SELECT id, title, author FROM books WHERE name = '$normalInput'"
  println("拼接 SQL: $sql3")
  try {
    suspendTransaction(db) {
      exec(sql3) { row -> rowToLine(row) }!!.toList()
      println("不应到达：断链查询不该成功")
    }
  } catch (e: Exception) {
    println("异常类型: ${e::class.simpleName}")
    println("数据库错误消息: ${e.message}")
  }
}

/** Row → 单行展示串：id: title / author。 */
private fun rowToLine(row: Row): String =
    "${row.get("id", Int::class.javaObjectType)}: ${row.get("title", String::class.java)} / " +
        row.get("author", String::class.java)
