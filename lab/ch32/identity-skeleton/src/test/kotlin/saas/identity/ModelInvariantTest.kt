package saas.identity

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.uuid.Uuid
import org.junit.jupiter.api.assertThrows
import saas.identity.impl.AdminTenantsApiServiceImpl
import saas.identity.impl.MeApiServiceImpl
import saas.identity.shared.models.Tenant
import saas.identity.shared.models.TenantMemberStatus
import saas.identity.shared.models.TenantMembership
import saas.identity.shared.models.TenantStatus

class ModelInvariantTest {

  private fun tenant(
      tenantKey: String = "acme",
      name: String = "Acme Corp",
  ): Tenant =
      Tenant(
          id = Uuid.random(),
          tenantKey = tenantKey,
          name = name,
          status = TenantStatus.active,
          createdAt = "2026-11-01T00:00:00Z",
          updatedAt = "2026-11-01T00:00:00Z",
      )

  private fun membership(
      userId: Uuid = Uuid.random(),
      tenantId: Uuid = Uuid.random(),
      status: TenantMemberStatus = TenantMemberStatus.active,
  ): TenantMembership =
      TenantMembership(
          id = Uuid.random(),
          userId = userId,
          tenantId = tenantId,
          roleIds = listOf("role-admin"),
          status = status,
          joinedAt = "2026-11-01T00:00:00Z",
      )

  // —— Tenant 字段不变量 ——

  @Test
  fun `合法 Tenant 构造并通过 validate`() {
    val t = tenant()
    t.validate()
    assertEquals(TenantStatus.active, t.status)
    assertEquals("acme", t.tenantKey)
  }

  @Test
  fun `tenantKey 低于 2 字符被 validate 拒`() {
    val t = tenant(tenantKey = "a")
    val e = assertFailsWith<IllegalArgumentException> { t.validate() }
    assertContains(e.message!!, "tenantKey length should be >= 2")
  }

  @Test
  fun `tenantKey 超过 64 字符被 validate 拒`() {
    val t = tenant(tenantKey = "k".repeat(65))
    val e = assertFailsWith<IllegalArgumentException> { t.validate() }
    assertContains(e.message!!, "tenantKey length should be <= 64")
  }

  @Test
  fun `name 超过 128 字符被 validate 拒`() {
    val t = tenant(name = "n".repeat(129))
    val e = assertFailsWith<IllegalArgumentException> { t.validate() }
    assertContains(e.message!!, "name length should be <= 128")
  }

  @Test
  fun `TenantStatus 值域只有 active 与 suspended`() {
    assertEquals(TenantStatus.active, TenantStatus.fromValue("active"))
    assertEquals(TenantStatus.suspended, TenantStatus.fromValue("suspended"))
    assertThrows<IllegalArgumentException> { TenantStatus.fromValue("disabled") }
  }

  // —— TenantMembership 字段不变量 ——

  @Test
  fun `合法 TenantMembership 构造并通过 validate`() {
    val m = membership()
    m.validate()
    assertEquals(1, m.roleIds.size)
    assertEquals(TenantMemberStatus.active, m.status)
  }

  @Test
  fun `TenantMemberStatus 值域四值齐全`() {
    assertEquals("invited", TenantMemberStatus.fromValue("invited").value)
    assertEquals("suspended", TenantMemberStatus.fromValue("suspended").value)
    assertEquals("disabled", TenantMemberStatus.fromValue("disabled").value)
    assertThrows<IllegalArgumentException> { TenantMemberStatus.fromValue("archived") }
  }

  // —— impl 桩语义（对码案例仓口径） ——

  @Test
  fun `AdminTenants 缺省分页 0-20`() {
    assertEquals(0, AdminTenantsApiServiceImpl.DEFAULT_PAGE)
    assertEquals(20, AdminTenantsApiServiceImpl.DEFAULT_PAGE_SIZE)
  }

  @Test
  fun `MeApiServiceImpl whoami 空 membership 返 null 而非抛`() {
    val svc = MeApiServiceImpl(memberships = emptyList())
    assertEquals(null, svc.meWhoami(Uuid.random(), null))
  }

  @Test
  fun `MeApiServiceImpl whoami claim 优先于首个 membership`() {
    val userId = Uuid.random()
    val t1 = Uuid.random()
    val t2 = Uuid.random()
    val m1 = membership(userId = userId, tenantId = t1)
    val m2 = membership(userId = userId, tenantId = t2)
    val svc = MeApiServiceImpl(memberships = listOf(m1, m2))
    assertEquals(m2, svc.meWhoami(userId, t2))
    assertEquals(m1, svc.meWhoami(userId, null))
  }

  @Test
  fun `MeApiServiceImpl listMyTenants 只返本人`() {
    val userId = Uuid.random()
    val mine = membership(userId = userId)
    val other = membership()
    val svc = MeApiServiceImpl(memberships = listOf(other, mine))
    assertEquals(listOf(mine), svc.meListMyTenants(userId))
  }
}
