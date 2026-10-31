# ch16 suspend-demo 实跑回执（receipts）

- 工程：`D:\qiand-life\1-projects\xr-knowledge-suite\code\kotlin-book\lab\ch16\suspend-demo`
- 实跑日期：2026-11-01
- 环境：Windows 11 Pro 10.0.22631 / JDK 21.0.6（Oracle LTS）/ Gradle 9.8.1（wrapper，发行版与案例仓同源腾讯镜像）/ Kotlin JVM 插件 2.4.21
- kotlinx-coroutines-core：**1.11.0**。版本出处：`version-lock.json` 无显式 coroutines 条目 → 取案例仓 `xr-code-suite/output/lab-management-system-kotlin`（Ktor 3.6.0 ktor-bom）`gradle dependencies --configuration runtimeClasspath` 实际解析结果 `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0`，非记忆填写
- 命令形式：本工程自带 Gradle wrapper（gradlew.bat + wrapper jar/properties 自案例仓原样收编），全程 `gradlew.bat <task> --console=plain`，工作目录 = 工程根
- 退出码汇总：编译反例失败轮 **exit 1** → 修复后复绿 **exit 0**；终态全量 `gradlew run` **exit 0**（四个演示全部实际执行，下述输出为逐字实录，时序数字按实测录入，正文引用时请按「约/量级」表述）

---

## 演示 1：blockedDemo（阻塞拖死线程池 vs 挂起让路）

命令：`gradlew.bat run --console=plain`（下同，四演示同一次 run 内顺序执行）

关键实测读数：线程池版瞬时任务等待 **4933ms**（约 5 秒，被三个 sleep 占坑任务拖死）；协程版（limitedParallelism(3)）瞬时协程等待 **1ms**（毫秒级，挂起不占线程）。

逐字输出（本节片段）：

```
========== 演示 1: blockedDemo（阻塞拖死线程池 vs 挂起让路） ==========
[blocked][+13ms][pool-1-thread-1] 占坑任务 0 开始 Thread.sleep(5000)
[blocked][+13ms][pool-1-thread-3] 占坑任务 2 开始 Thread.sleep(5000)
[blocked][+13ms][pool-1-thread-2] 占坑任务 1 开始 Thread.sleep(5000)
[blocked][+5040ms][pool-1-thread-3] 占坑任务 2 结束
[blocked][+5040ms][pool-1-thread-1] 占坑任务 0 结束
[blocked][+5040ms][pool-1-thread-2] 占坑任务 1 结束
[blocked][+5042ms][pool-1-thread-3] 瞬时任务终于开跑，从提交到执行实际等了 4933ms
[co][+46ms][DefaultDispatcher-worker-1] 占坑协程 0 开始 delay(5000)
[co][+46ms][DefaultDispatcher-worker-2] 占坑协程 1 开始 delay(5000)
[co][+47ms][DefaultDispatcher-worker-3] 占坑协程 2 开始 delay(5000)
[co][+155ms][DefaultDispatcher-worker-3] 瞬时协程开跑，从提交到执行实际等了 1ms
[co][+5053ms][DefaultDispatcher-worker-3] 占坑协程 1 结束
[co][+5053ms][DefaultDispatcher-worker-1] 占坑协程 0 结束
[co][+5053ms][DefaultDispatcher-worker-2] 占坑协程 2 结束
```

---

## 演示 2：firstSuspendDemo（首个 suspend 函数 + 线程释放观察）

关键实测读数：进入与返回线程名**同为 main**（单线程事件循环原地恢复）；等待期间第二个协程在 **+216ms**（挂起窗口 [0,1011ms) 内）在同一线程插队执行。

逐字输出（本节片段）：

```
========== 演示 2: firstSuspendDemo（首个 suspend 函数 + 线程释放观察） ==========
[first-suspend][+0ms][main] 进入 runBlocking，即将调用 slowFetch()
[first-suspend][+216ms][main] 池子里的线程在干别的活（第二个协程插队执行）
[first-suspend][+1011ms][main] slowFetch() 返回：模拟网络数据（对比进入/返回两条日志的线程名）
```

---

## 演示 3：manyCoroutinesDemo（单线程 10 万协程）

关键实测读数：100_000 个协程在单线程 `ch16-single` 上 launch 阶段 **204ms**，全部 join 完毕总耗时 **1357ms**（约 1 秒出头 = 一次 delay(1000) 的墙钟时间，与协程数量基本无关）。

逐字输出（本节片段）：

```
========== 演示 3: manyCoroutinesDemo（单线程 10 万协程） ==========
[many][+5ms][ch16-single] 第 0 号协程开始 delay(1000)
[many][+212ms][main] 100000 个协程 launch 完成，launch 阶段耗时 204ms（此刻该单线程上挂起的活跃协程数量级：10 万，线程数仍是 1）
[many][+1357ms][main] 100000 个协程全部 join 完毕，总耗时 1357ms
```

---

## 演示 4：p05DrillDemo（P05 判据演练）

正例（suspendingWait，体内 delay）：watcher 最大 tick 间隔 **211ms** ≈ 名义周期 200ms，未被饿住。
反例（blockingWait，体内 Thread.sleep）：watcher 被饿住，从 tick @1ms 直接跳到等待返回，最大间隔 **1011ms** ≈ 一个完整阻塞时长（与正例的时间差约 **800ms**，与名义周期差约 **810ms**；绝对意义上被饿住了约 **1000ms**）。

逐字输出（本节片段）：

```
========== 演示 4: p05DrillDemo（P05 判据演练：正例/反例） ==========
[p05-ok][+13ms][p05-ok-single] watcher tick @ 1ms
[p05-ok][+13ms][p05-ok-single] 被测等待开始（p05-ok）
[p05-ok][+215ms][p05-ok-single] watcher tick @ 212ms
[p05-ok][+416ms][p05-ok-single] watcher tick @ 413ms
[p05-ok][+619ms][p05-ok-single] watcher tick @ 616ms
[p05-ok][+823ms][p05-ok-single] watcher tick @ 820ms
[p05-ok][+1026ms][p05-ok-single] 被测等待返回
[p05-ok][+1027ms][main] watcher 最大 tick 间隔 = 211ms（tick 名义周期 200ms；含等待返回边界测点）

[p05-bad][+2ms][p05-bad-single] watcher tick @ 1ms
[p05-bad][+2ms][p05-bad-single] 被测等待开始（p05-bad）
[p05-bad][+1013ms][p05-bad-single] 被测等待返回
[p05-bad][+1013ms][main] watcher 最大 tick 间隔 = 1011ms（tick 名义周期 200ms；含等待返回边界测点）
```

工程备注：maxGap 计算把「等待返回」时刻并入测点序列——反例被饿住时 watcher 在空洞前往往只来得及打一个 tick，只按相邻 tick 相减会得出 0ms 假读数（首跑实测踩中后修正，修正前输出为 `watcher 最大 tick 间隔 = 0ms`）。

---

## P05 编译反例：普通函数直调 delay（先失败 → 后复绿）

### 第一轮：失败态（故意保留的反例版本）

反例源码（修复前，快照留档 `compile-fail/NonSuspendCall.before-fix.kt`）：

```kotlin
package lab.ch16

import kotlinx.coroutines.delay

/**
 * P05 编译反例（待修复态）：普通函数直接调 delay。
 * 这个文件故意停留在编译不过的状态跑一轮 compileKotlin 拿逐字报错，
 * 报错留档 receipts.md 后，本文件才被修复为 suspend 版复绿。
 */
fun notReallySuspending() {
    // delay 是 suspend 函数，普通函数体内没有挂起语境，编译器在此拦截
    delay(1000)
}
```

命令：`gradlew.bat compileKotlin --console=plain`

**退出码：1**

stdout（逐字）：

```
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED

> Task :compileKotlin FAILED

[Incubating] Problems report is available at: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch16/suspend-demo/build/reports/problems/problems-report.html
1 actionable task: 1 executed
```

stderr（逐字，完整报错文本）：

```
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch16/suspend-demo/src/main/kotlin/lab/ch16/NonSuspendCall.kt:12:5 Suspend function 'suspend fun delay(timeMillis: Long): Unit' can only be called from a coroutine or another suspend function.

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':compileKotlin' (registered by plugin 'org.jetbrains.kotlin.jvm').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: ILLEGAL_SUSPEND_FUNCTION_CALL
       Kotlin compiler error
         Suspend function 'suspend fun delay(timeMillis: Long): Unit' can only be called from a coroutine or another suspend function.
         Location: D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch16/suspend-demo/src/main/kotlin/lab/ch16/NonSuspendCall.kt:12

* Try:
> Run with --stacktrace option to get the stack trace
> Run with --info or --debug option to get more log output
> Run with --scan to get full insights from a Build Scan (powered by Develocity)
```

报错核心行（逐字）：`Suspend function 'suspend fun delay(timeMillis: Long): Unit' can only be called from a coroutine or another suspend function.`，编译器诊断码 `ILLEGAL_SUSPEND_FUNCTION_CALL`。

> 措辞备注：Kotlin 2.4.21（K2 编译器）实测报错为上述措辞；正文若要引用「suspending function can only be called from within a coroutine body」类旧措辞，请以本回执实测文本为准，不要手写报错。

### 第二轮：修复后复绿（标 suspend）

修复方式：给 `notReallySuspending` 加 `suspend` 修饰符（修复后源码 = `src/main/kotlin/lab/ch16/NonSuspendCall.kt` 终态）。

命令：`gradlew.bat compileKotlin --console=plain`

**退出码：0**

stdout（逐字）：

```
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin

BUILD SUCCESSFUL in 7s
1 actionable task: 1 executed
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

---

## 终态全量验证

命令：`gradlew.bat run --console=plain`（工程终态：含已修复的 NonSuspendCall.kt）

**退出码：0**

完整 stdout（逐字，含 gradle 任务行）：

```
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin UP-TO-DATE
> Task :compileJava NO-SOURCE
> Task :processResources NO-SOURCE
> Task :classes UP-TO-DATE

> Task :run
========== 演示 1: blockedDemo（阻塞拖死线程池 vs 挂起让路） ==========
[blocked][+13ms][pool-1-thread-1] 占坑任务 0 开始 Thread.sleep(5000)
[blocked][+13ms][pool-1-thread-3] 占坑任务 2 开始 Thread.sleep(5000)
[blocked][+13ms][pool-1-thread-2] 占坑任务 1 开始 Thread.sleep(5000)
[blocked][+5040ms][pool-1-thread-3] 占坑任务 2 结束
[blocked][+5040ms][pool-1-thread-1] 占坑任务 0 结束
[blocked][+5040ms][pool-1-thread-2] 占坑任务 1 结束
[blocked][+5042ms][pool-1-thread-3] 瞬时任务终于开跑，从提交到执行实际等了 4933ms
[co][+46ms][DefaultDispatcher-worker-1] 占坑协程 0 开始 delay(5000)
[co][+46ms][DefaultDispatcher-worker-2] 占坑协程 1 开始 delay(5000)
[co][+47ms][DefaultDispatcher-worker-3] 占坑协程 2 开始 delay(5000)
[co][+155ms][DefaultDispatcher-worker-3] 瞬时协程开跑，从提交到执行实际等了 1ms
[co][+5053ms][DefaultDispatcher-worker-3] 占坑协程 1 结束
[co][+5053ms][DefaultDispatcher-worker-1] 占坑协程 0 结束
[co][+5053ms][DefaultDispatcher-worker-2] 占坑协程 2 结束

========== 演示 2: firstSuspendDemo（首个 suspend 函数 + 线程释放观察） ==========
[first-suspend][+0ms][main] 进入 runBlocking，即将调用 slowFetch()
[first-suspend][+216ms][main] 池子里的线程在干别的活（第二个协程插队执行）
[first-suspend][+1011ms][main] slowFetch() 返回：模拟网络数据（对比进入/返回两条日志的线程名）

========== 演示 3: manyCoroutinesDemo（单线程 10 万协程） ==========
[many][+5ms][ch16-single] 第 0 号协程开始 delay(1000)
[many][+212ms][main] 100000 个协程 launch 完成，launch 阶段耗时 204ms（此刻该单线程上挂起的活跃协程数量级：10 万，线程数仍是 1）
[many][+1357ms][main] 100000 个协程全部 join 完毕，总耗时 1357ms

========== 演示 4: p05DrillDemo（P05 判据演练：正例/反例） ==========
[p05-ok][+13ms][p05-ok-single] watcher tick @ 1ms
[p05-ok][+13ms][p05-ok-single] 被测等待开始（p05-ok）
[p05-ok][+215ms][p05-ok-single] watcher tick @ 212ms
[p05-ok][+416ms][p05-ok-single] watcher tick @ 413ms
[p05-ok][+619ms][p05-ok-single] watcher tick @ 616ms
[p05-ok][+823ms][p05-ok-single] watcher tick @ 820ms
[p05-ok][+1026ms][p05-ok-single] 被测等待返回
[p05-ok][+1027ms][main] watcher 最大 tick 间隔 = 211ms（tick 名义周期 200ms；含等待返回边界测点）

[p05-bad][+2ms][p05-bad-single] watcher tick @ 1ms
[p05-bad][+2ms][p05-bad-single] 被测等待开始（p05-bad）
[p05-bad][+1013ms][p05-bad-single] 被测等待返回
[p05-bad][+1013ms][main] watcher 最大 tick 间隔 = 1011ms（tick 名义周期 200ms；含等待返回边界测点）

BUILD SUCCESSFUL in 21s
2 actionable tasks: 1 executed, 1 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html

BUILD SUCCESSFUL in 21s
2 actionable tasks: 1 executed, 1 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```
