package saas.identity.entity

import org.jetbrains.exposed.v1.core.Table

/** 租户成员-角色关联表骨架：复合主键对码案例仓 TenantMemberRoleTable。 */
object TenantMemberRoleTable : Table("tenant_member_role") {
  val memberId = uuid("member_id")
  val roleId = uuid("role_id")
  override val primaryKey = PrimaryKey(memberId, roleId)
}
