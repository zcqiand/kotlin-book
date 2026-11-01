// RBAC 角色-权限-菜单矩阵（对码案例仓 MeApiServiceImpl 四级 inList 装配的教学化简）：
// member(userId+tenantId) → tenant_member_role → sys_role_menu → sys_menu(perms)。
// 生效菜单 = 该成员全部角色挂接菜单的并集；perms 集合 = 生效菜单 perms 串集合。
package saas.identity.impl

import java.time.OffsetDateTime
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import saas.identity.entity.SysMenuTable
import saas.identity.entity.SysRoleMenuTable
import saas.identity.entity.SysRoleTable
import saas.identity.entity.TenantMemberRoleTable

data class MenuEntry(val menuId: Uuid, val menuName: String, val perms: String)

class RbacService(private val db: R2dbcDatabase) {

  suspend fun seedRole(tenantId: Uuid, roleCode: String, roleName: String): Uuid {
    val roleId = Uuid.random()
    suspendTransaction(db) {
      SysRoleTable.insert { st ->
        st[id] = roleId
        st[SysRoleTable.tenantId] = tenantId
        st[SysRoleTable.roleCode] = roleCode
        st[SysRoleTable.roleName] = roleName
        st[createdAt] = OffsetDateTime.now()
      }
    }
    return roleId
  }

  suspend fun seedMenu(menuName: String, permsValue: String): Uuid {
    val menuId = Uuid.random()
    suspendTransaction(db) {
      SysMenuTable.insert { st ->
        st[id] = menuId
        st[SysMenuTable.menuName] = menuName
        st[SysMenuTable.perms] = permsValue
        st[createdAt] = OffsetDateTime.now()
      }
    }
    return menuId
  }

  /** 角色→菜单挂接（sys_role_menu 一行），返回受影响行数。 */
  suspend fun grant(roleId: Uuid, menuId: Uuid): Int = suspendTransaction(db) {
    SysRoleMenuTable.insert {
      it[id] = Uuid.random()
      it[SysRoleMenuTable.roleId] = roleId
      it[SysRoleMenuTable.menuId] = menuId
    }.insertedCount
  }

  /** 成员→角色绑定（tenant_member_role 一行），返回受影响行数。 */
  suspend fun assignRole(userId: Uuid, tenantId: Uuid, roleId: Uuid): Int = suspendTransaction(db) {
    TenantMemberRoleTable.insert {
      it[id] = Uuid.random()
      it[TenantMemberRoleTable.userId] = userId
      it[TenantMemberRoleTable.tenantId] = tenantId
      it[TenantMemberRoleTable.roleId] = roleId
    }.insertedCount
  }

  /** 生效菜单矩阵：member → role(s) → role_menu → menu 四级 inList 装配。 */
  suspend fun effectiveMenus(userId: Uuid, tenantId: Uuid): List<MenuEntry> =
      suspendTransaction(db) {
        val roleIds =
            TenantMemberRoleTable.selectAll()
                .where {
                  (TenantMemberRoleTable.userId eq userId) and
                      (TenantMemberRoleTable.tenantId eq tenantId)
                }
                .toList()
                .map { it[TenantMemberRoleTable.roleId] }
        if (roleIds.isEmpty()) return@suspendTransaction emptyList()
        val menuIds =
            SysRoleMenuTable.selectAll()
                .where { SysRoleMenuTable.roleId inList roleIds }
                .toList()
                .map { it[SysRoleMenuTable.menuId] }
        if (menuIds.isEmpty()) return@suspendTransaction emptyList()
        SysMenuTable.selectAll()
            .where { SysMenuTable.id inList menuIds }
            .toList()
            .map { MenuEntry(it[SysMenuTable.id], it[SysMenuTable.menuName], it[SysMenuTable.perms]) }
            .sortedBy { it.perms }
      }

  /** 生效 perms 集合（requirePermission 的查库面）。 */
  suspend fun effectivePerms(userId: Uuid, tenantId: Uuid): Set<String> =
      effectiveMenus(userId, tenantId).map { it.perms }.toSet()
}
