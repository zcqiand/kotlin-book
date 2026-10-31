package lab.ch25

import org.jetbrains.exposed.v1.core.Table

/** 业务数字升具名常量——沿 ch21 纪律（不压制规则，把含义放回代码里）。 */
private const val TITLE_MAX_LEN = 128
private const val STATUS_MAX_LEN = 16

/** tasks 表：任务 Task = id / title / status / createdAt（epoch millis，免引 exposed-java-time）。 */
object TasksTable : Table("tasks") {
  val id = integer("id").autoIncrement()
  val title = varchar("title", TITLE_MAX_LEN)
  val status = varchar("status", STATUS_MAX_LEN)
  val createdAt = long("created_at")

  override val primaryKey = PrimaryKey(id)
}
