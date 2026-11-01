// 对码案例仓 tenant_member_role 形态（教学合并：案例仓绑 member 主键，lab 简化为
// userId+tenantId 二元绑定，省 tenant_member 表——差异已在 receipts 显式登记）。
package saas.identity.entity

import org.jetbrains.exposed.v1.core.Table

object TenantMemberRoleTable : Table("tenant_member_role") {
  val id = uuid("id")
  val userId = uuid("user_id")
  val tenantId = uuid("tenant_id")
  val roleId = uuid("role_id")
  override val primaryKey = PrimaryKey(id)
}
