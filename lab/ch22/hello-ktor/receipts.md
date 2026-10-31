# ch22 hello-ktor 实测回执

- 日期：2026-11-01
- 工程路径：`output/xr-know-020/code/kotlin-book/lab/ch22/hello-ktor/`
- 仓身份：`output/xr-know-020/code/kotlin-book/.git` 存在（`test -d` → `GIT_OK`）
- 工具链：系统 Gradle 9.8.1（无 wrapper，与 ch21 在册口径一致）、JDK 21.0.6 LTS（Oracle）、jvmToolchain(21)
- 在册约定核对：ch21 `quality-gate-demo` 无 version catalog（libs.versions.toml 不在册），kotlin("jvm") 2.4.21；ch22 按任务单自建 `gradle/libs.versions.toml`，kotlin 版本沿用 2.4.21
- 端口：8090（application.conf 指定，避开 8080）
- 启动方式：EngineMain + application.conf（`gradle run`，application 插件 mainClass 指向 `io.ktor.server.netty.EngineMain`）

## 1. 构建

命令：`gradle build`（在工程目录内，首次构建）

```
> Task :build

BUILD SUCCESSFUL in 13s
3 actionable tasks: 3 executed
```

挂钟耗时（`time gradle build`）：`real 0m13.950s`（依赖大部分本地缓存命中，未发生数分钟级首次拉取）

### 反例留档（首轮失败，已修复复绿）

首次 `gradle run` 失败：build.gradle.kts 未挂 `application` 插件，无 `run` 任务。逐字留档：

```
* What went wrong:
Selection failed
  Task 'run' not found in root project 'hello-ktor'.

BUILD FAILED in 6s
```

修复：build.gradle.kts 增加 `application` 插件与 `application { mainClass = "io.ktor.server.netty.EngineMain" }`，重跑成功。源文件以修复后版本为准（见下方第 5 节清单）。

## 2. 端点逐字回执（服务进程 PID 32432，java 启动时间 2026/11/1 04:54:56）

### 2.1 GET /

```
$ curl -s -i http://localhost:8090/
HTTP/1.1 200 OK
Content-Length: 62
Content-Type: text/plain; charset=UTF-8

Hello, Ktor! Config loaded from application.conf on port 8090.
```

### 2.2 GET /hello/Ktor

```
$ curl -s -i http://localhost:8090/hello/Ktor
HTTP/1.1 200 OK
Content-Length: 37
Content-Type: text/plain; charset=UTF-8

Hello, Ktor! Welcome to Ktor routing.
```

### 2.3 GET /api/books

```
$ curl -s -i http://localhost:8090/api/books
HTTP/1.1 200 OK
Content-Length: 183
Content-Type: application/json

[{"id":1,"title":"Kotlin in Action","author":"Dmitry Jemerov"},{"id":2,"title":"Atomic Kotlin","author":"Bruce Eckel"},{"id":3,"title":"Programming Kotlin","author":"Stephen Samuel"}]
```

### 2.4 GET /api/books/1

```
$ curl -s -i http://localhost:8090/api/books/1
HTTP/1.1 200 OK
Content-Length: 61
Content-Type: application/json

{"id":1,"title":"Kotlin in Action","author":"Dmitry Jemerov"}
```

### 2.5 GET /api/books/999（故意失败，404 分支）

```
$ curl -s -i http://localhost:8090/api/books/999
HTTP/1.1 404 Not Found
Content-Length: 30
Content-Type: application/json

{"error":"Book 999 not found"}
```

### 2.6 GET /no-such-route（Ktor 默认 404）

```
$ curl -s -i http://localhost:8090/no-such-route
HTTP/1.1 404 Not Found
Content-Length: 0
```

## 3. 热重载实测（改前/改后逐字对比）

### 3.1 改前响应

```
$ curl -s http://localhost:8090/
Hello, Ktor! Config loaded from application.conf on port 8090.
```

### 3.2 改动

`src/main/kotlin/helloktor/Application.kt` 中 `/` 的 respondText 改为：

```
"Hello, Ktor! Hot reload works: this text changed without restarting the process."
```

### 3.3 重编译（不重启服务进程，第二次 gradle 调用）

```
$ gradle classes
> Task :classes UP-TO-DATE

BUILD SUCCESSFUL in 15s
2 actionable tasks: 1 executed, 1 up-to-date
```

（`1 executed` 即 compileKotlin：class 文件 mtime 全部刷新为 04:55:42，输出目录 `build/classes/kotlin/main/helloktor/`）

### 3.4 改后响应

```
$ curl -s -m 3 http://localhost:8090/
Hello, Ktor! Hot reload works: this text changed without restarting the process.
```

### 3.5 同进程铁证

```
$ netstat -ano | grep ":8090"    → LISTENING  PID 32432
$ powershell Get-Process -Id 32432 → java  StartTime 2026/11/1 4:54:56
```

时间线：进程启动 04:54:56 → 源码修改+重编译 04:55:42 → 新文本已由同一 PID 32432 返回。进程未重启，响应已变。

### 3.6 机制说明

启动路线 = EngineMain + application.conf，配置 `ktor.development = true` 与 `ktor.watch.paths = [ "hello-ktor" ]`。Ktor 开发模式监视 classpath 中路径命中 watch.paths 的条目（本工程命中 `build/classes/kotlin/main`，其绝对路径含 `hello-ktor`）；监视到编译产物变化后，Ktor 在**同一 JVM 进程内**重载应用模块（重建应用实例），无需重启进程。重编译由另开的 `gradle classes` 触发（运行中的 `gradle run` 与第二次 gradle 调用未发生锁冲突）。注：classpath 未挂 SLF4J provider（NOP logger），故日志层无重载痕迹，证据以进程 PID/启动时间 + 响应文本变化为准。

## 4. 偏差记录

1. ch21 在册工程无 version catalog，ch22 的 libs.versions.toml 为本工程新建（kotlin 2.4.21 沿用在册口径）；catalog 中每个 Ktor 模块带 version.ref 仅为目录格式合法，实际版本约束以 `implementation(platform(libs.ktor.bom))` 收口。
2. `gradle run` 首轮因缺 application 插件失败，已修复（反例逐字留档于第 1 节）。
3. 热重载最终源码中 `/` 的问候文本保留为"改后"文本（与第 3.4 节逐字一致）；写臂落正文清单以第 5 节最终版为准。
4. Ktor 依赖版本任务单指定 3.6.0、kotlinx-serialization 1.9.0，实际解析成功（BUILD SUCCESSFUL），无降版本。

## 5. 最终源文件（逐字，写臂落正文清单以此为准）

见正文回报，与盘上文件一致：

- `build.gradle.kts` / `settings.gradle.kts` / `gradle/libs.versions.toml` / `.gitignore` / `src/main/resources/application.conf` / `src/main/kotlin/helloktor/Application.kt`

## 6. embeddedServer 直启变体实测（2026-11-01 补充，端口 8091，EngineMain 侧 8090 全程未动）

新增源文件 `src/main/kotlin/helloktor/EmbeddedServerDemo.kt`（逐字，一次编译通过，未改任何既有文件）：

```kotlin
package helloktor

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

// 直启变体：embeddedServer 把服务嵌进本进程，端口作为参数写死，
// 用于最小起步演示；配置文件与热重载走 application.conf + EngineMain 一路。
fun main() {
    embeddedServer(Netty, port = 8091, module = Application::module).start(wait = true)
}
```

与 Application.kt 的 `fun main(args: Array<String>)` 同包并存不冲突：两个 main 落在不同 facade 类（`ApplicationKt` / `EmbeddedServerDemoKt`），`gradle build` 一次通过。

### 6.1 构建

命令：`gradle build`（工程目录内，新增 EmbeddedServerDemo.kt 后）

```
BUILD SUCCESSFUL in 9s
6 actionable tasks: 5 executed, 1 up-to-date
```

挂钟耗时（`time gradle build`）：`real 0m9.624s`

### 6.2 启动（copyRuntimeDeps classpath 直启，不用 gradle run）

先 `gradle build`（内含 classes 编译），运行时依赖目录 `build/runtime-deps/` 由在册任务 `copyRuntimeDeps` 产出（此前已拷齐）。实际启动命令逐字（Git Bash，工程目录内，classpath Windows 分号分隔）：

```
java -cp "build/classes/kotlin/main;build/classes/java/main;build/runtime-deps/*" helloktor.EmbeddedServerDemoKt
```

入口类 = `helloktor.EmbeddedServerDemoKt`（embeddedServer 变体的 main；EngineMain 的 main 仍在 `helloktor.ApplicationKt`，application 插件 mainClass 未改，`installDist`/`gradle run` 路线不受影响）。启动日志逐字（classpath 无 SLF4J provider，与第 3.6 节口径一致，无 Netty 启动横幅）：

```
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
```

进程监听确认（`netstat -ano | grep ":8091"`）：`LISTENING PID 37936`；同刻 8090 仍由原进程 PID 32432 监听，两实例并存。

### 6.3 端点逐字回执（服务进程 PID 37936）

#### GET /hello/Ktor（正文清单 22-3 用）

```
$ curl -s -i http://localhost:8091/hello/Ktor
HTTP/1.1 200 OK
Content-Length: 37
Content-Type: text/plain; charset=UTF-8

Hello, Ktor! Welcome to Ktor routing.
```

#### GET /

```
$ curl -s -i http://localhost:8091/
HTTP/1.1 200 OK
Content-Length: 80
Content-Type: text/plain; charset=UTF-8

Hello, Ktor! Hot reload works: this text changed without restarting the process.
```

（内容与第 3.4 节"改后"文本一致：直启变体与 EngineMain 共用同一 `Application.module()`，路由表与文案完全相同——这正是两条启动路线只换装配入口、不换业务装配的证据。）

#### GET /api/books

```
$ curl -s -i http://localhost:8091/api/books
HTTP/1.1 200 OK
Content-Length: 183
Content-Type: application/json

[{"id":1,"title":"Kotlin in Action","author":"Dmitry Jemerov"},{"id":2,"title":"Atomic Kotlin","author":"Bruce Eckel"},{"id":3,"title":"Programming Kotlin","author":"Stephen Samuel"}]
```

（同一实例 ContentNegotiation 生效，JSON 出得来：`install` 在 module() 内，与启动方式无关。）

### 6.4 收尾

```
$ taskkill /F /PID 37936
成功: 已终止 PID 为 37936 的进程。
```

杀后 `netstat -ano | grep ":8091"` 无 LISTENING 行，端口释放；8090（PID 32432）未受影响。

杀后复跑 `gradle build`：

```
BUILD SUCCESSFUL in 7s
6 actionable tasks: 6 up-to-date
```

挂钟耗时：`real 0m7.601s`。全绿。

### 6.5 本节偏差记录

1. 工程无 gradle wrapper，用系统 Gradle 9.8.1（与第 1 节口径一致）。
2. 启动未走 `installDist` bin 脚本，走 copyRuntimeDeps classpath 直启：installDist 的 bin 脚本 mainClass 固定指向 EngineMain，直启 embeddedServer 的 main 必须自己拼 classpath，正好演示两条路线的入口差异。
3. EmbeddedServerDemo.kt 按任务单原文一次编译通过，未做任何 API 适配。

## 7. 22.4 端点补充回执（8091 直启实例，2026-11-01）

22.4 节正文三发请求（单条命中 / 路由内 404 / 路由外 404 默认应答）在 8091 直启实例上补实测。第 1-6 节未改动。

### 7.1 构建与启动

构建确认（产物已在位，全部 UP-TO-DATE）：

```
$ gradle classes copyRuntimeDeps

> Task :compileJava NO-SOURCE
> Task :processResources UP-TO-DATE
> Task :classes UP-TO-DATE
> Task :copyRuntimeDeps UP-TO-DATE

BUILD SUCCESSFUL in 6s
3 actionable tasks: 3 up-to-date
```

启动命令逐字（与 6.2 节同款，Git Bash，工程目录内）：

```
java -cp "build/classes/kotlin/main;build/classes/java/main;build/runtime-deps/*" helloktor.EmbeddedServerDemoKt
```

启动日志逐字（与 6.2 节口径一致，无 Netty 横幅）：

```
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
```

进程监听确认（`netstat -ano | grep ":8091"`）：`LISTENING PID 18640`（IPv4 与 IPv6 双栈行）。启动后约 1 秒内可应答（首轮探活即 200）。

### 7.2 三发逐字回执（curl -s -i，状态行+头+体全录）

#### GET /api/books/1（预期 200 JSON 单条，实际吻合）

```
$ curl -s -i http://localhost:8091/api/books/1
HTTP/1.1 200 OK
Content-Length: 61
Content-Type: application/json

{"id":1,"title":"Kotlin in Action","author":"Dmitry Jemerov"}
```

#### GET /api/books/999（预期 404 JSON 错误体，实际吻合）

```
$ curl -s -i http://localhost:8091/api/books/999
HTTP/1.1 404 Not Found
Content-Length: 30
Content-Type: application/json

{"error":"Book 999 not found"}
```

#### GET /no-such-route（预期 404 默认应答，实际吻合——无路由命中走 Ktor 默认 404，空体无 Content-Type）

```
$ curl -s -i http://localhost:8091/no-such-route
HTTP/1.1 404 Not Found
Content-Length: 0
```

### 7.3 收尾

```
$ taskkill /F /PID 18640
成功: 已终止 PID 为 18640 的进程。
```

杀后 `netstat -ano | grep ":8091"` 无 LISTENING 行，端口释放。

杀后复跑 `gradle build`：

```
BUILD SUCCESSFUL in 6s
6 actionable tasks: 6 up-to-date
```

全绿。

### 7.4 本节偏差记录

1. 无偏差：三发状态码、Content-Type、响应体均与 22.4 节正文叙事一致（单条 JSON / 路由内 JSON 错误体 / 路由外默认空体 404）。
2. 本次为独立新进程（PID 18640），非第 6 节 PID 37936 复用；8090（EngineMain 侧）全程未动。
