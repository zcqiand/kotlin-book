package lab.ch24

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// 统一错误体：所有非 2xx 响应都长这一个样子，客户端只需解析两个字段
@Serializable
data class ApiError(val status: Int, val error: String)

@Serializable
data class Book(val id: Int, val title: String, val author: String)

// 响应体一律走 @Serializable DTO：裸 mapOf 混装 Int/String 会让
// kotlinx.serialization 拒绝序列化（不同元素类型的集合暂不支持）
@Serializable
data class HealthResponse(val status: String, val service: String)

@Serializable
data class StatsResponse(val books: Int, val users: Int, val requestsToday: Int, val youAre: String)

@Serializable
data class UserEntry(val sub: String, val role: String)

@Serializable
data class AuditResponse(val lastLoginBy: String, val records: Int)

@Serializable
data class LoginRequest(val sub: String, val role: String = "admin")

@Serializable
data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSec: Long = TokenService.ACCESS_TTL_MS / 1000,
)

// 认证通过后放进 call 的身份载体：把 claims 里最常用的两个字段提出来，
// 业务路由取 principal 时不用再碰 JWT 库类型
class AdminPrincipal(val sub: String, val role: String)

/**
 * 路由层取身份的统一入口：每次合法访问在此落一行 AUTH_200 结构化日志，
 * 与 challenge 里的 AUTH_401 共用同一个 logger，认证成败可在日志里对齐回放
 */
private fun ApplicationCall.adminIdentity(): AdminPrincipal {
    val p = principal<AdminPrincipal>()!!
    authLog.info("event=AUTH_200 path=${request.uri} reason=ok sub=${p.sub} role=${p.role}")
    return p
}

// 结构化日志专用 logger：事件面（AUTH_401/AUTH_200）全部从这里走，
// 与框架自身的 logger 分开，回放时按 logger 名一眼可筛
val authLog: Logger = LoggerFactory.getLogger("lab.ch24.AuthAudit")

private fun ApplicationCall.errorBody(status: HttpStatusCode, message: String) =
    ApiError(status.value, message)

fun Application.module() {
    install(ContentNegotiation) { json() }

    // StatusPages：把没被任何路由接住的异常统一收口成一个错误体。
    // 不装它，未处理异常会变成 Netty 裸 500 页面，客户端拿到的是
    // HTML 而不是 JSON——错误面失去统一形态
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error(
                "event=UNHANDLED_500 path=${call.request.uri} exception=${cause::class.simpleName} message=${cause.message}"
            )
            call.respond(
                HttpStatusCode.InternalServerError,
                call.errorBody(HttpStatusCode.InternalServerError, "internal_error")
            )
        }
    }

    install(Authentication) {
        // jwt 认证方案：校验逻辑由 TokenService 的同一把密钥支撑
        jwt("admin-jwt") {
            realm = "admin-api"
            // 用 TokenService 的密钥构造校验器：谁签发的令牌谁能验，
            // 两边密钥不同则签名校验必然失败
            verifier(TokenService.ktorVerifier())
            validate { credential ->
                // 走到这里说明签名/过期校验已过，再补一层业务面检查；
                // 返回 null 即视为认证失败，走下方 challenge 收口
                val sub = credential.payload.subject
                val role = credential.payload.getClaim("role")?.asString()
                if (sub != null && role != null) AdminPrincipal(sub, role) else null
            }
            challenge { _, _ ->
                // 缺令牌与坏令牌分开记因：运维排查时「没带」和「带了但不对」
                // 是两类完全不同的问题（前者多半是客户端没接认证，后者是密钥/时钟问题）。
                // 插件的 challenge 回调不带失败原因，这里用请求头有无自判
                val reason =
                    if (call.request.headers[HttpHeaders.Authorization] == null) "missing_token"
                    else "bad_token"
                authLog.warn(
                    "event=AUTH_401 path=${call.request.uri} reason=$reason sub=-"
                )
                call.response.headers.append(HttpHeaders.WWWAuthenticate, "Bearer realm=\"admin-api\"")
                call.respond(
                    HttpStatusCode.Unauthorized,
                    call.errorBody(HttpStatusCode.Unauthorized, "unauthorized")
                )
            }
        }
    }

    routing {
        // ---- 公开路由：不需要令牌 ----
        get("/public/health") {
            call.respond(HealthResponse(status = "up", service = "jwt-auth-demo"))
        }
        get("/public/books") {
            call.respond(
                listOf(
                    Book(1, "Kotlin 实战", "林牧"),
                    Book(2, "Ktor 服务端开发", "沈砚"),
                    Book(3, "数据访问模式", "周临"),
                )
            )
        }
        // 演示登录面：换两枚令牌。真实系统里这一步前面要接真实的
        // 密码/验证码校验，本工程只演示「校验通过之后」的签发段
        post("/public/login") {
            val req = call.receive<LoginRequest>()
            call.respond(
                TokenPair(
                    accessToken = TokenService.issueAccess(req.sub, req.role),
                    refreshToken = TokenService.issueRefresh(req.sub, req.role),
                )
            )
        }

        // ---- 受保护路由：必须带合法 access 令牌 ----
        authenticate("admin-jwt") {
            get("/admin/stats") {
                val p = call.adminIdentity()
                call.respond(
                    StatsResponse(
                        books = 9,
                        users = 128,
                        requestsToday = 5310,
                        youAre = "${p.sub}(${p.role})",
                    )
                )
            }
            get("/admin/users") {
                call.respond(
                    listOf(
                        UserEntry("linmu", "admin"),
                        UserEntry("shenyan", "editor"),
                    )
                )
            }
            get("/admin/audit") {
                val p = call.adminIdentity()
                call.respond(AuditResponse(lastLoginBy = p.sub, records = 42))
            }
        }

        // ---- 演示路由：故意抛异常，验证 StatusPages 统一收口 ----
        get("/demo/error") {
            throw IllegalStateException("demo-only-unhandled-exception")
        }
    }
}
