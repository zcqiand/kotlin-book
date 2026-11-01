// 对码案例仓 saas-identity-platform-kotlin（v0.2.1-20261025）同路径文件逐列复刻（教学级简化不背业务注释）。
// 字段语义：tenant_key 唯一短码 / name 展示名 / status 字典（1=active 0=disabled）。
package saas.identity.entity

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object TenantTable : Table("tenant") {
  val id = uuid("id")
  val tenantKey = varchar("tenant_key", 64)
  val name = varchar("name", 128)
  val status = short("status")
  val createdAt = timestampWithTimeZone("created_at")
  val updatedAt = timestampWithTimeZone("updated_at")
  override val primaryKey = PrimaryKey(id)
}
