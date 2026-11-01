// 对码案例仓 sys_role_menu 形态：角色→菜单授权关系（多对多）。
package saas.identity.entity

import org.jetbrains.exposed.v1.core.Table

object SysRoleMenuTable : Table("sys_role_menu") {
  val id = uuid("id")
  val roleId = uuid("role_id")
  val menuId = uuid("menu_id")
  override val primaryKey = PrimaryKey(id)
}
