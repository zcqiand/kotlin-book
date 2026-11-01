package io.xr.harness

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.runBlocking
import saas.identity.impl.JwtIssuer
import saas.identity.impl.SsoService
import saas.identity.impl.TokenIssuer
import saas.identity.infra.AuthException
import saas.identity.infra.connectVerify
import saas.identity.infra.ensureVerifyDatabase
import saas.identity.infra.resetSchema

/**
 * 实测件 2：SSO 单点登录 authorize code 一次性——签发 → 换双令牌成功 → 同 code 二次换发被拒
 *（三次调用各一实测读数）。
 */
class SsoCodeTest {

  private val db = connectVerify()
  private val jwt = JwtIssuer(AuthFlowTest.SIGNING_KEY, AuthFlowTest.ISSUER, AuthFlowTest.AUDIENCE, TokenIssuer.ACCESS_TTL_SECONDS)
  private val tokens = TokenIssuer(db, jwt)
  private val sso = SsoService(db, tokens)

  @kotlin.test.BeforeTest
  fun setUp() {
    runBlocking {
      ensureVerifyDatabase()
      resetSchema(db)
    }
  }

  /** code 一次性：authorize 签发 → 首换成功（拿到可验签 access）→ 重放被拒 INVALID_GRANT。 */
  @Test
  fun authorizeCodeIsOneTime() = runBlocking {
    val userId = Uuid.random()
    val tenantId = Uuid.random()
    val clientId = "sso-web-app"

    // 调用 1：authorize 签发一次性 code（绑定 user/tenant/client，5 分钟有效）
    val code = sso.issueAuthorizationCode(userId, tenantId, clientId)
    assertTrue(code.startsWith("code_"), "code 形态（读数：$code）")
    assertEquals(5L, SsoService.CODE_TTL_MINUTES, "CODE_TTL_MINUTES 对码案例仓")

    // 调用 2：code 换双令牌——成功
    val pair = sso.exchangeCode(code, clientId)
    assertEquals(userId.toString(), jwt.decode(pair.accessToken).subject, "换发对 sub")
    assertEquals(tenantId.toString(), jwt.decode(pair.accessToken).get("tenant_id"), "换发对 tenant_id")
    assertNotEquals(pair.refreshToken, pair.accessToken, "双令牌非同串")

    // 调用 3：同一 code 第二次换发被拒（行已删，防重放）
    val e =
        assertFailsWith<AuthException> { sso.exchangeCode(code, clientId) }
    assertEquals(400, e.status, "重放=400")
    assertTrue(e.message!!.contains("INVALID_GRANT"), "拒绝原因串（读数：${e.message}）")
    assertTrue(e.message!!.contains("不存在或已被使用"), "一次性语义可读")
  }

  /** 未知 code / client 不匹配 → 同一 INVALID_GRANT 通道（不区分，防探测）。 */
  @Test
  fun unknownCodeAndClientMismatchRejected() = runBlocking {
    val userId = Uuid.random()
    val tenantId = Uuid.random()
    val code = sso.issueAuthorizationCode(userId, tenantId, "sso-web-app")

    val e1 = assertFailsWith<AuthException> { sso.exchangeCode("code_unknown", "sso-web-app") }
    assertEquals(400, e1.status)
    assertTrue(e1.message!!.contains("INVALID_GRANT"), "未知 code（读数：${e1.message}）")

    val e2 = assertFailsWith<AuthException> { sso.exchangeCode(code, "other-app") }
    assertEquals(400, e2.status)
    assertTrue(e2.message!!.contains("INVALID_GRANT"), "client 不匹配同通道（读数：${e2.message}）")

    // 正主 code 未被动过（前两次探测均未消费）——仍可换发成功，证明探测面不误伤
    val pair = sso.exchangeCode(code, "sso-web-app")
    assertTrue(pair.accessToken.isNotEmpty(), "边界内换发放行")
  }
}
