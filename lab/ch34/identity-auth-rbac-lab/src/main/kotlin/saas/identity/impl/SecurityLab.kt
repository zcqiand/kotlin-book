// 认证 vs 授权边界（案例仓 Security.kt requireBearer 收敛 401 + 教学补 requirePermission 403）：
// 401 = 认证侧（无令牌/坏签名/过期令牌——你是谁没说清）；403 = 授权侧（令牌有效但菜单 perms 缺权限串——你没被允许）。
package saas.identity.impl

import io.jsonwebtoken.Claims
import kotlin.uuid.Uuid
import saas.identity.infra.AuthException

class SecurityLab(private val jwt: JwtIssuer, private val rbac: RbacService) {

  /** 认证侧：null/空 → 401；验签/过期/格式 → 一切失败收敛 401（案例仓终审 I1 口径）。 */
  fun requireBearer(token: String?): Claims {
    if (token.isNullOrBlank()) throw AuthException(401, "Bearer token required")
    return runCatching { jwt.decode(token) }
        .getOrElse { throw AuthException(401, "invalid bearer token: ${it.javaClass.simpleName}") }
  }

  /**
   * 授权侧：先认证（401 通道），再查菜单 perms（403 通道）——顺序不可倒置：
   * 权限判定只对已认证身份生效。
   */
  suspend fun requirePermission(token: String?, permission: String): Claims {
    val claims = requireBearer(token)
    val userId = Uuid.parse(claims.subject)
    val tenantId = Uuid.parse(claims.get("tenant_id").toString())
    if (permission !in rbac.effectivePerms(userId, tenantId)) {
      throw AuthException(403, "permission denied: $permission")
    }
    return claims
  }
}
