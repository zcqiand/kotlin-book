// 双令牌签发与 rotate（对码案例仓 TokenPair.kt + OauthApiServiceImpl.rotateRefreshToken）：
// access 走 JWT 真签（jjwt）；refresh 随机串落库绑定 user/tenant；
// rotate 撤旧签新（旧 rt 置 revoked=true 再签新对，旧 rt 再用 → INVALID_GRANT）。
// TTL 教学口径沿第 24 章：access 5 分钟 / refresh 7 天（案例仓 3600/30 天，差异见 receipts）。
package saas.identity.impl

import java.time.OffsetDateTime
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import saas.identity.entity.RefreshTokenTable
import saas.identity.infra.AuthException

data class TokenPairResult(
    val accessToken: String,
    val refreshToken: String,
    val userId: Uuid,
    val tenantId: Uuid,
)

class TokenIssuer(private val db: R2dbcDatabase, private val jwt: JwtIssuer) {

  /** 签发对：access（JWT）+ refresh（随机串落库，绑定 user/tenant）。 */
  suspend fun issueTokenPair(
      userId: Uuid,
      tenantId: Uuid,
      clientId: String = LAB_CLIENT_ID,
  ): TokenPairResult {
    val now = OffsetDateTime.now()
    val access = jwt.issueAccessToken(userId, tenantId)
    val refreshValue = "rt_${Uuid.random()}"
    suspendTransaction(db) {
      RefreshTokenTable.insert { st ->
        st[id] = Uuid.random()
        st[RefreshTokenTable.refreshToken] = refreshValue
        st[RefreshTokenTable.userId] = userId
        st[RefreshTokenTable.tenantId] = tenantId
        st[revoked] = false
        st[expiresAt] = now.plusSeconds(REFRESH_TTL_SECONDS)
        st[createdAt] = now
      }
    }
    return TokenPairResult(access, refreshValue, userId, tenantId)
  }

  /** refresh rotate：撤旧签新；client 绑定以旧 rt 行为准（案例仓 rotateRefreshToken 镜像）。 */
  suspend fun rotate(rtValue: String): TokenPairResult {
    val rt =
        suspendTransaction(db) {
          RefreshTokenTable.selectAll()
              .where { RefreshTokenTable.refreshToken eq rtValue }
              .firstOrNull()
        } ?: throw AuthException(400, "INVALID_GRANT: refreshToken 不存在或已被使用")
    if (rt[RefreshTokenTable.revoked]) {
      throw AuthException(400, "INVALID_GRANT: revoked refresh_token")
    }
    if (rt[RefreshTokenTable.expiresAt] < OffsetDateTime.now()) {
      throw AuthException(400, "INVALID_GRANT: expired refresh_token")
    }
    suspendTransaction(db) {
      RefreshTokenTable.update({ RefreshTokenTable.id eq rt[RefreshTokenTable.id] }) {
        it[revoked] = true
      }
    }
    return issueTokenPair(rt[RefreshTokenTable.userId], rt[RefreshTokenTable.tenantId])
  }

  companion object {
    // 第 24 章教学口径（案例仓 TokenPair 为 3600 / 30 天）
    const val ACCESS_TTL_SECONDS = 300L
    const val REFRESH_TTL_SECONDS = 7L * 24 * 3600
    const val LAB_CLIENT_ID = "lab-client"
  }
}
