# structured-concurrency-demo 实测回执（ch17）

环境：系统 Gradle 9.8.1 直跑（不用 wrapper），`JAVA_TOOL_OPTIONS=-Dstdout.encoding=UTF-8`。

依赖口径：`org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0`——出处为家族案例仓
`xr-code-suite/output/lab-management-system-android/gradle/libs.versions.toml` 实锁
`coroutines = "1.11.0"`（该仓 version-lock.json 同口径）。主仓
`output/xr-know-020/version-lock.json` 未单列 kotlinx-coroutines（仅 kotlinx-serialization 1.9.0），
属 version-lock 缺口，待回填；回填后本工程依赖行需与主仓口径核对。

> 工程修正备注（实测中发现并修复，非 runbook 预案，共两处）：
>
> 1. 演示 4 首跑自锁：`runBlocking` 里的 `launch` 不带调度器时继承其**单线程事件循环**，
>    子协程的 CPU 死循环把主线程堵死，父协程 `delay(300)` 永远无法恢复，输出停在演示头
>    （首跑 JVM 挂死，手动 kill 后修复）。最小修复：演示 4 两处父协程改为
>    `launch(Dispatchers.Default)`，子协程随父继承 Default。
> 2. 演示 5(c) 首跑炸出：把 `async` 直接放进 `runBlocking`，await 处 catch 虽接到异常，
>    但 async 子协程失败**同时**沿 Job 层级取消了父作用域，`runBlocking` 末尾仍被炸出
>    （`Exception in thread "main" java.lang.IllegalStateException`，:run FAILED，exit 1）。
>    最小修复：(c) 的 async 根在独立作用域 `CoroutineScope(Job() + Dispatchers.Default)`
>    上，隔离出「Deferred 交付」一条线。此现象本身是结构化并发机制的活教材，正文引用时
>    可作为脚注口径：await 处 try-catch 接到的是值，拦不住父作用域被掀翻。
>
> 两处修复后 BUILD SUCCESSFUL，全绿回执如下。

## 全绿回执（五演示）

命令：`gradle run`（工程：lab/ch17/structured-concurrency-demo）

```text
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :processResources NO-SOURCE
> Task :compileKotlin
> Task :compileJava NO-SOURCE
> Task :classes UP-TO-DATE

> Task :run
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
========== 演示 1: leakDemo（GlobalScope 泄漏复盘：作用域退了协程还活着） ==========
[leak][+245ms][DefaultDispatcher-worker-2] 业务作用域：活干完了，退场
[leak][+254ms][main] 业务作用域 Job 完结。GlobalScope 里的协程无人管辖，tick 还会继续出现
[leak][+547ms][DefaultDispatcher-worker-2] 泄漏协程 tick 1：业务作用域早已退场，我还活着
[leak][+1056ms][DefaultDispatcher-worker-2] 泄漏协程 tick 2：业务作用域早已退场，我还活着
[leak][+1569ms][DefaultDispatcher-worker-2] 泄漏协程 tick 3：业务作用域早已退场，我还活着
[leak][+2083ms][DefaultDispatcher-worker-2] 泄漏协程 tick 4：业务作用域早已退场，我还活着
---------- 演示 1 结束，本段耗时 2084ms ----------
========== 演示 2: scopeTreeDemo（父子层级：作用域一退协程全收） ==========
[tree][+172ms][DefaultDispatcher-worker-1] 子协程 3 tick 1
[tree][+172ms][DefaultDispatcher-worker-3] 子协程 1 tick 1
[tree][+172ms][DefaultDispatcher-worker-2] 子协程 2 tick 1
[tree][+323ms][DefaultDispatcher-worker-2] 子协程 3 tick 2
[tree][+323ms][DefaultDispatcher-worker-3] 子协程 1 tick 2
[tree][+323ms][DefaultDispatcher-worker-1] 子协程 2 tick 2
[tree][+421ms][main] cancel 前：作用域名下 children 计数 = 3
[tree][+423ms][main] scope.cancel() 已调用，joinAll 等三个子协程收工（父等子）
[tree][+424ms][main] cancel 后：children 计数 = 0
[tree][+424ms][main] 结论：作用域一退，3 个子协程全部收工，一个不漏
---------- 演示 2 结束，本段耗时 424ms ----------
========== 演示 3: p06DrillDemo（P06 选型演练：async 要值 / launch 干活） ==========
[p06][+1006ms][main] (a) async 并发：三科总分 275，总耗时 1005ms（约等于一个任务的 1s，不是三个相加）
[p06][+4026ms][main] (a-对照) 顺序 await：总分 275，总耗时 3020ms（若顺序 await 则 3s：选型错了，并发白给）
[p06][+4028ms][main] (b) launch 副作用任务「语文」干完，无返回值可接
[p06][+4028ms][main] (b) launch 副作用任务「数学」干完，无返回值可接
[p06][+4028ms][main] (b) launch 副作用任务「英语」干完，无返回值可接
[p06][+4029ms][main] (b) launch 版：joinAll 收工，总耗时 2ms（毫秒级：没有等待可重叠，join 只是等收尾）
---------- 演示 3 结束，本段耗时 4032ms ----------
========== 演示 4: cancelChainDemo（取消传播与 ensureActive 协作式检查） ==========
[cancel][+306ms][DefaultDispatcher-worker-3] 父协程：调用 child.cancel()
[cancel][+306ms][DefaultDispatcher-worker-1] 子协程：在第 222600000 轮后的 ensureActive() 接到取消，收工
[cancel][+308ms][DefaultDispatcher-worker-1] 父协程感知：child.join() 返回，子协程已收工
[cancel][+618ms][DefaultDispatcher-worker-3] 对照组父协程：调用 child.cancel()
[cancel][+2307ms][DefaultDispatcher-worker-1] 对照组子协程：裸循环转了 673552737 轮，到时限自停——cancel 传到了但没人理
[cancel][+2307ms][DefaultDispatcher-worker-1] 对照组父协程感知：join() 返回——但子协程不是被取消停的，是自己跑满时限
---------- 演示 4 结束，本段耗时 2307ms ----------
========== 演示 5: exceptionBoundaryDemo（异常边界三对照） ==========
[exc][+113ms][main] (a) 协程体内：即将抛出 IllegalStateException
[exc][+113ms][main] (a) 调用点 try-catch：没接到——异常沿 Job 层级一路掀到根（runBlocking）才现身：launch 体内的异常
[exc][+232ms][DefaultDispatcher-worker-1] (b) 根作用域 handler：接到了——IllegalStateException: 同款异常
[exc][+232ms][main] (b) 结论：handler 接住了，作用域没有崩，进程继续
[exc][+342ms][main] (c) await 处 try-catch：接到了——async 体内的异常（async 的异常憋在 Deferred 里，谁来取就在取处抛）
---------- 演示 5 结束，本段耗时 342ms ----------

BUILD SUCCESSFUL in 16s
2 actionable tasks: 2 executed
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

EXIT=0。

## 耗时读数（供正文引用）

| 演示 | 本段耗时 | 关键读数 |
|------|---------|---------|
| 1 leakDemo | 2084ms | 4 声 tick 各隔约 500ms，全部落在「业务作用域退场」之后 |
| 2 scopeTreeDemo | 424ms | cancel 前 children=3，cancel 后 children=0 |
| 3 p06DrillDemo | 4032ms | async 并发 1005ms vs 顺序 await 3020ms；launch 版 join 2ms |
| 4 cancelChainDemo | 2307ms | 正例 cancel 后同毫秒收工（2.2 亿轮处的 ensureActive）；对照组空转 2s 跑满 6.7 亿轮自停 |
| 5 exceptionBoundaryDemo | 342ms | 三段结论行：没接到 / 接到了 / 接到了 |

五段合计约 9.2 秒（不含编译），单次总时长满足 ≤30 秒约束。

## 可复跑性口径

- 演示 1 的 GlobalScope 协程限定 4 声 tick 后自停、main 持引用 join——这是**演示闸**，
  真实事故没有这道闸；注释与正文引用时须带此限定语。
- 演示 4 对照组裸循环带 2 秒时限自停，同为**演示闸**（否则进程永不退出）。
- tick/轮数/毫秒读数每次运行会有个位数毫秒漂移与轮数量级波动，属正常；正文引用取
  「约 500ms 一声」「约 1s / 约 3s」「同毫秒收工 / 跑满 2 秒」档位，不逐字锁死数字。
