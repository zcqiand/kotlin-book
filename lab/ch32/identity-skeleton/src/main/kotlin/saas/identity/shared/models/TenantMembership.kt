package saas.identity.shared.models

/** 租户成员状态枚举：值域对码案例仓 TenantMemberStatus（active/invited/suspended/disabled）。 */
enum class TenantMemberStatus(val value: String) {
  active("active"),
  invited("invited"),
  suspended("suspended"),
  disabled("disabled");

  companion object {
    fun fromValue(value: String): TenantMemberStatus =
        values().firstOrNull { it.value == value }
            ?: throw IllegalArgumentException("No enum constant TenantMemberStatus.$value")
  }
}

/**
 * 租户成员身份数据类：字段对码案例仓 TenantMembership
 * （id/userId/tenantId/roleIds/status/joinedAt）。
 */
data class TenantMembership(
    val id: kotlin.uuid.Uuid,
    val userId: kotlin.uuid.Uuid,
    val tenantId: kotlin.uuid.Uuid,
    val roleIds: List<String>,
    val status: TenantMemberStatus,
    val joinedAt: String,
) {
  fun validate() {
    kotlin.uuid.Uuid.parse(id.toString())
    kotlin.uuid.Uuid.parse(userId.toString())
    kotlin.uuid.Uuid.parse(tenantId.toString())
  }
}
