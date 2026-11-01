/**
 * ch30 lab｜最小面 RBAC。
 *
 * 对码来源：案例仓 lab-management-system-kotlin
 * `src/main/kotlin/io/xr/lab/impl/AuthApiServiceImpl.kt` L32-45——
 * DEMO_PERMISSIONS 权限串家族（五个业务域前缀 contract/sample/inspection/report/audit
 * + 通配 "*"）。此处取子集四枚：sample:write、inspection:write、report:issue、
 * audit:read，并保留 "*" 通配语义对齐案例仓。
 */
package lab.ch30

/** 角色：名字 + 权限串集合（对码 DEMO_PERMISSIONS 家族的子集） */
data class Role(val name: String, val permissions: Set<String>)

/** 无权限异常：role=角色名，perm=被拒权限串 */
class ForbiddenException(val role: String, val perm: String) :
    RuntimeException("越权：$role 无 $perm")

/** 权限闸：命中（含通配 "*"）放行，否则抛 [ForbiddenException] */
fun requirePermission(role: Role, perm: String) {
    if (perm !in role.permissions && "*" !in role.permissions) {
        throw ForbiddenException(role.name, perm)
    }
}
