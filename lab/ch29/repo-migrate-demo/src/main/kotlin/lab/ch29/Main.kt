package lab.ch29

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

/**
 * ch29 实测主程序：数据地基一条龙。
 *   init-db  建（若缺）章级验证库 kotlin_book_ch29_verify
 *   migrate  按版本清单应用迁移，schema_migration 登记台账
 *   seed     种子 JSON upsert 灌入（幂等可重跑）
 *   smoke    仓储读写 + 事务回滚五项冒烟 + 清场
 *   all      init-db → migrate → seed → smoke 一条龙
 */
fun main(args: Array<String>) = runBlocking {
  val db = connectVerify()
  val repository = ExposedSpecialtyRepository(db)
  when (args.firstOrNull()) {
    "init-db" -> initDb()
    "migrate" -> Migrator(db).migrate()
    "seed" -> SeedLoader(repository).seed()
    "smoke" -> smoke(db, repository)
    "all" -> {
      initDb()
      Migrator(db).migrate()
      SeedLoader(repository).seed()
      smoke(db, repository)
    }
    else -> error("用法: run <init-db|migrate|seed|smoke|all>")
  }
}

private suspend fun initDb() {
  val result = ensureVerifyDatabase()
  println(if (result == "created") "数据库 $PG_VERIFY_DB 已创建" else "数据库 $PG_VERIFY_DB 已存在，跳过创建")
}

private suspend fun smoke(db: R2dbcDatabase, repository: SpecialtyRepository) {
  println("== 冒烟 1｜仓储读：findAll ==")
  println("[冒烟1] findAll = ${repository.findAll().size} 行（期望 3）")

  println("== 冒烟 2｜仓储读：findByCode ==")
  println("[冒烟2] findByCode(STRC) = ${repository.findByCode("STRC")?.name}（期望 混凝土结构）")

  println("== 冒烟 3｜仓储写：upsert 新增专业 ==")
  repository.upsert(Specialty(
      code = "SMOKE",
      officialNo = "GB 99999-2026",
      name = "冒烟测试专用",
      isOfficial = false,
      enabled = false,
      sortOrder = 99,
  ))
  println("[冒烟3] upsert 后 findAll = ${repository.findAll().size} 行（期望 4）")

  println("== 冒烟 4｜仓储写：setEnabled 停用再启用 ==")
  repository.setEnabled("STEL", false)
  val disabled = repository.findByCode("STEL")?.enabled
  repository.setEnabled("STEL", true)
  val reEnabled = repository.findByCode("STEL")?.enabled
  println("[冒烟4] STEL enabled: $disabled → $reEnabled（期望 false → true）")

  println("== 冒烟 5｜事务边界：中途失败整块回滚 ==")
  runCatching {
    suspendTransaction(db) {
      InspectionSpecialtiesTable.insert {
        it[code] = "ROLLBACK_ROW"
        it[officialNo] = "无"
        it[name] = "本该回滚的行"
        it[isOfficial] = false
        it[enabled] = false
        it[sortOrder] = 999
        it[createdAt] = "1970-01-01 00:00:00"
        it[updatedAt] = "1970-01-01 00:00:00"
      }
      error("中途失败：ROLLBACK_ROW 不该留下来")
    }
  }
  println("[冒烟5] 回滚后 findByCode(ROLLBACK_ROW) = ${repository.findByCode("ROLLBACK_ROW")}（期望 null）")

  println("== 清场 ==")
  suspendTransaction(db) {
    InspectionSpecialtiesTable.deleteWhere { InspectionSpecialtiesTable.code eq "SMOKE" }
  }
  println("[清场] 冒烟行已删，findAll = ${repository.findAll().size} 行（期望 3）")
}
