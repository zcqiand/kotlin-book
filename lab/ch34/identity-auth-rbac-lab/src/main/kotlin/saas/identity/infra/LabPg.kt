// 本机 PG 接入（连接纪律沿第 23/29/33 章）：127.0.0.1:54321，postgres/postgres，
// 章级隔离验证库 kotlin_book_ch34_verify（共享 PG 100.79.128.25 全程零触碰）。
package saas.identity.infra

import io.r2dbc.spi.ConnectionFactories
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import reactor.core.publisher.Mono
import saas.identity.entity.AuthCodeTable
import saas.identity.entity.RefreshTokenTable
import saas.identity.entity.SysMenuTable
import saas.identity.entity.SysRoleMenuTable
import saas.identity.entity.SysRoleTable
import saas.identity.entity.TenantMemberRoleTable

const val PG_HOST = "127.0.0.1"
const val PG_PORT = "54321"
const val PG_USER = "postgres"
const val PG_PASSWORD = "postgres"
const val PG_VERIFY_DB = "kotlin_book_ch34_verify"

fun verifyUrl() = "r2dbc:postgresql://$PG_USER:$PG_PASSWORD@$PG_HOST:$PG_PORT/$PG_VERIFY_DB"

fun connectVerify(): R2dbcDatabase = R2dbcDatabase.connect(verifyUrl())

/**
 * 建（若缺）验证库。CREATE DATABASE 不能在事务块里跑（autoCommit 必须 on），
 * 走裸 R2DBC SPI（ch29 Db.kt 同款，ch33 LabPg 镜像）。返回 "created"（新建）或 "exists"（已存在）。
 */
fun ensureVerifyDatabase(): String {
  val maintenanceUrl = "r2dbc:postgresql://$PG_USER:$PG_PASSWORD@$PG_HOST:$PG_PORT/postgres"
  val factory = ConnectionFactories.get(maintenanceUrl)
  val conn = Mono.from(factory.create()).block() ?: error("维护库连接失败")
  try {
    Mono.from(conn.createStatement("CREATE DATABASE $PG_VERIFY_DB").execute()).block()
    return "created"
  } catch (e: Exception) {
    if (e.message?.contains("already exists") == true) return "exists"
    throw e
  } finally {
    conn.close()
  }
}

/** 表重建：lab 每轮全量重来（先 drop 后 create），测试可重入。 */
suspend fun resetSchema(db: R2dbcDatabase) {
  suspendTransaction(db) {
    SchemaUtils.drop(
        SysRoleTable,
        SysMenuTable,
        SysRoleMenuTable,
        TenantMemberRoleTable,
        RefreshTokenTable,
        AuthCodeTable,
    )
    SchemaUtils.create(
        SysRoleTable,
        SysMenuTable,
        SysRoleMenuTable,
        TenantMemberRoleTable,
        RefreshTokenTable,
        AuthCodeTable,
    )
  }
}
