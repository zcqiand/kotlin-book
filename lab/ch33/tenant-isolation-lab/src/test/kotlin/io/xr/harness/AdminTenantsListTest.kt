package io.xr.harness

import kotlin.uuid.Uuid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import saas.identity.impl.AdminTenantsApiServiceImpl
import saas.identity.impl.MeApiServiceImpl
import saas.identity.infra.connectVerify
import saas.identity.infra.ensureVerifyDatabase
import saas.identity.infra.resetSchema

/**
 * 实测件 1：租户生命周期全流程（创建 A → 创建 B → 成员入库 → 停用 → 删除），逐步读数。
 * 每轮 drop+create 全量重来，可重入。
 */
class AdminTenantsListTest {

  private val db = connectVerify()
  private val admin = AdminTenantsApiServiceImpl(db)
  private val me = MeApiServiceImpl(db)

  private lateinit var tenantA: Uuid
  private lateinit var tenantB: Uuid
  private val userA = Uuid.random()
  private val userB = Uuid.random()

  @kotlin.test.BeforeTest
  fun setUp() = runBlocking {
    ensureVerifyDatabase()
    resetSchema(db)
  }

  @Test
  fun tenantLifecycleCreateDisableDelete() = runBlocking {
    // 步 1：建租户 A → 总行数 0→1，status=1
    tenantA = admin.createTenant("lab-alpha", "实验租户甲")
    assertEquals(1L, admin.countTenants(), "建 A 后租户总数")
    assertEquals(1, admin.tenantStatus(tenantA), "A 初始 status")

    // 步 2：建租户 B → 总行数 1→2
    tenantB = admin.createTenant("lab-beta", "实验租户乙")
    assertEquals(2L, admin.countTenants(), "建 B 后租户总数")

    // 步 3：成员入库 → A 2 人、B 1 人
    me.insertMember(tenantA, userA, "甲组一号", createdAt = OFFSET_MIN)
    me.insertMember(tenantA, userB, "甲组二号", createdAt = OFFSET_MIN.plusSeconds(1))
    me.insertMember(tenantB, userA, "乙组一号", createdAt = OFFSET_MIN)
    assertEquals(2, me.listMembersInContext(tenantA).size, "A 上下文成员数")
    assertEquals(1, me.listMembersInContext(tenantB).size, "B 上下文成员数")

    // 步 4：停用 A → 受影响 1 行，status 读回 0，B 不受影响
    assertEquals(1, admin.disableTenant(tenantA), "停用 A 受影响行数")
    assertEquals(0, admin.tenantStatus(tenantA), "A 停用后 status")
    assertEquals(1, admin.tenantStatus(tenantB), "B 不受影响")

    // 步 5：删除 A → 成员 2 行 + 租户 1 行一并清掉
    val (members, tenants) = admin.deleteTenant(tenantA)
    assertEquals(2, members, "删除 A 清掉的成员行数")
    assertEquals(1, tenants, "删除 A 清掉的租户行数")
    assertEquals(1L, admin.countTenants(), "删除后只剩 B")
    assertEquals(0, me.listMembersInContext(tenantA).size, "A 成员已随租户清空")
    assertEquals(-1, admin.tenantStatus(tenantA), "A 主键已不存在")
    assertTrue(admin.listTenants().all { it.tenantKey == "lab-beta" }, "存量只剩 B")
  }

  companion object {
    // 固定种子时间戳：保证 A 组两条成员行的 createdAt 排序确定
    private val OFFSET_MIN = java.time.OffsetDateTime.parse("2026-06-01T00:00:00+00:00")
  }
}
