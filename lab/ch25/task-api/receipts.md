# ch25 task-api 实测回执

- 日期：2026-11-01
- 工程：`output/xr-know-020/code/kotlin-book/lab/ch25/task-api/`
- 版本口径（version-lock.json）：Kotlin 2.4.21 / Gradle 9.8.1 / JDK 21 / Ktor 3.6.0（ktor-bom 收口）/ Exposed 1.5.1（exposed-bom 收口，org.jetbrains.exposed.v1.*）/ r2dbc-postgresql 1.1.3.RELEASE / jjwt 0.12.6（api 显式 + impl/jackson runtimeOnly）/ kotlinx-serialization 1.9.0 / kotlinx-coroutines 1.11.0 / junit 6.1.3（junit-bom）/ slf4j 2.0.16（api + simple）/ ktfmt 0.27.0 / detekt 1.23.8——本书第五个 version catalog（gradle/libs.versions.toml），版本零自填
- 工艺对位：四级门控沿 ch21（ktfmt→detekt→test→build 全挂 check）；Ktor/EngineMain 沿 ch22；Exposed+R2DBC+本机 PG 沿 ch23；TokenService/jwt validate/challenge 分因/StatusPages ApiError/AUTH 结构化日志沿 ch24
- PG：本机 127.0.0.1:54321（postgres/postgres），验证库 `taskapi_ch25_verify`，零容器、零共享 PG（100.79.128.25 未触碰）
- 端口：服务 8082（application.conf），递进不撞：ch22=8090 / ch23=54321(PG) / ch24=8081 / ch25=8082
- 运行方式：系统 Gradle 9.8.1 直跑，`JAVA_TOOL_OPTIONS=-Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8`（stderr.encoding 为补测批新增，见「坑位实录」）

---

## 实测件 0：四级门控 + 测试全绿

命令：`gradle clean check --console=plain`（exit 0；四关全挂 check：detekt → ktfmtCheckMain/ktfmtCheckScripts/ktfmtCheckTest → test → build）

```
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :clean
> Task :detekt
> Task :ktfmtCheckMain
> Task :ktfmtCheckScripts
> Task :ktfmtCheckTest
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :processResources
> Task :processTestResources NO-SOURCE
> Task :compileKotlin
> Task :compileJava NO-SOURCE
> Task :classes
> Task :jar
> Task :compileTestKotlin
> Task :compileTestJava NO-SOURCE
> Task :testClasses UP-TO-DATE

> Task :test
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8

TaskApiIntegrationTest > 非法迁移抛业务异常且真库状态不变() PASSED

TaskApiIntegrationTest > countByStatus 与 recentTitles 与表内分布对死() PASSED

TaskApiIntegrationTest > create 落库后 findAll 查得回且初始 TODO() PASSED

TaskApiIntegrationTest > 迁移链全经真库逐段落库() PASSED

TaskApiIntegrationTest > delete 真删 查无残留() PASSED

TaskServiceUnitTest > DONE 是终态 无任何合法迁移() PASSED

TaskServiceUnitTest > 非法迁移 TODO 直接跳 DONE 抛 IllegalTransitionException() PASSED

TaskServiceUnitTest > 删除后查无此任务 再删返回 false() PASSED

TaskServiceUnitTest > 合法迁移链 TODO 到 DOING 到 DONE 全程放行() PASSED

TaskServiceUnitTest > stats 两路并发合流 计数与最近任务对死() PASSED

TaskServiceUnitTest > 创建任务初始状态为 TODO() PASSED

> Task :check

[Incubating] Problems report is available at: file:///D:/qiand-life/1-projects/xr-knowledge-suite/output/xr-know-020/code/kotlin-book/lab/ch25/task-api/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.8.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 16s
10 actionable tasks: 10 executed
```

JUnit XML 报告统计（build/test-results/test/）：单元 `tests="6" skipped="0" failures="0" errors="0"` + 集成 `tests="5" skipped="0" failures="0" errors="0"`，合计 tests=11。

- 单元测试 6 条（TaskServiceUnitTest，仓储用内存测试替身 FakeTaskRepository）：创建初始 TODO / 合法迁移链 TODO→DOING→DONE / 非法迁移抛 IllegalTransitionException 且状态不变 / DONE 终态无任何合法迁移 / stats 两路 async 合流与 StateFlow 快照一致 / 删除后查无再删 false。
- 集成测试 5 条（TaskApiIntegrationTest，真实 PG，沿 ch23 形态）：create 落库查回 / 迁移链逐段落库 / 非法迁移异常且真库状态不变 / delete 真删查无残留 / countByStatus+recentTitles 与表内分布对死。

detekt 门控有效性留痕：首轮 check detekt 真拦 4 条（TooGenericExceptionCaught / ThrowsCount / MagicNumber / UnusedPrivateProperty），按 ch21 纪律逐条修代码（不压制规则），复跑全绿。拦截行原文：`Db.kt:34:12 The caught exception is too generic` / `Application.kt:125:21 Too many throw statements in the function taskRoutes. The maximum number of allowed throw statements is 2` / `Application.kt:36:59 This expression contains a magic number` / `TaskServiceUnitTest.kt:91:9 Private property 't1' is unused`。

---

## 编号发（服务运行态逐发回执）

服务启动行（端口 8082，首轮实测件序号发运行实例）：

```
[main] INFO Application - Autoreload is disabled because the development mode is off.
[main] INFO Application - event=DB_BOOT database=taskapi_ch25_verify state=created
[main] INFO Application - event=DB_SCHEMA table=tasks ensured
[main] INFO Application - event=APP_READY service=task-api endpoints=7
[main] INFO Application - Application started in 1.465 seconds.
[DefaultDispatcher-worker-1] INFO Application - Responding at http://127.0.0.1:8082
```

补测批修复后重启，DB_BOOT 如实上报 exists：

```
[main] INFO Application - event=DB_BOOT database=taskapi_ch25_verify state=exists
[main] INFO Application - event=DB_SCHEMA table=tasks ensured
[DefaultDispatcher-worker-2] INFO Application - Responding at http://127.0.0.1:8082
```

### 发①｜无令牌 GET /tasks → 401（missing_token 分因）

预期：401 + 统一错误体 + WWW-Authenticate 头；服务端 AUTH_401 日志 reason=missing_token。

命令：`curl -s -i http://127.0.0.1:8082/tasks`

```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Bearer realm="admin-api"
Content-Length: 37
Content-Type: application/json

{"status":401,"error":"unauthorized"}
```

服务端对应行：`WARN lab.ch25.AuthAudit - event=AUTH_401 path=/tasks reason=missing_token sub=-`

### 发②｜POST /public/login → 200 双令牌

预期：200，签发 accessToken/refreshToken 双令牌，expiresInSec=300（access 5 分钟短效；refresh 同签法 7 天长效）。

命令：`curl -s -i -X POST http://127.0.0.1:8082/public/login -H "Content-Type: application/json" -d '{"sub":"ops-user","role":"admin"}'`

```
HTTP/1.1 200 OK
Content-Length: 386
Content-Type: application/json

{"accessToken":"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJvcHMtdXNlciIsInJvbGUiOiJhZG1pbiIsImlhdCI6MTc5MzQ4NzU5MSwiZXhwIjoxNzkzNDg3ODkxfQ.r-Mq434TfYtz6Mx7JxOwoK12Pfv323W0QrqRd-aMqPM","refreshToken":"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJvcHMtdXNlciIsInJvbGUiOiJhZG1pbiIsImlhdCI6MTc5MzQ4NzU5MSwiZXhwIjoxNzk0MDkyMzkxfQ.bNU-nQ54oaN5cGu9MeCqN0r0B3EEQeWhNFRUv7ekq1g","tokenType":"Bearer","expiresInSec":300}
```

### 发③｜带令牌 POST /tasks（title=写周报）→ 201 含 id 与 status=TODO

预期：201 Created，响应含自增 id 与 status=TODO。

命令：`curl -s -i -X POST http://127.0.0.1:8082/tasks -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" --data-binary @_work/body-create.json`

（body 落文件再 `--data-binary` 是转码坑对策：直传中文 arg 会被 shell 面 GBK 化，首发实坏已删行修复，见「坑位实录」）

```
HTTP/1.1 201 Created
Content-Length: 71
Content-Type: application/json

{"id":16,"title":"写周报","status":"TODO","createdAt":1793487629997}
```

### 发④｜带令牌 GET /tasks → 列表含③那条

预期：200，数组含 id=16、title=写周报、status=TODO。

命令：`curl -s -i http://127.0.0.1:8082/tasks -H "Authorization: Bearer $TOKEN"`

```
HTTP/1.1 200 OK
Content-Length: 73
Content-Type: application/json

[{"id":16,"title":"写周报","status":"TODO","createdAt":1793487629997}]
```

### 发⑤｜带令牌 POST /tasks/16/transition target=DOING → 200 status=DOING

预期：200，status=DOING（合法迁移 TODO→DOING 放行）——本发同时是发⑥反例的复绿对照。

命令：`curl -s -i -X POST http://127.0.0.1:8082/tasks/16/transition -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" --data-binary @_work/body-doing.json`

```
HTTP/1.1 200 OK
Content-Length: 72
Content-Type: application/json

{"id":16,"title":"写周报","status":"DOING","createdAt":1793487629997}
```

### 发⑥｜反例：新任务 TODO 直接跳 DONE → 409 统一错误体（与⑤复绿对照成对）

反例造数（6a）：新建「跳步反例」得 id=17，状态 TODO：

```
HTTP/1.1 201 Created
Content-Length: 74
Content-Type: application/json

{"id":17,"title":"跳步反例","status":"TODO","createdAt":1793487641595}
```

反例正文（6b）：target=DONE 直接跳 → 409 Conflict + 统一错误体：

命令：`curl -s -i -X POST http://127.0.0.1:8082/tasks/17/transition ... --data-binary @_work/body-done.json`

```
HTTP/1.1 409 Conflict
Content-Length: 57
Content-Type: application/json

{"status":409,"error":"illegal_transition: TODO -> DONE"}
```

状态未破坏核验（6c）：GET /tasks/17 仍 TODO：

```
HTTP/1.1 200 OK
Content-Length: 74
Content-Type: application/json

{"id":17,"title":"跳步反例","status":"TODO","createdAt":1793487641595}
```

复绿对照：同一条链路上合法迁移（发⑤ TODO→DOING）200 放行，且 6c 证明反例被拒后资源原状——红绿成对，非法迁移被网关面收口成 409、不落库。

### 发⑦｜带令牌 GET /stats → 计数与 StateFlow 快照一致

预期：countsByStatus 与④⑤⑥后的实际分布对死（id16=DOING、id17=TODO → TODO:1 + DOING:1，DONE 无）；recentTitles 按 id 倒序 = [跳步反例, 写周报]；响应读 StateFlow 的 value（service.stats() 先 update 快照，路由层 cachedStats() 取值）。

命令：`curl -s -i http://127.0.0.1:8082/stats -H "Authorization: Bearer $TOKEN"`

```
HTTP/1.1 200 OK
Content-Length: 83
Content-Type: application/json

{"countsByStatus":{"DOING":1,"TODO":1},"recentTitles":["跳步反例","写周报"]}
```

对账：表内两行 = id16 DOING + id17 TODO，计数 {DOING:1, TODO:1} 与 recent 两条逐一对上。

### 发⑧｜坏令牌 GET /tasks → 401（bad_token 分因）

预期：401 + 统一错误体；服务端 AUTH_401 日志 reason=bad_token（与发①的 missing_token 分因互斥）。

命令：`curl -s -i http://127.0.0.1:8082/tasks -H "Authorization: Bearer deadbeef.not-a-token.forged"`

```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Bearer realm="admin-api"
Content-Length: 37
Content-Type: application/json

{"status":401,"error":"unauthorized"}
```

服务端对应行：`WARN lab.ch25.AuthAudit - event=AUTH_401 path=/tasks reason=bad_token sub=-`

---

## PG 库准备/清理记录

- 准备：服务启动自举（bootDatabase）——首启 `event=DB_BOOT database=taskapi_ch25_verify state=created`（裸 R2DBC SPI 建，CREATE DATABASE 不进事务块，沿 ch23 结论），随后 `event=DB_SCHEMA table=tasks ensured` 幂等建表。
- 数据跨重启核验：修复批重启后 GET /tasks 原样返回 16/17 两行（id 与 createdAt 不变）——验证库为真库非内存。
- 清理：`gradle dropTasksTable --console=plain`（exit 0），tasks 表 drop，验证库保留盘上：

```
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin UP-TO-DATE
> Task :compileJava NO-SOURCE
> Task :processResources UP-TO-DATE
> Task :classes UP-TO-DATE

> Task :dropTasksTable
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
tasks 表已清理（验证库 taskapi_ch25_verify 保留）

[Incubating] Problems report is available at: file:///D:/qiand-life/1-projects/xr-knowledge-suite/output/xr-know-020/code/kotlin-book/lab/ch25/task-api/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.8.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 8s
3 actionable tasks: 1 executed, 2 up-to-date
```

---

## 结构化日志面（AUTH/OPS 双 logger，launch 后台副作用实证）

OPS_* 由服务层 launch 派后台副作用任务写（变更落库后异步落一行，不阻塞响应）：

```
[DefaultDispatcher-worker-1] INFO lab.ch25.OpsAudit - event=OPS_CREATE id=18 title=审计打点 status=TODO
[DefaultDispatcher-worker-1] INFO lab.ch25.OpsAudit - event=OPS_DELETE id=18 title=审计打点 status=TODO
```

AUTH_* 由 jwt 插件 validate/challenge 写，401 分因 missing_token/bad_token 互斥、200 落 sub/role：

```
[eventLoopGroupProxy-4-1] WARN lab.ch25.AuthAudit - event=AUTH_401 path=/tasks reason=missing_token sub=-
[eventLoopGroupProxy-4-2] INFO lab.ch25.AuthAudit - event=AUTH_200 path=/tasks reason=ok sub=ops-user role=admin
[eventLoopGroupProxy-4-8] INFO lab.ch25.AuthAudit - event=AUTH_200 path=/tasks/16/transition reason=ok sub=ops-user role=admin
[eventLoopGroupProxy-4-10] INFO lab.ch25.AuthAudit - event=AUTH_200 path=/tasks/17/transition reason=ok sub=ops-user role=admin
[eventLoopGroupProxy-4-12] INFO lab.ch25.AuthAudit - event=AUTH_200 path=/stats reason=ok sub=ops-user role=admin
[eventLoopGroupProxy-4-1] WARN lab.ch25.AuthAudit - event=AUTH_401 path=/tasks reason=bad_token sub=-
```

---

## 坑位实录（ch25 当场新增，供 pitfalls.md 收录）

1. **reactive execute 不消费结果不报错**：`Mono.from(createStatement(...).execute()).block()` 在 r2dbc-postgresql 上可能不等服务端完成即正常返回——「库已存在」的服务端错误（42P04）被静默丢弃，二次启动照样打 `state=created` 误报。修复：`flatMap { Mono.from(it.rowsUpdated) }` 消费结果强制错误上浮；修复后重启如实上报 `state=exists`。ch23 的 catch 版恰好工作是因为错误在 block 内上浮的时序不同——reactive 面不消费 Result 就不算执行完，是通用坑。
2. **curl 中文直传被 shell 面 GBK 化**：`-d '{"title":"写周报"}'` 直传落库即坏（字节级验证：GBK 首字节 + U+FFFD 混杂）。对策：UTF-8 字节转义落文件 + `--data-binary @file`；HTTP 响应面逐字节复核为正 UTF-8。与既有「PS 5.1 中文转码坑」同族，bash 面同样要防。
3. **slf4j-simple 走 stderr**：只设 `-Dstdout.encoding=UTF-8` 时日志行中文仍乱（stderr 独立编码），补 `-Dstderr.encoding=UTF-8` 后 AUTH/OPS 行中文正常。
4. **JUnit @Test 方法隐式返回值即失联**：测试体最后一句是 assertThrows 时，`= runBlocking { ... }` 表达式体把返回类型推断成 `IllegalTransitionException` 而非 void——JUnit 只认 void 方法，该测试静默不执行（无报错、计数少一）。修复：显式标 `: Unit`。计数对账（XML tests 数 vs @Test 注解数）是唯一可靠的发现手段。

---

## 附：回执总检

- 逐字纪律：发①-⑧均单轮整块输出（`curl -s -i > _work/rec-*.log` 原样拷贝），中文响应体经 python 字节级复核（json.loads + 相等断言），无跨轮拼合、无手写编造。
- 环境注记：系统 Gradle 9.8.1 直跑；git 身份 = 随书仓 `output/xr-know-020/code/kotlin-book/.git`（rev-parse 验过）；本工程未 commit（留编排臂）。
- 对账日志：`lab/ch25/task-api/_work/`（server.log、rec-*.log、access.token、body-*.json；工程 .gitignore 排 `build/` `.gradle/`，_work 按全仓约定不入库）。
