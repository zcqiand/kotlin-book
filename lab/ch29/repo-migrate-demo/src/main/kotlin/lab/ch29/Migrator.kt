package lab.ch29

import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

/** 迁移版本登记表：一张小表记录哪个版本已经上过库。 */
object MigrationTable : Table("schema_migration") {
  val version = text("version")
  val appliedAt = text("applied_at")
  override val primaryKey = PrimaryKey(version)
}

/**
 * 迁移器：版本化脚本管理结构演进。
 * 版本清单显式登记、按序应用；已应用的版本查登记表跳过（幂等可重跑）。
 */
class Migrator(private val db: R2dbcDatabase) {

  private val versions = listOf(
      "V001__init.sql",
      "V002__test_records_reviewer.sql",
  )

  suspend fun migrate() {
    suspendTransaction(db) { SchemaUtils.create(MigrationTable) }
    for (file in versions) {
      if (isApplied(file)) {
        println("迁移 $file：已应用，跳过")
        continue
      }
      apply(file)
    }
  }

  private suspend fun isApplied(version: String): Boolean = suspendTransaction(db) {
    MigrationTable.selectAll()
        .where { MigrationTable.version eq version }
        .toList()
        .isNotEmpty()
  }

  private suspend fun apply(file: String) {
    val sql = javaClass.getResourceAsStream("/db/migration/$file")
        ?.bufferedReader(Charsets.UTF_8)?.readText()
        ?: error("迁移脚本缺失：/db/migration/$file")
    val statements = sql.lines()
        .filterNot { it.trimStart().startsWith("-->") } // 分节注释不进执行队列
        .joinToString("\n")
        .split(";")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    suspendTransaction(db) {
      statements.forEach { stmt ->
        exec(stmt) { true }?.toList() // DDL 无结果集，收空流即可
      }
      MigrationTable.insert {
        it[version] = file
        it[appliedAt] = java.time.LocalDateTime.now().toString().take(19).replace("T", " ")
      }
    }
    println("迁移 $file：应用成功（${statements.size} 条语句）")
  }
}
