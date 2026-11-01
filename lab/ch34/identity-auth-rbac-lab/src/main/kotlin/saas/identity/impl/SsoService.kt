// SSO 单点登录（对码案例仓 OauthApiServiceImpl.exchangeAuthorizationCode）：
// authorize 签发一次性 code（绑定 user/tenant/client，CODE_TTL_MINUTES=5）→
// code 换双令牌 → 换发即删行防重放（同 code 二次换发 → INVALID_GRANT）。
package saas.identity.impl

import java.time.OffsetDateTime
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import saas.identity.entity.AuthCodeTable
import saas.identity.infra.AuthException

class SsoService(private val db: R2dbcDatabase, private val tokens: TokenIssuer) {

  /** authorize 端点：签发一次性 code。 */
  suspend fun issueAuthorizationCode(userId: Uuid, tenantId: Uuid, clientId: String): String {
    val codeValue = "code_${Uuid.random()}"
    val now = OffsetDateTime.now()
    suspendTransaction(db) {
      AuthCodeTable.insert { st ->
        st[id] = Uuid.random()
        st[AuthCodeTable.code] = codeValue
        st[AuthCodeTable.clientId] = clientId
        st[AuthCodeTable.userId] = userId
        st[AuthCodeTable.tenantId] = tenantId
        st[expiresAt] = now.plusMinutes(CODE_TTL_MINUTES)
        st[createdAt] = now
      }
    }
    return codeValue
  }

  /** token 端点：code 换双令牌（一次性消费）。 */
  suspend fun exchangeCode(codeValue: String, clientId: String): TokenPairResult {
    val row =
        suspendTransaction(db) {
          AuthCodeTable.selectAll()
              .where { (AuthCodeTable.code eq codeValue) and (AuthCodeTable.clientId eq clientId) }
              .firstOrNull()
        } ?: throw AuthException(400, "INVALID_GRANT: code 不存在或已被使用")
    if (row[AuthCodeTable.expiresAt] < OffsetDateTime.now()) {
      deleteCode(codeValue)
      throw AuthException(400, "INVALID_GRANT: expired code")
    }
    val userId = row[AuthCodeTable.userId]
    val tenantId = row[AuthCodeTable.tenantId]
    deleteCode(codeValue) // 一次性消费防重放
    return tokens.issueTokenPair(userId, tenantId, clientId)
  }

  private suspend fun deleteCode(codeValue: String) {
    suspendTransaction(db) { AuthCodeTable.deleteWhere { AuthCodeTable.code eq codeValue } }
  }

  companion object {
    const val CODE_TTL_MINUTES = 5L // 对码案例仓 OauthApiServiceImpl.CODE_TTL_MINUTES
  }
}
