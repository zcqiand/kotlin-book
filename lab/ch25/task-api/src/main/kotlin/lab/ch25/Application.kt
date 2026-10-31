package lab.ch25

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** 毫秒/秒换算——具名常量沿 ch21 纪律（不压制 MagicNumber，把含义放回代码里）。 */
private const val MS_PER_SEC = 1000L

@Serializable data class LoginRequest(val sub: String, val role: String = "admin")

@Serializable
data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSec: Long = TokenService.ACCESS_TTL_MS / MS_PER_SEC,
)

/** 认证通过后放进 call 的身份载体：路由取 principal 时不用再碰 JWT 库类型——沿 ch24。 */
class AdminPrincipal(val sub: String, val role: String)

/** 结构化日志专用 logger：事件面（AUTH_401/AUTH_200）全从这里走——沿 ch24。 */
val authLog: Logger = LoggerFactory.getLogger("lab.ch25.AuthAudit")

private fun ApplicationCall.adminIdentity(): AdminPrincipal {
  val p = principal<AdminPrincipal>()!!
  authLog.info("event=AUTH_200 path=${request.uri} reason=ok sub=${p.sub} role=${p.role}")
  return p
}

private fun ApplicationCall.errorBody(status: HttpStatusCode, message: String) =
    ApiError(status.value, message)

/** 路径参数统一解析：非数字 id 一律按「资源不存在」收口（404 统一面）。 */
private fun ApplicationCall.pathId(): Int =
    parameters["id"]?.toIntOrNull() ?: throw NotFoundException("task")

/** 启动引导：建（若缺）验证库 + 幂等建表，产出「PG 库准备」日志行。 */
private fun Application.bootDatabase(): R2dbcDatabase {
  val state = ensureVerifyDatabase()
  log.info("event=DB_BOOT database=$PG_VERIFY_DB state=$state")
  val db = connectVerify()
  runBlocking { ExposedTaskRepository(db).ensureSchema() }
  log.info("event=DB_SCHEMA table=tasks ensured")
  return db
}

/** StatusPages 统一错误面：NotFound→404 / IllegalTransition→409 / 其余→500，一个错误体形态。 */
private fun Application.installErrors() {
  install(StatusPages) {
    exception<NotFoundException> { call, cause ->
      call.application.log.warn(
          "event=NOT_FOUND_404 path=${call.request.uri} message=${cause.message}"
      )
      call.respond(HttpStatusCode.NotFound, call.errorBody(HttpStatusCode.NotFound, "not_found"))
    }
    exception<IllegalTransitionException> { call, cause ->
      call.application.log.warn(
          "event=CONFLICT_409 path=${call.request.uri} message=${cause.message}"
      )
      call.respond(
          HttpStatusCode.Conflict,
          call.errorBody(
              HttpStatusCode.Conflict,
              "illegal_transition: ${cause.from} -> ${cause.to}",
          ),
      )
    }
    exception<Throwable> { call, cause ->
      call.application.log.error(
          "event=UNHANDLED_500 path=${call.request.uri} exception=${cause::class.simpleName} message=${cause.message}"
      )
      call.respond(
          HttpStatusCode.InternalServerError,
          call.errorBody(HttpStatusCode.InternalServerError, "internal_error"),
      )
    }
  }
}

/** 认证插件：签发走 TokenService（jjwt），校验用同一把密钥的 auth0 侧校验器——沿 ch24。 */
private fun Application.installAuth() {
  install(Authentication) {
    jwt("admin-jwt") {
      realm = "admin-api"
      verifier(TokenService.ktorVerifier())
      validate { credential ->
        val sub = credential.payload.subject
        val role = credential.payload.getClaim("role")?.asString()
        if (sub != null && role != null) AdminPrincipal(sub, role) else null
      }
      challenge { _, _ ->
        // 缺令牌与坏令牌分开记因：用请求头有无自判——沿 ch24 同款口径
        val reason =
            if (call.request.headers[HttpHeaders.Authorization] == null) "missing_token"
            else "bad_token"
        authLog.warn("event=AUTH_401 path=${call.request.uri} reason=$reason sub=-")
        call.response.headers.append(HttpHeaders.WWWAuthenticate, "Bearer realm=\"admin-api\"")
        call.respond(
            HttpStatusCode.Unauthorized,
            call.errorBody(HttpStatusCode.Unauthorized, "unauthorized"),
        )
      }
    }
  }
}

/** 路由层：只做 HTTP 形态与身份提取，业务全下放服务层——依赖单向向下。 */
private fun Routing.taskRoutes(service: TaskService) {
  // 公开面：签发双令牌（真实系统这里前面要接密码/验证码校验）
  post("/public/login") {
    val req = call.receive<LoginRequest>()
    call.respond(
        TokenPair(
            accessToken = TokenService.issueAccess(req.sub, req.role),
            refreshToken = TokenService.issueRefresh(req.sub, req.role),
        )
    )
  }

  authenticate("admin-jwt") {
    get("/tasks") {
      call.adminIdentity()
      call.respond(service.list())
    }
    post("/tasks") {
      call.adminIdentity()
      val req = call.receive<TaskCreateRequest>()
      call.respond(HttpStatusCode.Created, service.create(req.title))
    }
    get("/tasks/{id}") {
      call.adminIdentity()
      call.respond(service.get(call.pathId()))
    }
    post("/tasks/{id}/transition") {
      call.adminIdentity()
      val req = call.receive<TransitionRequest>()
      call.respond(service.transition(call.pathId(), req.targetStatus))
    }
    delete("/tasks/{id}") {
      call.adminIdentity()
      val id = call.pathId()
      if (!service.delete(id)) throw NotFoundException("task $id")
      call.respond(ApiError(status = HttpStatusCode.OK.value, error = "deleted"))
    }
    get("/stats") {
      call.adminIdentity()
      service.stats() // 两路并发取值 + update 快照
      call.respond(service.cachedStats()) // 响应读 StateFlow 的 value
    }
  }
}

fun Application.module() {
  install(ContentNegotiation) { json() }
  installErrors()
  installAuth()
  val service = TaskService(ExposedTaskRepository(bootDatabase()))
  routing { taskRoutes(service) }
  log.info("event=APP_READY service=task-api endpoints=7")
}
