// 对码案例仓 oauth_code 形态（教学化简）：authorize 一次性 code，绑定 user/tenant/client，
// 换发后整行删除防重放（案例仓 deleteCode 同款）。
package saas.identity.entity

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object AuthCodeTable : Table("auth_code") {
  val id = uuid("id")
  val code = varchar("code", 128)
  val clientId = varchar("client_id", 64)
  val userId = uuid("user_id")
  val tenantId = uuid("tenant_id")
  val expiresAt = timestampWithTimeZone("expires_at")
  val createdAt = timestampWithTimeZone("created_at")
  override val primaryKey = PrimaryKey(id)
}
