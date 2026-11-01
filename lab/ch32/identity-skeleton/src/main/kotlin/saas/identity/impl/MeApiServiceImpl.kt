package saas.identity.impl

import kotlin.uuid.Uuid
import saas.identity.shared.models.TenantMembership

/** 教学骨架用最小委托接口：方法名对码案例仓 MeApiDelegate（不引入 Ktor 面）。 */
interface MeApiDelegate {
  fun meWhoami(userId: Uuid, claimedTenantId: Uuid?): TenantMembership?

  fun meListMyTenants(userId: Uuid): List<TenantMembership>
}

/**
 * me 族服务骨架（对码案例仓 MeApiServiceImpl）。
 * 「claim 优先 → 首个 membership；空 = null（禁 first() 崩）」的口径在教学桩里保留为
 * meWhoami 的返回语义；成员查询排序口径（恒 created_at ASC + id ASC）注记不背实现。
 */
class MeApiServiceImpl(
    private val memberships: List<TenantMembership> = emptyList(),
) : MeApiDelegate {

  override fun meWhoami(userId: Uuid, claimedTenantId: Uuid?): TenantMembership? {
    val mine = memberships.filter { it.userId == userId }
    // claim 优先 → 首个 membership；空 = null
    return mine.firstOrNull { it.tenantId == claimedTenantId } ?: mine.firstOrNull()
  }

  override fun meListMyTenants(userId: Uuid): List<TenantMembership> {
    return memberships.filter { it.userId == userId }
  }
}
