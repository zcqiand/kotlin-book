package lab.ch25

import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.R2dbcException
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import reactor.core.publisher.Mono

/**
 * 连接五件套走实测臂数据库纪律：本机 PG 127.0.0.1:54321，postgres/postgres， 章级隔离验证库 taskapi_ch25_verify（沿 ch23 体例，库名换
 * ch25 专属）。 端口递进不撞：ch22 服务=8090 / ch23 库=54321 / ch24 服务=8081 / ch25 服务=8082。
 */
const val PG_HOST = "127.0.0.1"
const val PG_PORT = "54321"
const val PG_USER = "postgres"
const val PG_PASSWORD = "postgres"
const val PG_VERIFY_DB = "taskapi_ch25_verify"

/** 维护库：CREATE DATABASE 只能对已存在的库发起，用自带的 postgres 库。 */
fun maintenanceUrl() = "r2dbc:postgresql://$PG_USER:$PG_PASSWORD@$PG_HOST:$PG_PORT/postgres"

fun verifyUrl() = "r2dbc:postgresql://$PG_USER:$PG_PASSWORD@$PG_HOST:$PG_PORT/$PG_VERIFY_DB"

fun connectVerify(): R2dbcDatabase = R2dbcDatabase.connect(verifyUrl())

/**
 * 建（若缺）验证库。绕过 Exposed 事务：PostgreSQL 规定 CREATE DATABASE 不能在事务块里跑（autoCommit 必须 on），所以这一步走裸 R2DBC
 * SPI——沿 ch23 结论。 已存在性只捕数据库访问错误族判重：泛化 catch 会把真实连接故障也一并吞掉。 关键坑（ch25 实测）：reactive 面上
 * `Mono.from(execute()).block()` 可能不等服务端完成就正常返回， 「库已存在」的服务端错误被静默丢弃、二次启动照样误报 created——必须消费
 * rowsUpdated 强制结果回流。 返回 "created"（新建）或 "exists"（已存在）。
 */
fun ensureVerifyDatabase(): String {
  val factory = ConnectionFactories.get(maintenanceUrl())
  val conn = Mono.from(factory.create()).block() ?: error("维护库连接失败")
  try {
    Mono.from(conn.createStatement("CREATE DATABASE $PG_VERIFY_DB").execute())
        .flatMap { Mono.from(it.rowsUpdated) } // 消费结果：服务端错误在此上浮，不再静默
        .block()
    return "created"
  } catch (e: R2dbcException) {
    // 只捕数据库访问错误族按「已存在」判重（PG 42P04 duplicate_database）；
    // 连接故障等其他异常照常外抛，不吞——泛化 catch 的正确解法是收窄类型面
    if (e.message?.contains("already exists") == true) return "exists"
    throw e
  } finally {
    conn.close()
  }
}
