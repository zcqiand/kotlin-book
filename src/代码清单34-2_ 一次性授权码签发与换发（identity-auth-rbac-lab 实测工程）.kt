/** 一次性授权码：签发绑定 user/tenant/client，五分钟有效，换发一次即作废。 */
class SsoCodeService(
    private val store: SsoCodeStore,
    private val tokens: TokenService,
) {

  fun issueCode(userId: Uuid, tenantId: Uuid, clientId: String): String {
    val code = "code_" + Uuid.random()
    store.save(code, userId, tenantId, clientId, expiresAt = OffsetDateTime.now().plusMinutes(5))
    return code
  }

  fun exchange(code: String, clientId: String): TokenPair {
    val row = store.find(code)
        ?: throw InvalidGrantError("INVALID_GRANT: code 不存在或已被使用")
    store.delete(code)
    if (row.clientId != clientId) throw InvalidGrantError("INVALID_GRANT: client 不匹配")
    return tokens.issuePair(row.userId, row.tenantId)
  }
}