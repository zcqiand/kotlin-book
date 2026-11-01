// 对码案例仓 sys_role 形态（教学化简：仅保留租户内角色主档）。
package saas.identity.entity

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object SysRoleTable : Table("sys_role") {
  val id = uuid("id")
  val tenantId = uuid("tenant_id")
  val roleCode = varchar("role_code", 64) // 如 tenant_admin / member
  val roleName = varchar("role_name", 64)
  val createdAt = timestampWithTimeZone("created_at")
  override val primaryKey = PrimaryKey(id)
}
