package saas.identity.impl

import saas.identity.shared.models.Tenant

/** 教学骨架用最小委托接口：方法名对码案例仓 AdminTenantsApiDelegate（不引入 Ktor 面）。 */
interface AdminTenantsApiDelegate {
  fun adminTenantsListTenants(page: Int?, pageSize: Int?): List<Tenant>
}

/**
 * admin tenants 服务骨架（对码案例仓 AdminTenantsApiServiceImpl：本切片仅 list）。
 * 案例仓走 R2dbcDatabase + Exposed 查询；教学骨架为内存桩，只保类骨架与核心方法签名。
 */
class AdminTenantsApiServiceImpl(
    private val tenants: MutableList<Tenant> = mutableListOf(),
) : AdminTenantsApiDelegate {

  override fun adminTenantsListTenants(page: Int?, pageSize: Int?): List<Tenant> {
    val p = page ?: DEFAULT_PAGE
    val ps = pageSize ?: DEFAULT_PAGE_SIZE
    return tenants.drop(p * ps).take(ps)
  }

  companion object {
    // 缺省分页参数对码案例仓 DEFAULT_PAGE=0 / DEFAULT_PAGE_SIZE=20
    const val DEFAULT_PAGE = 0
    const val DEFAULT_PAGE_SIZE = 20
  }
}
