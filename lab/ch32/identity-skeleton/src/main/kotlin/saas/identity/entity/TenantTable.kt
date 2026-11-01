package saas.identity.entity

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

/** 租户表骨架：列定义对码案例仓 TenantTable（教学级简化，结构保留、注释不背业务）。 */
object TenantTable : Table("tenant") {
  val id = uuid("id")
  val tenantKey = varchar("tenant_key", 64)
  val name = varchar("name", 128)
  val status = short("status")
  val createdAt = timestampWithTimeZone("created_at")
  val updatedAt = timestampWithTimeZone("updated_at")
  override val primaryKey = PrimaryKey(id)
}
