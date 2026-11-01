package saas.identity.impl

import java.time.OffsetDateTime
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.any
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import saas.identity.entity.TenantMemberTable
import saas.identity.entity.TenantTable
import saas.identity.infra.TenantSwitchException
import saas.identity.shared.models.SwitchTenantResponse

/**
 * me 族（教学级，对码案例仓 MeApiServiceImpl.meSwitchTenant 语义）：
 * 换租户 = 租户存在 + 本人是该租户非 disabled 成员 → 签发带目标租户上下文的换发对；
 * 行级隔离查询入口 listMembersInContext 只认上下文 tenantId，天然画边界。
 */
class MeApiServiceImpl(private val db: R2dbcDatabase) {

  /** 换租户：返回换发对（教学级 token 为构造串，不背 JWT 依赖）。 */
  suspend fun switchTenant(userId: Uuid, targetTenantId: Uuid): SwitchTenantResponse {
    val tenantExists =
        suspendTransaction(db) {
          TenantTable.selectAll().where { TenantTable.id eq targetTenantId }.firstOrNull() != null
        }
    if (!tenantExists) throw TenantSwitchException(TenantSwitchException.Kind.NOT_FOUND, "tenant $targetTenantId")
    // 成员资格门槛 = 非 disabled（status≠0；invited/suspended 可切，S5 口径）
    val isMember =
        suspendTransaction(db) {
          TenantMemberTable.selectAll()
              .where {
                (TenantMemberTable.userId eq userId) and
                    (TenantMemberTable.tenantId eq targetTenantId)
              }
              .any { it[TenantMemberTable.status] != 0.toShort() }
        }
    if (!isMember) {
      throw TenantSwitchException(
          TenantSwitchException.Kind.NOT_FOUND,
          "user $userId is not an active member of tenant $targetTenantId",
      )
    }
    // refresh 纯构造不落库（案例仓 me_impl.py 镜像口径）
    val refresh = "saas-rt-$userId-${System.currentTimeMillis()}-$targetTenantId"
    return SwitchTenantResponse(
        accessToken = "lab-at-$userId-$targetTenantId",
        refreshToken = refresh,
        expiresAt = OffsetDateTime.now().plusSeconds(3600).toString(),
        tenantId = targetTenantId,
    )
  }

  /**
   * 行级隔离查询入口：同一入口按上下文租户过滤，只回边界内成员行
   * （模拟行级隔离：上下文从换发对来，查询恒 where tenant_id = ctx）。
   */
  suspend fun listMembersInContext(tenantId: Uuid): List<MemberRow> =
      suspendTransaction(db) {
        TenantMemberTable.selectAll()
            .where { TenantMemberTable.tenantId eq tenantId }
            .orderBy(
                TenantMemberTable.createdAt to SortOrder.ASC,
                TenantMemberTable.id to SortOrder.ASC,
            )
            .map { row ->
              MemberRow(
                  id = row[TenantMemberTable.id],
                  userId = row[TenantMemberTable.userId],
                  tenantId = row[TenantMemberTable.tenantId],
                  memberName = row[TenantMemberTable.memberName],
                  status = row[TenantMemberTable.status].toInt(),
              )
            }
            .toList()
      }

  /**
   * 跨租户不可见读：按主键读，但恒拼上下文租户谓词——主键属于别的租户时命中 0 行（隔离边界不泄露存在性）。
   */
  suspend fun readMemberInContext(tenantId: Uuid, memberId: Uuid): MemberRow? =
      suspendTransaction(db) {
        TenantMemberTable.selectAll()
            .where {
              (TenantMemberTable.id eq memberId) and (TenantMemberTable.tenantId eq tenantId)
            }
            .firstOrNull()
            ?.let { row ->
              MemberRow(
                  id = row[TenantMemberTable.id],
                  userId = row[TenantMemberTable.userId],
                  tenantId = row[TenantMemberTable.tenantId],
                  memberName = row[TenantMemberTable.memberName],
                  status = row[TenantMemberTable.status].toInt(),
              )
            }
  }

  /**
   * 跨租户不可见改：按主键改名字，恒拼上下文租户谓词——越界改命中 0 行（受影响行数=0 即拒绝落地）。
   */
  suspend fun renameMemberInContext(tenantId: Uuid, memberId: Uuid, newName: String): Int =
      suspendTransaction(db) {
        TenantMemberTable.update(
                {
                  (TenantMemberTable.id eq memberId) and
                      (TenantMemberTable.tenantId eq tenantId)
                }
            ) {
              it[memberName] = newName
              it[updatedAt] = OffsetDateTime.now()
            }
      }

  /** 成员入库（lifecycle 种子用；status 字典沿 S5）。 */
  suspend fun insertMember(
      tenantId: Uuid,
      userId: Uuid,
      memberName: String?,
      status: Short = 1,
      isOwner: Boolean = false,
      createdAt: OffsetDateTime = OffsetDateTime.now(),
  ): Uuid {
    val id = Uuid.random()
    suspendTransaction(db) {
      TenantMemberTable.insert {
        it[TenantMemberTable.id] = id
        it[TenantMemberTable.tenantId] = tenantId
        it[TenantMemberTable.userId] = userId
        it[TenantMemberTable.memberName] = memberName
        it[TenantMemberTable.isOwner] = isOwner
        it[TenantMemberTable.status] = status
        it[TenantMemberTable.createdAt] = createdAt
        it[TenantMemberTable.updatedAt] = createdAt
      }
    }
    return id
  }

  data class MemberRow(
      val id: Uuid,
      val userId: Uuid,
      val tenantId: Uuid,
      val memberName: String?,
      val status: Int,
  )
}
