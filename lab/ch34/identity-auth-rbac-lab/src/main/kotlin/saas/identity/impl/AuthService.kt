// 认证入口（不拉 HTTP，测试服务层直调——ch33 同款教学裁剪）：login 换双令牌 /
// refresh 换新 / decodeAccess 验签（一切验签失败收敛 401，案例仓 Security.requireBearer 镜像）。
package saas.identity.impl

import io.jsonwebtoken.Claims
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import saas.identity.infra.AuthException

class AuthService(private val db: R2dbcDatabase, private val jwt: JwtIssuer) {

  private val tokens = TokenIssuer(db, jwt)

  /** login：凭证换双令牌（教学化简：user/tenant 由调用方给定，不验口令哈希）。 */
  suspend fun login(userId: Uuid, tenantId: Uuid): TokenPairResult =
      tokens.issueTokenPair(userId, tenantId)

  /** refresh 换新：rotate 撤旧签新。 */
  suspend fun refresh(rtValue: String): TokenPairResult = tokens.rotate(rtValue)

  /**
   * 验签入口：null/空串 → 401「Bearer token required」；验签/格式/过期失败 →
   * 401「invalid bearer token: <异常类名>」（携带根因类名供实测读数）。
   */
  fun decodeAccess(token: String?): Claims {
    if (token.isNullOrBlank()) throw AuthException(401, "Bearer token required")
    return runCatching { jwt.decode(token) }
        .getOrElse {
          throw AuthException(401, "invalid bearer token: ${it.javaClass.simpleName}")
        }
  }
}
