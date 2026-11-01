package lab.ch29

import io.r2dbc.spi.ConnectionFactories
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import reactor.core.publisher.Mono

/**
 * 连接纪律沿第 23 章：本机 PG 127.0.0.1:54321，postgres/postgres，
 * 章级隔离验证库 kotlin_book_ch29_verify。
 */
const val PG_HOST = "127.0.0.1"
const val PG_PORT = "54321"
const val PG_USER = "postgres"
const val PG_PASSWORD = "postgres"
const val PG_VERIFY_DB = "kotlin_book_ch29_verify"

/** 维护库：CREATE DATABASE 只能对已存在的库发起，用自带的 postgres 库。 */
fun maintenanceUrl() = "r2dbc:postgresql://$PG_USER:$PG_PASSWORD@$PG_HOST:$PG_PORT/postgres"

fun verifyUrl() = "r2dbc:postgresql://$PG_USER:$PG_PASSWORD@$PG_HOST:$PG_PORT/$PG_VERIFY_DB"

fun connectVerify(): R2dbcDatabase = R2dbcDatabase.connect(verifyUrl())

/**
 * 建（若缺）验证库。绕过 Exposed 事务：PostgreSQL 规定 CREATE DATABASE
 * 不能在事务块里跑（autoCommit 必须 on），所以这一步走裸 R2DBC SPI。
 * 返回 "created"（新建）或 "exists"（已存在）。
 */
fun ensureVerifyDatabase(): String {
  val factory = ConnectionFactories.get(maintenanceUrl())
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
