// 对码案例仓 sys_menu 形态（教学化简：保留 perms 列——「域:动作」风格权限串，如 member:read）。
package saas.identity.entity

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object SysMenuTable : Table("sys_menu") {
  val id = uuid("id")
  val menuName = varchar("menu_name", 64)
  val perms = varchar("perms", 128) // 权限串：「域:动作」，requirePermission 按串精确匹配
  val createdAt = timestampWithTimeZone("created_at")
  override val primaryKey = PrimaryKey(id)
}
