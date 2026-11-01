/** 矩阵装配：角色→角色菜单关联→菜单，收成权限串集合。 */
suspend fun effectivePerms(db: Database, roleIds: List<Uuid>): Set<String> =
    suspendTransaction(db) {
      val menuIds = SysRoleMenuTable.selectAll()
          .where { SysRoleMenuTable.roleId inList roleIds }
          .map { it[SysRoleMenuTable.menuId] }
      if (menuIds.isEmpty()) emptySet()
      else SysMenuTable.selectAll()
          .where { SysMenuTable.id inList menuIds }
          .mapNotNull { it[SysMenuTable.perms] }
          .toSet()
    }

/** 权限闸：核验对象是矩阵算出来的权限集合，缺权限串即授权拒绝（403）。 */
fun requirePermission(effective: Set<String>, perm: String) {
  if (perm !in effective) throw ForbiddenException("permission denied: $perm")
}