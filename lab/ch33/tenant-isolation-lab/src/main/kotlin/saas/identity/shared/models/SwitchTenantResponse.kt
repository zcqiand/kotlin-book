// 对码案例仓同路径文件复刻（教学级简化：去 @Serializable 与 BadParameterException 基建，
// validate() 语义等价——tenantId 必须是合法 UUID，非法即抛 IllegalArgumentException）。
package saas.identity.shared.models

import kotlin.uuid.Uuid

data class SwitchTenantResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: String,
    val tenantId: Uuid,
) {
  fun validate() {
    try {
      Uuid.parse(tenantId.toString())
    } catch (e: IllegalArgumentException) {
      throw IllegalArgumentException("Property tenantId should be a valid UUID: $tenantId")
    }
  }
}
