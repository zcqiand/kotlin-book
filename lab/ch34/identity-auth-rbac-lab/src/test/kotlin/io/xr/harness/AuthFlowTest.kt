package io.xr.harness

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import saas.identity.entity.RefreshTokenTable
import saas.identity.impl.AuthService
import saas.identity.impl.JwtIssuer
import saas.identity.impl.TokenIssuer
import saas.identity.infra.AuthException
import saas.identity.infra.connectVerify
import saas.identity.infra.ensureVerifyDatabase
import saas.identity.infra.resetSchema

/**
 * 实测件 1：双令牌认证链路——login 签发 access（JWT claims）+ refresh（落库绑定）→
 * access 过期实测（1 秒 TTL 变体）→ refresh 换新成功 → rotate 撤旧签新（旧 rt 再用被拒）。
 */
class AuthFlowTest {

  private val db = connectVerify()
  private val jwt = JwtIssuer(SIGNING_KEY, ISSUER, AUDIENCE, TokenIssuer.ACCESS_TTL_SECONDS)
  private val auth = AuthService(db, jwt)

  @kotlin.test.BeforeTest
  fun setUp() {
    runBlocking {
      ensureVerifyDatabase()
      resetSchema(db)
    }
  }

  /** 教学 TTL 常量读数：access 300 秒（5 分钟）/ refresh 604800 秒（7 天）。 */
  @Test
  fun ttlConstantsFollowChapter24Convention() {
    assertEquals(300L, TokenIssuer.ACCESS_TTL_SECONDS, "ACCESS_TTL_SECONDS")
    assertEquals(7L * 24 * 3600, TokenIssuer.REFRESH_TTL_SECONDS, "REFRESH_TTL_SECONDS")
  }

  /** login 换双令牌：access 可验签且 claims 齐（sub/tenant_id/jti/iat/exp）；refresh 落库绑定 user/tenant。 */
  @Test
  fun loginIssuesVerifiedAccessTokenAndBoundRefresh() = runBlocking {
    val userId = Uuid.random()
    val tenantId = Uuid.random()
    val pair = auth.login(userId, tenantId)

    val claims = jwt.decode(pair.accessToken)
    assertEquals(userId.toString(), claims.subject, "sub claim")
    assertEquals(tenantId.toString(), claims.get("tenant_id"), "tenant_id claim")
    assertNotNull(claims.id, "jti claim")
    assertNotNull(claims.issuedAt, "iat claim")
    assertNotNull(claims.expiration, "exp claim")
    // exp - iat = TTL 教学口径 300 秒
    assertEquals(
        300L,
        (claims.expiration.time - claims.issuedAt.time) / 1000L,
        "exp-iat 差值=access TTL",
    )

    // refresh 落库行绑定校验
    val row =
        suspendTransaction(db) {
          RefreshTokenTable.selectAll()
              .where { RefreshTokenTable.refreshToken eq pair.refreshToken }
              .firstOrNull()
        }
    assertNotNull(row, "refresh 行落库")
    assertEquals(userId, row[RefreshTokenTable.userId], "refresh 绑定 user")
    assertEquals(tenantId, row[RefreshTokenTable.tenantId], "refresh 绑定 tenant")
    assertEquals(false, row[RefreshTokenTable.revoked], "refresh 初始未撤销")
    assertTrue(pair.refreshToken.startsWith("rt_"), "refresh 随机串前缀")
  }

  /** access 过期实测（ttlSeconds=1 短变体）：decode 抛过期异常（401 通道）→ refresh 换新成功。 */
  @Test
  fun expiredAccessRejectedThenRefreshExchangeSucceeds() = runBlocking {
    val userId = Uuid.random()
    val tenantId = Uuid.random()

    // 1 秒 TTL 变体（不等 5 分钟）
    val shortJwt = JwtIssuer(SIGNING_KEY, ISSUER, AUDIENCE, ttlSeconds = 1)
    val shortToken = shortJwt.issueAccessToken(userId, tenantId)
    Thread.sleep(1500) // 越过 1 秒过期线
    val e =
        assertFailsWith<AuthException> { auth.decodeAccess(shortToken) }
    assertEquals(401, e.status, "过期 access 走认证侧 401")
    assertTrue(e.message!!.contains("Expired"), "根因=过期（读数：${e.message}）")

    // refresh 换新：以未过期的正式对换发
    val pair = auth.login(userId, tenantId)
    val renewed = auth.refresh(pair.refreshToken)
    assertTrue(renewed.accessToken.isNotEmpty(), "换新拿到新 access")
    jwt.decode(renewed.accessToken) // 新 access 可正常验签（不抛即过）
    assertNotEquals(pair.refreshToken, renewed.refreshToken, "rotate 签出新 refresh")
    assertNotEquals(pair.accessToken, renewed.accessToken, "rotate 签出新 access")
  }

  /** refresh rotate 撤旧签新：旧 rt 第二次使用 → INVALID_GRANT（400）。 */
  @Test
  fun refreshRotateOneTimeUse() = runBlocking {
    val userId = Uuid.random()
    val tenantId = Uuid.random()
    val pair1 = auth.login(userId, tenantId)
    val pair2 = auth.refresh(pair1.refreshToken) // 第一次换新成功
    assertEquals(userId, pair2.userId, "换新对绑定不变")

    val e =
        assertFailsWith<AuthException> { auth.refresh(pair1.refreshToken) } // 旧 rt 再用被拒
    assertEquals(400, e.status, "旧 rt 再用=400")
    assertTrue(e.message!!.contains("INVALID_GRANT"), "拒绝原因串（读数：${e.message}）")
    assertTrue(e.message!!.contains("revoked"), "撤旧语义可读")
  }

  /** HS256 密钥 >=32 字节硬约束（案例仓 JwtIssuer require 校验镜像）。 */
  @Test
  fun hs256SigningKeyMustBeAtLeast32Bytes() {
    val e =
        assertFailsWith<IllegalArgumentException> {
          JwtIssuer("short-key-under-32-bytes", ISSUER, AUDIENCE, 300)
        }
    assertTrue(e.message!!.contains(">=32"), "约束文案（读数：${e.message}）")
    // 边界内：恰好 32 字节合法
    JwtIssuer("0123456789abcdef0123456789abcdef", ISSUER, AUDIENCE, 300)
  }

  companion object {
    const val SIGNING_KEY = "lab-ch34-signing-key-0123456789-abcdef" // 38 字节 >=32
    const val ISSUER = "lab-issuer"
    const val AUDIENCE = "lab-client"
  }
}
