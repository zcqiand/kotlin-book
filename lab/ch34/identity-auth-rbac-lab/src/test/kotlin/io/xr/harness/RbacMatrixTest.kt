package io.xr.harness

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.runBlocking
import saas.identity.impl.RbacService
import saas.identity.infra.connectVerify
import saas.identity.infra.ensureVerifyDatabase
import saas.identity.infra.resetSchema

/**
 * 实测件 3：RBAC 角色-权限-菜单矩阵——种子 tenant_admin（3 菜单）/ member（1 菜单），
 * 按角色装配生效菜单矩阵，实测两角色生效菜单数与 perms 集合，矩阵必须不同。
 */
class RbacMatrixTest {

  private val db = connectVerify()
  private val rbac = RbacService(db)

  private lateinit var tenantId: Uuid
  private lateinit var adminUser: Uuid
  private lateinit var memberUser: Uuid
  private lateinit var outsider: Uuid // 无角色对照

  @kotlin.test.BeforeTest
  fun setUp() {
    runBlocking {
      ensureVerifyDatabase()
      resetSchema(db)
      tenantId = Uuid.random()
      adminUser = Uuid.random()
      memberUser = Uuid.random()
      outsider = Uuid.random()

      // 种子菜单 3 个（perms 沿「域:动作」风格）
      val menuMemberRead = rbac.seedMenu("会员档案", "member:read")
      val menuMemberWrite = rbac.seedMenu("会员编辑", "member:write")
      val menuReportIssue = rbac.seedMenu("报表签发", "report:issue")

      // 种子两角色 + 挂接菜单：tenant_admin 全量 3 个；member 仅 member:read 1 个
      val adminRole = rbac.seedRole(tenantId, "tenant_admin", "租户管理员")
      rbac.grant(adminRole, menuMemberRead)
      rbac.grant(adminRole, menuMemberWrite)
      rbac.grant(adminRole, menuReportIssue)
      val memberRole = rbac.seedRole(tenantId, "member", "普通成员")
      rbac.grant(memberRole, menuMemberRead)

      // 成员绑定角色
      rbac.assignRole(adminUser, tenantId, adminRole)
      rbac.assignRole(memberUser, tenantId, memberRole)
    }
  }

  /** 生效菜单矩阵：admin 3 菜单/perms 集 3 个；member 1 菜单/perms 集 1 个；两矩阵不同。 */
  @Test
  fun effectiveMatrixDiffersByRole() = runBlocking {
    val adminMenus = rbac.effectiveMenus(adminUser, tenantId)
    val memberMenus = rbac.effectiveMenus(memberUser, tenantId)

    // 数字读数：菜单数
    assertEquals(3, adminMenus.size, "tenant_admin 生效菜单数")
    assertEquals(1, memberMenus.size, "member 生效菜单数")

    // 集合读数：perms 集合
    val adminPerms = rbac.effectivePerms(adminUser, tenantId)
    val memberPerms = rbac.effectivePerms(memberUser, tenantId)
    assertEquals(setOf("member:read", "member:write", "report:issue"), adminPerms, "admin perms 集")
    assertEquals(setOf("member:read"), memberPerms, "member perms 集")

    // 矩阵不同断言
    assertNotEquals(adminMenus.map { it.perms }, memberMenus.map { it.perms }, "两角色矩阵不同")
    assertTrue(memberPerms.all { it in adminPerms }, "member perms 是 admin 子集（挂接设计自证）")

    // 无角色对照：空矩阵（四级 inList 首级断流）
    assertEquals(0, rbac.effectiveMenus(outsider, tenantId).size, "无角色成员矩阵为空")
  }

  /** 装配按租户隔离：同一 adminUser 在另一租户无角色 → 矩阵为空（RBAC 也吃租户边界）。 */
  @Test
  fun matrixScopedByTenant() = runBlocking {
    val otherTenant = Uuid.random()
    assertEquals(0, rbac.effectiveMenus(adminUser, otherTenant).size, "他租户矩阵为空")
    assertEquals(3, rbac.effectiveMenus(adminUser, tenantId).size, "本租户矩阵不变")
  }
}
