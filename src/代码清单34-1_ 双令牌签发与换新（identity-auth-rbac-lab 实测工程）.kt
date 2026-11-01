/** HS256 签发与验签：claims 带 sub/tenant_id/jti 与 iat/exp；密钥不足 32 字节启动即拒。 */
class JwtIssuer(signingKey: String, private val ttlSeconds: Long) {

  private val key: SecretKey = SecretKeySpec(signingKey.toByteArray(), "HmacSHA256")

  init {
    require(signingKey.toByteArray().size >= 32) { "签名密钥不足 32 字节" }
  }

  fun issueAccessToken(userId: Uuid, tenantId: Uuid): String {
    val now = System.currentTimeMillis() / 1000
    return Jwts.builder()
        .subject(userId.toString())
        .claim("tenant_id", tenantId.toString())
        .id(Uuid.random().toString())
        .issuedAt(Date(now * 1000))
        .expiration(Date((now + ttlSeconds) * 1000))
        .signWith(key)
        .compact()
  }

  /** 验签与过期校验一体：任何失败都收敛进认证拒绝通道（401）。 */
  fun decode(token: String): Claims =
      Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
}

/** 双令牌服务：签出双卡，换新时撤旧签新。 */
class TokenService(private val jwt: JwtIssuer, private val refreshStore: RefreshTokenStore) {

  fun issuePair(userId: Uuid, tenantId: Uuid): TokenPair =
      TokenPair(
          accessToken = jwt.issueAccessToken(userId, tenantId),
          refreshToken = refreshStore.issue(userId, tenantId),
      )

  /** 换新：旧 refresh 撤销、新对签出；旧卡再用只得到同一个拒绝读数。 */
  fun exchange(refreshToken: String): TokenPair {
    val row = refreshStore.find(refreshToken)
        ?: throw InvalidGrantError("INVALID_GRANT: refreshToken 不存在或已被使用")
    if (row.revoked) throw InvalidGrantError("INVALID_GRANT: revoked refresh_token")
    refreshStore.revoke(row.id)
    return issuePair(row.userId, row.tenantId)
  }
}