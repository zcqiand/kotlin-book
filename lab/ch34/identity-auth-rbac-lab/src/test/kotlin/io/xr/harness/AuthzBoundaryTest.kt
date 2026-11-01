package io.xr.harness

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.runBlocking
import saas.identity.impl.JwtIssuer
import saas.identity.impl.RbacService
import saas.identity.impl.SecurityLab
import saas.identity.impl.TokenIssuer
import saas.identity.infra.AuthException
import saas.identity.infra.connectVerify
import saas.identity.infra.ensureVerifyDatabase
import saas.identity.infra.resetSchema

/**
 * 实测件 4：认证 vs 授权边界（401/403）——无令牌/坏签名/过期令牌三种走认证侧 401；
 * 有效令牌但缺权限串走授权侧 403；边界内对照组放行。
 */
class AuthzBoundaryTest {

  private val db = connectVerify()
  private val jwt = JwtIssuer(AuthFlowTest.SIGNING_KEY, AuthFlowTest.ISSUER, AuthFlowTest.AUDIENCE, TokenIssuer.ACCESS_TTL_SECONDS)
  private val rbac = RbacService(db)
  private val security = SecurityLab(jwt, rbac)

  private lateinit var tenantId: Uuid
  private lateinit var memberUser: Uuid // 仅 member:read

  @kotlin.test.BeforeTest
  fun setUp() {
    runBlocking {
      ensureVerifyDatabase()
      resetSchema(db)
      tenantId = Uuid.random()
      memberUser = Uuid.random()
      val menuRead = rbac.seedMenu("会员档案", "member:read")
      val memberRole = rbac.seedRole(tenantId, "member", "普通成员")
      rbac.grant(memberRole, menuRead)
      rbac.assignRole(memberUser, tenantId, memberRole)
    }
  }

  private fun tokenFor(user: Uuid, issuer: JwtIssuer = jwt): String =
      issuer.issueAccessToken(user, tenantId)

  /** 401 三种：无令牌 / 坏签名（他人密钥签发）/ 过期令牌（1 秒 TTL 变体）。 */
  @Test
  fun authenticationSideRejectsThreeWaysWith401() = runBlocking {
    // 1) 无令牌
    val e1 = assertFailsWith<AuthException> { security.requireBearer(null) }
    assertEquals(401, e1.status, "无令牌=401")
    assertTrue(e1.message!!.contains("Bearer token required"), "读数：${e1.message}")

    // 2) 坏签名：同 issuer/audience 但异密钥签发的 token，验签不过
    val attackerJwt =
        JwtIssuer("attacker-key-0123456789-0123456789-xx", AuthFlowTest.ISSUER, AuthFlowTest.AUDIENCE, 300)
    val forged = tokenFor(memberUser, attackerJwt)
    val e2 = assertFailsWith<AuthException> { security.requireBearer(forged) }
    assertEquals(401, e2.status, "坏签名=401")
    assertTrue(e2.message!!.contains("Signature"), "根因=签名（读数：${e2.message}）")

    // 3) 过期令牌
    val shortJwt = JwtIssuer(AuthFlowTest.SIGNING_KEY, AuthFlowTest.ISSUER, AuthFlowTest.AUDIENCE, ttlSeconds = 1)
    val stale = shortJwt.issueAccessToken(memberUser, tenantId)
    Thread.sleep(1500) // 越过 1 秒过期线
    val e3 = assertFailsWith<AuthException> { security.requireBearer(stale) }
    assertEquals(401, e3.status, "过期令牌=401")
    assertTrue(e3.message!!.contains("Expired"), "根因=过期（读数：${e3.message}）")
  }

  /** 403 一发 + 对照组放行：有效令牌，requirePermission 查菜单 perms。 */
  @Test
  fun authorizationSideRejectsWith403ThenControlPasses() = runBlocking {
    val token = tokenFor(memberUser)

    // 403：令牌有效，但 member 角色无 member:write
    val e =
        assertFailsWith<AuthException> { security.requirePermission(token, "member:write") }
    assertEquals(403, e.status, "缺权限串=403")
    assertTrue(e.message!!.contains("permission denied"), "读数：${e.message}")
    assertTrue(e.message!!.contains("member:write"), "缺的具体权限串可读")

    // 对照组：边界内放行（member:read 在 member 角色矩阵内）
    val claims = security.requirePermission(token, "member:read")
    assertEquals(memberUser.toString(), claims.subject, "放行时 claims 可续用（身份已证）")
  }

  /** 认证先于授权：同一缺权限请求，令牌无效时先吃 401 而非 403（通道顺序断言）。 */
  @Test
  fun authenticationPrecedesAuthorization() = runBlocking {
    val e =
        assertFailsWith<AuthException> { security.requirePermission(null, "member:write") }
    assertEquals(401, e.status, "无令牌 + 缺权限：先 401 认证通道")
  }
}
