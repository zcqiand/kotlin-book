# ch24 jwt-auth-demo 实测回执

- 日期：2026-11-01
- 工程：`output/xr-know-020/code/kotlin-book/lab/ch24/jwt-auth-demo/`
- 版本口径（version-lock.json）：Kotlin 2.4.21 / Gradle 9.8.1 / JDK 21 / Ktor 3.6.0（ktor-bom 收口）/ jjwt 0.12.6（api 显式 + impl/jackson runtimeOnly）/ kotlinx-serialization-json 1.9.0 / slf4j 2.0.16（api + simple）
- 驱动路线：纯内存演示，零数据库、零 Exposed（章级纪律：ch23 用过 PostgreSQL，本章不需要）。
- 运行方式：系统 Gradle 9.8.1 直跑（phase4 Gradle 环境纪律），`JAVA_TOOL_OPTIONS=-Dstdout.encoding=UTF-8`；EngineMain 读 application.conf，端口 8081（避开 ch22 的 8090）。
- 依赖树事实（`gradle dependencies` 实测）：ktor-server-auth-jwt 3.6.0 传递引入 com.auth0:java-jwt:4.6.0——Ktor jwt 插件的 `verifier()` 参数是 auth0 的 `JWTVerifier` 类型；本工程签发走 version-lock 钉定的 jjwt 0.12.6。JWT 是跨库互认的标准格式，`TokenService.ktorVerifier()` 用同一把密钥字节构造 auth0 侧校验器，jjwt 签出的令牌 auth0 校验器照样验过（实测件 3 即证）。

---

## 实测件 1：无令牌请求受保护路由 → 401

命令：`curl -s -i http://127.0.0.1:8081/admin/stats`（exit 0）

```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Bearer realm="admin-api"
Content-Length: 37
Content-Type: application/json

{"status":401,"error":"unauthorized"}
```

要点：统一质询头 `WWW-Authenticate: Bearer realm="admin-api"` 由 challenge 回调显式追加；错误体与全站非 2xx 同一结构（status + error 两字段）。

## 实测件 2：坏令牌（篡改签名）请求 → 401

前置：`POST /public/login`（body `{"sub":"linmu","role":"admin"}`）签出合法 access 令牌（151 字符，payload 可见 sub/role/iat/exp），再把签名段首字符替换为 X 构造同长坏令牌（151 字符）。

命令：`curl -s -i -H "Authorization: Bearer <坏令牌>" http://127.0.0.1:8081/admin/stats`（exit 0）

```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Bearer realm="admin-api"
Content-Length: 37
Content-Type: application/json

{"status":401,"error":"unauthorized"}
```

要点：坏签名在插件校验器（auth0 JWTVerifier）处被拒，进 challenge 收口为同一错误体；与实测件 1 的区分在日志层（见实测件 5：missing_token vs bad_token）。

## 实测件 3：合法 access 令牌请求 → 200 + claims 内容

命令：`curl -s -i -H "Authorization: Bearer <合法令牌>" http://127.0.0.1:8081/admin/stats`（exit 0）

```
HTTP/1.1 200 OK
Content-Length: 68
Content-Type: application/json

{"books":9,"users":128,"requestsToday":5310,"youAre":"linmu(admin)"}
```

要点：`youAre` 字段回显的是令牌 claims 里的 sub+role——jjwt 签发、auth0 校验器验签、claims 经 JWTPrincipal 提取，跨库链路全通。

## 实测件 4：/demo/error → StatusPages 统一错误体

命令：`curl -s -i http://127.0.0.1:8081/demo/error`（exit 0；路由内故意抛 IllegalStateException）

```
HTTP/1.1 500 Internal Server Error
Content-Length: 39
Content-Type: application/json

{"status":500,"error":"internal_error"}
```

要点：未处理异常被 StatusPages exception<Throwable> 统一收口为全站同一错误体；异常本体走服务端日志（`event=UNHANDLED_500 path=/demo/error exception=IllegalStateException message=demo-only-unhandled-exception`），不泄给客户端。

## 实测件 5：401/200 结构化日志链路回放

logger 名 `lab.ch24.AuthAudit`（slf4j-simple 控制台输出），三次请求与实测件 1/2/3 一一对应、顺序一致：

```
[eventLoopGroupProxy-4-1] WARN lab.ch24.AuthAudit - event=AUTH_401 path=/admin/stats reason=missing_token sub=-
[eventLoopGroupProxy-4-3] WARN lab.ch24.AuthAudit - event=AUTH_401 path=/admin/stats reason=bad_token sub=-
[eventLoopGroupProxy-4-4] INFO lab.ch24.AuthAudit - event=AUTH_200 path=/admin/stats reason=ok sub=linmu role=admin
```

要点：行内字段自拼成 key=value 可回放形态；缺令牌（missing_token）与坏令牌（bad_token）分因记录，AUTH_200 带 sub/role 可定位到主体。

## 实测件 6：构建全绿

命令：`gradle build --console=plain`（exit 0）

```
> Task :build

BUILD SUCCESSFUL in 17s
6 actionable tasks: 5 executed, 1 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

---

## 附：回执总检

- 逐字纪律：实测件 1-5 均为单轮整块输出（curl 重定向/日志 grep 原样拷贝进 `_work/rec*.log`），无跨轮拼合。
- 中途失败留痕（红态已修，共两处）：①首版 `verifier(TokenService.verifier())` 编译失败——Ktor jwt 插件 verifier 参数类型是 auth0 `JWTVerifier` 而非 jjwt `JwtParser`，实测编译报错 `Argument type mismatch: actual type is 'JwtParser', but 'JWTVerifier' was expected`，修正为 `TokenService.ktorVerifier()`（同密钥构造 auth0 校验器，跨库互认）。②首版路由裸 `mapOf` 混装 Int/String 响应，实测件 3 得 500，服务器日志坐实 `Serializing collections of different element types is not yet supported`，修正为 @Serializable DTO 后复绿（回执即现版实测件 3）。
- 无令牌/坏令牌区分实现注记：插件 challenge 回调签名为 `(scheme, realm)` 不携带失败原因，缺/坏区分由回调内自判 `Authorization` 请求头有无（实测件 5 两行日志为证）。
- 环境注记：Gradle wrapper 未现场下载，按 phase4 Gradle 环境纪律用系统 Gradle 9.8.1 直跑；服务器以 `gradle run` 后台挂起，回执取毕即 taskkill，无遗留监听。
- 随书仓身份：`output/xr-know-020/code/kotlin-book/.git` 存在（`git rev-parse --show-toplevel` 通过，首行 `D:/qiand-life/1-projects/xr-knowledge-suite/output/xr-know-020/code/kotlin-book`）。
- 过程日志存 `_work/`（不入库）：rec0-build / rec1-no-token / rec2-login / rec2-bad-token / rec3-good-token / rec4-demo-error / rec5-auth-log / server.log。

---

## 补拍件 A：反例现场留档（裸奔三发 200，复绿前）+ 复绿回执

日期：2026-11-01（第 24 章 lab 补拍臂）。临时改造方式：`install(Authentication)` 整块块注释 + `authenticate("admin-jwt")` 包裹拆除（三条 /admin 路由裸登记）+ 两处 `call.adminIdentity()` 换 `AdminPrincipal("bare-mode", "bare")` 占位（否则 principal 为 null 直接 NPE，会经 StatusPages 变 500，拍不到裸奔 200）。改造前 Application.kt 已字节级留底 `_work/bare-backup/Application.kt.orig`（sha1 `c00d81dfedb2c5853caa6d9197f4f51928ed5337`）。

复绿前，无凭证三发逐字回执（exit 0 各一发）：

### A-1 GET /admin/stats（无凭证）

```
HTTP/1.1 200 OK
Content-Length: 71
Content-Type: application/json

{"books":9,"users":128,"requestsToday":5310,"youAre":"bare-mode(bare)"}
```

### A-2 GET /admin/users（无凭证）

```
HTTP/1.1 200 OK
Content-Length: 66
Content-Type: application/json

[{"sub":"linmu","role":"admin"},{"sub":"shenyan","role":"editor"}]
```

### A-3 GET /admin/audit（无凭证）

```
HTTP/1.1 200 OK
Content-Length: 40
Content-Type: application/json

{"lastLoginBy":"bare-mode","records":42}
```

反例要点：同一组 /admin 路由，Authentication 插件缺席时对无凭证请求照单全收——stats 统计面、users 用户清单、audit 审计记录全部泄露给匿名调用方。这就是书稿正文「认证不是路由的可选装饰，而是受保护面的唯一门」的反例实物。

### A-4 复绿回执

复绿操作：`_work/bare-backup/Application.kt.orig` 拷回原位，`cmp` 逐字节一致 + sha1 复核同值（`c00d81dfedb2c5853caa6d9197f4f51928ed5337`）——工程现态即复绿态，临时改造已全部移出。复绿后重启服务，无凭证一发验证：

```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Bearer realm="admin-api"
Content-Length: 37
Content-Type: application/json

{"status":401,"error":"unauthorized"}
```

复绿后构建全绿：`gradle build` → `BUILD SUCCESSFUL in 9s`（6 actionable tasks: 4 executed, 2 up-to-date）。留底件 `Application.kt.orig` 保留在 `_work/`（不入库）供追溯。

## 补拍件 B：TokenService 签发/校验/篡改三回合（TokenDemo 实测）

新增正式工程件：`src/main/kotlin/lab/ch24/TokenDemo.kt`（演示入口，top-level main）+ TokenService.kt 增补 `verifyStrict()`（与 verify 同一条解析链但不吞异常，供打印失败原因）+ build.gradle.kts 注册 `runTokenDemo` JavaExec 任务。运行：`gradle runTokenDemo`（exit 0）。

逐字回执：

```
=== round 1: issue access + refresh ===
accessToken  = eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJsaW5tdSIsInJvbGUiOiJhZG1pbiIsImlhdCI6MTc5MzQ4NDc5NSwiZXhwIjoxNzkzNDg1MDk1fQ.F0VdTN05RCmzLE5lHqM_0aPoevVDTOwP6hXRVXUym4U
refreshToken = eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJsaW5tdSIsInJvbGUiOiJhZG1pbiIsImlhdCI6MTc5MzQ4NDc5NSwiZXhwIjoxNzk0MDg5NTk1fQ.l-9nVns88IY5hVpbsUmjQUh8RQWgTL5IIfNfPU7z_UY
access decoded claims = sub=linmu role=admin iat=Sun Nov 01 06:13:15 CST 2026 exp=Sun Nov 01 06:18:15 CST 2026
refresh decoded claims = sub=linmu role=admin iat=Sun Nov 01 06:13:15 CST 2026 exp=Sun Nov 08 06:13:15 CST 2026
=== round 2: verify legit token ===
verify(access) = PASS sub=linmu role=admin exp=Sun Nov 01 06:18:15 CST 2026
=== round 3: tampered token ===
tampered payload segment head char 'X': eyJhbGciOiJIUzI1NiJ9.XyJzdWIiOiJsaW5tdSIsInJvbGUiOiJhZG1pbiI......
verifyStrict(tampered) FAIL exception=io.jsonwebtoken.security.SignatureException
message=JWT signature does not match locally computed signature. JWT validity cannot be asserted and should not be trusted.
```

要点：两枚令牌同 sub/role/iat，仅 exp 不同（access now+5min / refresh now+7d），坐实「同一密钥签出的两种寿命」；载荷段首字符改一个（'e'→'X'，Base64 解码后 JSON 即破）当场 `SignatureException`——签名对载荷有整段覆盖，改任何一字都逃不过校验。

## 补拍件 C：书稿代码块裁剪清单（正文誊写用）

五块均从工程现态源码裁出，代码字面零改动，省略处以「……（省略）」标位，注释保留「为什么」语义（个别注释为控行数做了等义压缩）。回溯锚点：

- 块 A/B 来源：`src/main/kotlin/lab/ch24/TokenService.kt`
- 块 C/D/E 来源：`src/main/kotlin/lab/ch24/Application.kt`

裁剪后工程无残渣：块 A/B 所涉 verifyStrict/TokenDemo 为正式留档件（补拍件 B 已述）；块 C/D/E 均为现态源码原样区段，无临时改造混入（复绿 sha1 已核）。

### 块 A：TokenService 双令牌签发段

来源：TokenService.kt L21、L23-35、L49-56（L37-48 ktorVerifier 段省略）

```kotlin
object TokenService {
    // HS256 要求密钥长度不少于 32 字节，短密钥会被 jjwt 直接拒绝
    private const val DEMO_SECRET = "demo-only-secret-key-change-me-32-bytes!!"

    private val key: SecretKey = Keys.hmacShaKeyFor(DEMO_SECRET.toByteArray())

    const val ACCESS_TTL_MS: Long = 5 * 60 * 1000L        // access：5 分钟短效
    const val REFRESH_TTL_MS: Long = 7 * 24 * 60 * 60 * 1000L // refresh：7 天长效

    /** 签发 access 令牌：claims = sub + role + iat + exp（exp 为 now + 5 分钟） */
    fun issueAccess(sub: String, role: String): String = issue(sub, role, ACCESS_TTL_MS)

    /** 签发 refresh 令牌：claims 结构与 access 相同，差别只在 exp（now + 7 天） */
    fun issueRefresh(sub: String, role: String): String = issue(sub, role, REFRESH_TTL_MS)

    // ……（省略：ktorVerifier()——用同一把密钥构造 Ktor 插件侧 auth0 校验器，跨库互认）

    private fun issue(sub: String, role: String, ttlMs: Long): String =
        Jwts.builder()
            .subject(sub)                                          // sub：令牌主体
            .claim("role", role)                                   // role：自定义私有声明
            .issuedAt(Date())                                      // iat：签发时间
            .expiration(Date(System.currentTimeMillis() + ttlMs))  // exp：过期时间
            .signWith(key, Jwts.SIG.HS256)                         // HS256 对称签名
            .compact()
    // ……（省略：verify / verifyStrict 校验段，见块 B）
}
```

### 块 B：TokenService 校验段（解析+验签+过期，含异常出口）

来源：TokenService.kt L58-85（verifyStrict 为本次补拍增补的正式留档件）

```kotlin
    /**
     * 校验令牌：签名不对、格式不对、已过期都统一返回 null。
     *
     * 为什么不向外抛异常：调用方（认证插件）只关心「这枚令牌能不能用」，
     * 校验失败的细分原因（坏签名/过期/格式）交给日志层去区分，接口面
     * 保持单一布尔语义，避免调用方逐类捕获。
     */
    fun verify(token: String): Claims? = try {
        Jwts.parser()
            .verifyWith(key)          // 同时校验签名与签发方密钥
            .build()
            .parseSignedClaims(token) // 签名/过期/格式任一不过即抛 JwtException
            .payload
    } catch (_: JwtException) {
        null
    }

    /**
     * 演示用严格校验：与 verify 同一条解析链，但不吞异常——JwtException 原样
     * 抛给调用方，供 TokenDemo 打印失败的具体原因（坏签名/过期/格式各自异常
     * 类名不同）。verify 本体就是它的 null 包裹，两者不是两条链路
     */
    fun verifyStrict(token: String): Claims =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
```

### 块 C：Authentication 插件接线段

来源：Application.kt L98-129（challenge 内 401 响应段 L122-126 省略）、L178-181（authenticate 块内取 /admin/audit 代表路由；/admin/stats、/admin/users 同构省略）

```kotlin
install(Authentication) {
    jwt("admin-jwt") {
        realm = "admin-api"
        // 校验器与签发共用同一把密钥：两边密钥不同则签名校验必然失败
        verifier(TokenService.ktorVerifier())
        validate { credential ->
            // 签名/过期校验已过再补业务面检查；返回 null 即认证失败，走 challenge
            val sub = credential.payload.subject
            val role = credential.payload.getClaim("role")?.asString()
            if (sub != null && role != null) AdminPrincipal(sub, role) else null
        }
        challenge { _, _ ->
            // 缺令牌（没带）与坏令牌（不对）分开记因：排查时是两类完全不同的问题；
            // 插件的 challenge 回调不带失败原因，这里用请求头有无自判
            val reason =
                if (call.request.headers[HttpHeaders.Authorization] == null) "missing_token"
                else "bad_token"
            authLog.warn(
                "event=AUTH_401 path=${call.request.uri} reason=$reason sub=-"
            )
            // ……（省略：追加 WWW-Authenticate 质询头并回 401 统一错误体）
        }
    }
}

authenticate("admin-jwt") {
    get("/admin/audit") {
        val p = call.adminIdentity()
        call.respond(AuditResponse(lastLoginBy = p.sub, records = 42))
    }
    // ……（省略：/admin/stats、/admin/users 两条同构路由）
}
```

### 块 D：StatusPages 接线段（异常映射 + 统一错误体结构）

来源：Application.kt L27-29（ApiError）、L77-78（errorBody）、L83-96（StatusPages 装配）

```kotlin
// 统一错误体：所有非 2xx 响应都长这一个样子，客户端只需解析两个字段
@Serializable
data class ApiError(val status: Int, val error: String)

private fun ApplicationCall.errorBody(status: HttpStatusCode, message: String) =
    ApiError(status.value, message)

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
```

### 块 E：AuthAudit 结构化日志段（401/200 各一行的记录处）

来源：Application.kt L73-75（authLog 声明）、L63-71（adminIdentity = AUTH_200 记录处）、L116-121（challenge 内分因 + AUTH_401 记录处，段内代码在块 C 亦出现，此处聚焦日志面）

```kotlin
// 结构化日志专用 logger：事件面（AUTH_401/AUTH_200）全部从这里走，
// 与框架自身的 logger 分开，回放时按 logger 名一眼可筛
val authLog: Logger = LoggerFactory.getLogger("lab.ch24.AuthAudit")

/**
 * 路由层取身份的统一入口：每次合法访问在此落一行 AUTH_200 结构化日志，
 * 与 challenge 里的 AUTH_401 共用同一个 logger，认证成败可在日志里对齐回放
 */
private fun ApplicationCall.adminIdentity(): AdminPrincipal {
    val p = principal<AdminPrincipal>()!!
    authLog.info("event=AUTH_200 path=${request.uri} reason=ok sub=${p.sub} role=${p.role}")
    return p
}

// challenge 回调内（401 分因）：缺令牌与坏令牌分开记因，排查时是两类完全不同的问题
val reason =
    if (call.request.headers[HttpHeaders.Authorization] == null) "missing_token"
    else "bad_token"
authLog.warn(
    "event=AUTH_401 path=${call.request.uri} reason=$reason sub=-"
)
```

过程日志补录：`_work/rec-bare-1-stats.log` / `rec-bare-2-users.log` / `rec-bare-3-audit.log` / `rec-restore-401.log` / `rec-tokendemo.log` / `rec-final-build.log` / `server-bare.log` / `server-restored.log` / `bare-backup/Application.kt.orig`。
