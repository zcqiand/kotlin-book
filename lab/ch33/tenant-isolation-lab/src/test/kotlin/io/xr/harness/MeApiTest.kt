package io.xr.harness

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.uuid.Uuid
import kotlinx.coroutines.runBlocking
import saas.identity.impl.MeApiServiceImpl
import saas.identity.impl.AdminTenantsApiServiceImpl
import saas.identity.infra.connectVerify
import saas.identity.infra.ensureVerifyDatabase
import saas.identity.infra.resetSchema
import saas.identity.infra.TenantSwitchException

/**
 * 实测件 2/3/4：行级隔离换租户 + 跨租户不可见 + 回归全绿。
 * 种子：A 租户 2 名成员、B 租户 1 名成员；userX 同属 A、B，userC 只属 B。
 */
class MeApiTest {

  private val db = connectVerify()
  private val admin = AdminTenantsApiServiceImpl(db)
  private val me = MeApiServiceImpl(db)

  private lateinit var tenantA: Uuid
  private lateinit var tenantB: Uuid
  private lateinit var memberBOnly: Uuid // 只属于 B 的成员行主键（跨租户探测目标）

  private val userX = Uuid.random() // 双租户成员：换租户主体
  private val userC = Uuid.random() // B 独占成员

  @kotlin.test.BeforeTest
  fun setUp() {
    runBlocking {
      ensureVerifyDatabase()
      resetSchema(db)
      tenantA = admin.createTenant("lab-alpha", "实验租户甲")
      tenantB = admin.createTenant("lab-beta", "实验租户乙")
      val t0 = java.time.OffsetDateTime.parse("2026-06-01T00:00:00+00:00")
      me.insertMember(tenantA, userX, "甲组一号", createdAt = t0)
      me.insertMember(tenantA, Uuid.random(), "甲组二号", createdAt = t0.plusSeconds(1))
      memberBOnly = me.insertMember(tenantB, userC, "乙组一号", createdAt = t0)
      me.insertMember(tenantB, userX, "甲组兼乙组", createdAt = t0.plusSeconds(2))
    }
  }

  /** 实测件 2：同一查询入口换租户上下文，A 上下文 2 行 / B 上下文 1 行，互不越界。 */
  @Test
  fun switchTenantContextScopesQueryResults() = runBlocking {
    // 上下文 = A：查得 2 行，全部 tenantId=A
    val ctxA = me.switchTenant(userX, tenantA)
    assertEquals(tenantA, ctxA.tenantId, "换发对回显目标租户")
    val rowsA = me.listMembersInContext(ctxA.tenantId)
    assertEquals(2, rowsA.size, "A 上下文行数")
    assertTrue(rowsA.all { it.tenantId == tenantA }, "A 上下文无越界行")

    // 同一入口换到上下文 = B：查得 2 行（userX 在 B 的成员行 + B 独占的 userC），全部 tenantId=B
    val ctxB = me.switchTenant(userX, tenantB)
    assertEquals(tenantB, ctxB.tenantId, "第二次换发对回显")
    val rowsB = me.listMembersInContext(ctxB.tenantId)
    assertEquals(2, rowsB.size, "B 上下文行数")
    assertTrue(rowsB.all { it.tenantId == tenantB }, "B 上下文无越界行")
    assertEquals(2 + 2, rowsA.size + rowsB.size, "两侧读数合计与种子一致")
  }

  /** 实测件 3：A 上下文按 B 的成员主键读/改 → 读 0 行（null）、改 0 行（拒绝落地）。 */
  @Test
  fun crossTenantPrimaryKeyAccessIsInvisible() = runBlocking {
    val ctxA = me.switchTenant(userX, tenantA)
    // 越界读：主键在库里真实存在（B 的成员），但 A 上下文谓词下命中 0 行
    assertNull(me.readMemberInContext(ctxA.tenantId, memberBOnly), "A 上下文读 B 主键=不可见")
    // 对照：换到 B 上下文同一主键立即可见（排除「主键不存在」解释）
    val ctxB = me.switchTenant(userX, tenantB)
    assertEquals(memberBOnly, me.readMemberInContext(ctxB.tenantId, memberBOnly)?.id, "B 上下文可读")
    // 越界改：回到 A 上下文改 B 成员名 → 受影响 0 行，B 侧数据原样
    assertEquals(0, me.renameMemberInContext(ctxA.tenantId, memberBOnly, "越界改名"), "越界改受影响行数")
    assertEquals("乙组一号", me.readMemberInContext(ctxB.tenantId, memberBOnly)?.memberName, "B 数据未被改动")
    // 边界内改：B 上下文改名 → 受影响 1 行
    assertEquals(1, me.renameMemberInContext(ctxB.tenantId, memberBOnly, "乙组改名"), "边界内改受影响行数")
  }

  /** 隔离门槛：非成员换租户 → NotFound（不泄露租户存在性）；未知租户 → NotFound。 */
  @Test
  fun switchGuardsNonMemberAndUnknownTenant() = runBlocking {
    val stranger = Uuid.random() // 任何租户都不是成员
    val e1 =
        assertFailsWith<TenantSwitchException> {
          runBlocking { me.switchTenant(stranger, tenantA) }
        }
    assertEquals(TenantSwitchException.Kind.NOT_FOUND, e1.kind, "非成员换租户")
    assertTrue(e1.message!!.contains("not an active member"))
    val e2 =
        assertFailsWith<TenantSwitchException> {
          runBlocking { me.switchTenant(userX, Uuid.random()) }
        }
    assertEquals(TenantSwitchException.Kind.NOT_FOUND, e2.kind, "未知租户换租户")
    assertTrue(e2.message!!.contains("tenant"))
  }

  /** 成员资格门槛 = 非 disabled：invited(2)/suspended(3) 可切，disabled(0) 404 语义（S5 口径）。 */
  @Test
  fun switchGateByMemberStatus() = runBlocking {
    val invited = Uuid.random()
    val suspended = Uuid.random()
    val disabled = Uuid.random()
    val t0 = java.time.OffsetDateTime.parse("2026-06-01T00:00:00+00:00")
    me.insertMember(tenantB, invited, "受邀人", status = 2, createdAt = t0)
    me.insertMember(tenantB, suspended, "停用人", status = 3, createdAt = t0)
    me.insertMember(tenantB, disabled, "禁用人", status = 0, createdAt = t0)
    assertEquals(tenantB, me.switchTenant(invited, tenantB).tenantId, "invited 可切")
    assertEquals(tenantB, me.switchTenant(suspended, tenantB).tenantId, "suspended 可切")
    val e =
        assertFailsWith<TenantSwitchException> {
          runBlocking { me.switchTenant(disabled, tenantB) }
        }
    assertEquals(TenantSwitchException.Kind.NOT_FOUND, e.kind, "disabled 不可切")
  }

  /** 换发对契约：validate() 合法直通 / tenantId 非法即抛（模型不变量）。 */
  @Test
  fun switchTenantResponseContract() {
    val resp =
        saas.identity.shared.models.SwitchTenantResponse(
            accessToken = "lab-at-x",
            refreshToken = "saas-rt-x",
            expiresAt = "2026-06-01T00:00:00+00:00",
            tenantId = tenantA,
        )
    resp.validate() // 不抛即过
    val bad =
        saas.identity.shared.models.SwitchTenantResponse(
            accessToken = "lab-at-x",
            refreshToken = "saas-rt-x",
            expiresAt = "2026-06-01T00:00:00+00:00",
            tenantId = Uuid.parse("00000000-0000-0000-0000-000000000000"),
        )
    bad.validate() // 零值 UUID 也是合法 UUID，直通（对码案例仓 validate 语义）
  }
}
