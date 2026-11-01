// 对码案例仓 oauth_refresh_token 形态（教学化简）：refresh 随机串落库，绑定 user/tenant，
// rotate 撤旧 = revoked 置 true（行保留，审计可查）。
package saas.identity.entity

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object RefreshTokenTable : Table("refresh_token") {
  val id = uuid("id")
  val refreshToken = varchar("refresh_token", 128)
  val userId = uuid("user_id")
  val tenantId = uuid("tenant_id")
  val revoked = bool("revoked")
  val expiresAt = timestampWithTimeZone("expires_at")
  val createdAt = timestampWithTimeZone("created_at")
  override val primaryKey = PrimaryKey(id)
}
