// 认证 vs 授权边界异常（案例仓 InvalidCredentialsError=401 / ForbiddenError 语义镜像，教学收敛为单异常类）：
// status=401 认证侧拒绝（无令牌/坏签名/过期令牌），status=403 授权侧拒绝（有效令牌但缺权限串）。
package saas.identity.infra

class AuthException(val status: Int, override val message: String) : RuntimeException(message)
