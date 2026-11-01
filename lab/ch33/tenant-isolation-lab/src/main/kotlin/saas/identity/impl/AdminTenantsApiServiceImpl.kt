package saas.identity.impl

import java.time.OffsetDateTime
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import saas.identity.entity.TenantMemberTable
import saas.identity.entity.TenantTable

/**
 * admin tenants 全生命周期（教学级，案例仓 AdminTenantsApiServiceImpl 只覆盖 list 读路径，
 * 本 lab 按同一写法补齐 create/disable/delete 三个生命周期 op，语义对 fastapi tenants_impl）。
 */
class AdminTenantsApiServiceImpl(private val db: R2dbcDatabase) {

  /** 创建租户：status=1（active）入库，返回主键。 */
  suspend fun createTenant(tenantKey: String, name: String): Uuid {
    val id = Uuid.random()
    val now = OffsetDateTime.now()
    suspendTransaction(db) {
      TenantTable.insert {
        it[TenantTable.id] = id
        it[TenantTable.tenantKey] = tenantKey
        it[TenantTable.name] = name
        it[status] = TENANT_STATUS_ACTIVE
        it[createdAt] = now
        it[updatedAt] = now
      }
    }
    return id
  }

  /** 停用：status 1→0，返回受影响行数（0=主键不存在）。 */
  suspend fun disableTenant(tenantId: Uuid): Int =
      suspendTransaction(db) {
        TenantTable.update({ TenantTable.id eq tenantId }) {
          it[status] = TENANT_STATUS_DISABLED
          it[updatedAt] = OffsetDateTime.now()
        }
      }

  /** 删除：先清成员再删租户本体，返回 (成员删除行数, 租户删除行数)。 */
  suspend fun deleteTenant(tenantId: Uuid): Pair<Int, Int> =
      suspendTransaction(db) {
        val members =
            TenantMemberTable.deleteWhere { TenantMemberTable.tenantId eq tenantId }
        val tenants = TenantTable.deleteWhere { TenantTable.id eq tenantId }
        members to tenants
      }

  /** 读路径：租户全量行（id/key/name/status）。 */
  suspend fun listTenants(): List<TenantRow> =
      suspendTransaction(db) {
        TenantTable.selectAll()
            .orderBy(TenantTable.createdAt to SortOrder.ASC)
            .map { row ->
              TenantRow(
                  id = row[TenantTable.id],
                  tenantKey = row[TenantTable.tenantKey],
                  name = row[TenantTable.name],
                  status = row[TenantTable.status].toInt(),
              )
            }
            .toList()
      }

  /** 总行数（lifecycle 读数用）。 */
  suspend fun countTenants(): Long =
      suspendTransaction(db) { TenantTable.selectAll().count() }

  /** 读单租户 status（-1=不存在）。 */
  suspend fun tenantStatus(tenantId: Uuid): Int =
      suspendTransaction(db) {
        TenantTable.selectAll()
            .where { TenantTable.id eq tenantId }
            .firstOrNull()
            ?.get(TenantTable.status)
            ?.toInt()
            ?: -1
      }

  data class TenantRow(val id: Uuid, val tenantKey: String, val name: String, val status: Int)

  companion object {
    const val TENANT_STATUS_ACTIVE: Short = 1
    const val TENANT_STATUS_DISABLED: Short = 0
    // 沿案例仓 AdminTenantsApiServiceImpl 缺省分页口径
    const val DEFAULT_PAGE = 0
    const val DEFAULT_PAGE_SIZE = 20
  }
}
