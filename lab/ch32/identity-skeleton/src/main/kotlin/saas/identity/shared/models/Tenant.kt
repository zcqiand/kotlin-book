package saas.identity.shared.models

/** 租户状态枚举：值域对码案例仓 TenantStatus（active/suspended）。 */
enum class TenantStatus(val value: String) {
  active("active"),
  suspended("suspended");

  companion object {
    fun fromValue(value: String): TenantStatus =
        values().firstOrNull { it.value == value }
            ?: throw IllegalArgumentException("No enum constant TenantStatus.$value")
  }
}

/**
 * 租户数据类：字段对码案例仓 Tenant（id/tenantKey/name/status/createdAt/updatedAt）。
 * validate() 为教学级简化：保字段不变量（UUID 合法 + tenantKey 2..64 + name 2..128），
 * 不引入案例仓的 BadParameterException 基建。
 */
data class Tenant(
    val id: kotlin.uuid.Uuid,
    val tenantKey: String,
    val name: String,
    val status: TenantStatus,
    val createdAt: String,
    val updatedAt: String,
) {
  fun validate() {
    kotlin.uuid.Uuid.parse(id.toString()) // 非法 UUID 抛 IllegalArgumentException
    require(tenantKey.length >= 2) { "Property tenantKey length should be >= 2" }
    require(tenantKey.length <= 64) { "Property tenantKey length should be <= 64" }
    require(name.length >= 2) { "Property name length should be >= 2" }
    require(name.length <= 128) { "Property name length should be <= 128" }
  }
}
