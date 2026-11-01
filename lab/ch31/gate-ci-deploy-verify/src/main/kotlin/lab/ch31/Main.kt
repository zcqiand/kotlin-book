package lab.ch31

import java.sql.DriverManager

/** 教学件：门控通过后的两用入口。demo=校验演示；db-check=PG 冒烟（建表/插数/select）。 */
private const val USAGE =
    "用法：demo | db-check（db-check 读 DATABASE_URL，如 postgresql://user:pass@127.0.0.1:15439/lab_ch31_verify）"

fun main(args: Array<String>) {
  when (args.firstOrNull()) {
    "demo" -> runDemo()
    "db-check" -> runDbCheck()
    else -> println(USAGE)
  }
}

private fun runDemo() {
  val cases =
      listOf(
          Triple("李工程师", "secret1", true),
          Triple("", "secret1", false),
          Triple("ab", "secret1", false),
          Triple("李工程师", "123", false),
      )
  for ((username, password, expectValid) in cases) {
    val result = LoginValidator.validate(username, password)
    val verdict = if ((result is LoginResult.Valid) == expectValid) "通过" else "不符"
    println("[校验] $username/${"*".repeat(password.length)} → ${describe(result)} → $verdict")
  }
}

private fun describe(result: LoginResult): String =
    when (result) {
      is LoginResult.Valid -> "有效"
      is LoginResult.Invalid -> "无效（${result.reason}）"
    }

/** PG 冒烟三连：建表 / 插数 / select 计数，逐条打印回执。连不上即异常退出（门控语义）。 */
private fun runDbCheck() {
  val raw = System.getenv("DATABASE_URL") ?: error("DATABASE_URL 未设置")
  val target = parseDatabaseUrl(raw)
  val jdbcUrl = "jdbc:postgresql://${target.host}:${target.port}/${target.db}"
  DriverManager.getConnection(jdbcUrl, target.user, target.password).use { conn ->
    conn.createStatement().use { st ->
      st.execute(
          "CREATE TABLE IF NOT EXISTS login_audit (" +
              "id serial primary key, username text not null, checked_at timestamp default now())",
      )
      println("[建表] login_audit 就绪")
      val inserted = st.executeUpdate("INSERT INTO login_audit (username) VALUES ('ch31_verify')")
      println("[插数] $inserted 行（username=ch31_verify）")
      val rs = st.executeQuery("SELECT count(*) FROM login_audit")
      rs.next()
      println("[查询] login_audit 共 ${rs.getInt(1)} 行")
    }
  }
}

/** 解析案例仓同款 DATABASE_URL 形态：postgresql://user:pass@host:port/db。 */
internal fun parseDatabaseUrl(raw: String): DatabaseUrl {
  val rest = raw.substringAfter("://")
  val credentialPart = rest.substringBefore("@")
  val locationPart = rest.substringAfter("@")
  val user = credentialPart.substringBefore(":")
  val password = credentialPart.substringAfter(":")
  val host = locationPart.substringBefore(":")
  val port = locationPart.substringAfter(":").substringBefore("/")
  val db = locationPart.substringAfter("/")
  return DatabaseUrl(host, port, db, user, password)
}

internal data class DatabaseUrl(
    val host: String,
    val port: String,
    val db: String,
    val user: String,
    val password: String,
)
