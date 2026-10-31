# flow-demo 实测回执（ch18）

工程：`lab/ch18/flow-demo`（沿 ch17 structured-concurrency-demo 同族体例）。
口径：系统 Gradle 9.8.1 直跑（不用 wrapper）· JDK 21（jvmToolchain）· Kotlin JVM 插件 2.4.21 ·
`org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0`——版本出处为主仓
`output/xr-know-020/version-lock.json` 已回填 kotlinx-coroutines=1.11.0
（2026-11-01，双源实证：android 案例仓 libs.versions.toml + ch16/ch17 lab 依赖树实测）。
跑法：`JAVA_TOOL_OPTIONS=-Dstdout.encoding=UTF-8 gradle --console=plain run`。

coroutines 1.11.0 解析实证（`gradle dependencies --configuration runtimeClasspath` 抽行）：

```text
\--- org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0
     \--- org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.11.0
```

> 工程修正备注（实测中发现并修复，非 runbook 预案，共两处）：
>
> 1. **全工程编译失败：无作用域裸调 `launch` 被升为编译错误**。coroutines 1.11.0 把顶层
>    `launch` 重载标了 DEPRECATION_ERROR，演示函数若是普通 `suspend fun`，体内 `launch`
>    就落到该重载上，8 处全炸（`'launch' can not be called without the corresponding
>    coroutine scope`，exit 1）。最小修复：六个演示与 `runDemo` 全部改为
>    `CoroutineScope` 扩展函数，由 `main` 的 `runBlocking` 作用域承接。这本身是
>    1.11.0 的 API 卫生信号，正文引用可作为脚注口径：裸 `GlobalScope.launch` 之外，
>    连无接收者的 `suspend fun` 里调 `launch` 都不再编译通过。
> 2. **演示 5 两轮参数演化（debounce 与间隙的物理不相容）**。任务卡原案「kot 停 500ms
>    再发 kotlinx，debounce(300) 只放行 kotlinx」物理不成立：静默满 300ms debounce
>    必然冲刷落盘，首跑实测 KOT!、KOTLINX! 两条都落到订阅者。先改 200ms，二跑实测
>    Windows 定时器过冲（计划 200ms 实走 312ms）仍越窗，KOT! 再次漏网。终版 100ms
>    （过冲后实测 218/219ms，距 300ms 静默窗余量 80ms+），连续两跑均只有 KOTLINX!
>    落地。正文写作口径：**「停 500ms 再发」的叙事必须改掉**——那一幕里 debounce 恰恰
>    会把前面的值放行；正确叙事是「kotlinx 赶在 300ms 静默窗关闭之前赶到，把 KOT!
>    顶掉，而它自己靠尾部 500ms 静默窗落地」。
>
> 两处修复后连续两跑 BUILD SUCCESSFUL、六演示逐字稳定（时序打点数值允许微抖），
> 全绿回执如下（取稳定第二遍）。

## 全绿回执（六演示）

命令：`gradle run`（工程：lab/ch18/flow-demo），第二遍。

```text
========== 演示 1: liveChainDemo（界面实时链路：数据一变，订阅方立即收到） ==========
[live][+14ms][main] 界面收到：库存快照 v0
[live][+166ms][main] 仓库写入 v1
[live][+166ms][main] 界面收到：库存快照 v1
[live][+319ms][main] 仓库写入 v2
[live][+319ms][main] 界面收到：库存快照 v2
[live][+477ms][main] 仓库写入 v3
[live][+477ms][main] 界面收到：库存快照 v3
---------- 演示 1 结束，本段耗时 635ms ----------
========== 演示 2: coldHotDemo（P07 冷热对照：上游跑两遍 vs 订阅者共享一遍） ==========
--- (a) 冷流 flow{}：两个收集者先后 collect，上游跑两遍 ---
[cold][+3ms][main] 收集者一 collect
[cold][+4ms][main] 冷流上游：开始生产
[cold][+11ms][main] 收集者一拿到：1
[cold][+163ms][main] 收集者一拿到：2
[cold][+318ms][main] 收集者一拿到：3
[cold][+318ms][main] 收集者二 collect
[cold][+318ms][main] 冷流上游：开始生产
[cold][+319ms][main] 收集者二拿到：1
[cold][+474ms][main] 收集者二拿到：2
[cold][+632ms][main] 收集者二拿到：3
--- (b) 热流 MutableStateFlow：上游只生产一遍，订阅者共享 ---
[hot][+1ms][main] 订阅者 A 收到：0
[hot][+201ms][main] 订阅者 A 收到：1
[hot][+406ms][main] 订阅者 A 收到：2
[hot][+609ms][main] 订阅者 B 收到：2
[hot][+810ms][main] 订阅者 A 收到：3
[hot][+810ms][main] 订阅者 B 收到：3
---------- 演示 2 结束，本段耗时 1645ms ----------
========== 演示 3: stateFlowDemo（P08 状态承载三性质一次演全） ==========
[state][+0ms][main] 收集者收到：首页 v0
[state][+154ms][main] 赋与当前相等的值：v0 —— 观察下面 150ms 内有没有新行
[state][+309ms][main] 赋新值：v1
[state][+309ms][main] 收集者收到：首页 v1
---------- 演示 3 结束，本段耗时 464ms ----------
========== 演示 4: sharedFlowDemo（事件广播：replay 重放与一次性事件） ==========
--- (a) replay=2：迟到订阅者收到最后两条的重放 ---
[shared][+388ms][main] 四个事件已发完，迟到订阅者现在加入（replay=2）
[shared][+391ms][main] 迟到订阅者收到：e3（重放）
[shared][+391ms][main] 迟到订阅者收到：e4（重放）
--- (b) replay=0：订阅前发的事件，迟到者永远收不到 ---
[shared][+543ms][main] 事件「登录已过期」已发，此刻订阅者为零（replay=0）
[shared][+697ms][main] 迟到订阅者已就位等了 150ms：一条也没等到（一次性事件不重放）
---------- 演示 4 结束，本段耗时 697ms ----------
========== 演示 5: operatorsDemo（操作符订阅链：filter/map/debounce） ==========
[ops][+3ms][main] 订阅链就位，开始输入……
[ops][+21ms][main] filter 判断："k"（length=1）→ 放行=false
[ops][+125ms][main] filter 判断："ko"（length=2）→ 放行=true
[ops][+127ms][main] map 加工："ko" → "KO!"
[ops][+234ms][main] filter 判断："kot"（length=3）→ 放行=true
[ops][+234ms][main] map 加工："kot" → "KOT!"
[ops][+453ms][main] filter 判断："kotlinx"（length=7）→ 放行=true
[ops][+453ms][main] map 加工："kotlinx" → "KOTLINX!"
[ops][+454ms][main] 订阅者收到：KOTLINX!
---------- 演示 5 结束，本段耗时 966ms ----------
========== 演示 6: selectionDrillDemo（选型决策表逐行演练） ==========
[drill][+0ms][main] 场景① 第 1 次进详情页，collect
[drill][+0ms][main] 场景① 上游开跑：拉取列表
[drill][+1ms][main] 场景① 拿到：商品A
[drill][+1ms][main] 场景① 拿到：商品B
[drill][+1ms][main] 场景① 第 2 次进详情页，collect
[drill][+1ms][main] 场景① 上游开跑：拉取列表
[drill][+1ms][main] 场景① 拿到：商品A
[drill][+1ms][main] 场景① 拿到：商品B
[drill][+1ms][main] 选型：flow{} 冷流——收集才执行、各次独立，两次进入各拉一遍全新数据
[drill][+1ms][main] 场景② 订阅者收到：最新状态 v1
[drill][+108ms][main] 选型：StateFlow——新订阅者立即拿当前值，界面永远显示最新状态
[drill][+108ms][main] 场景③ 「跳转登录页」事件已发，此刻无人订阅（replay=0）
[drill][+264ms][main] 场景③ 迟到订阅者就位等待 150ms：一无所获
[drill][+264ms][main] 选型：SharedFlow(replay=0)——不重放、错过即错过，一次性导航事件专用
---------- 演示 6 结束，本段耗时 264ms ----------

BUILD SUCCESSFUL in 11s
```

EXIT=0

正文读数提示（供写作臂引用）：六段实测合计约 4.5s；演示 5 的 KOTLINX! 是在
+454ms 落地的——它由上游 flow 完成时触发 debounce 冲刷（不必等满 300ms 静默），
这正是「最后一个建议也要立刻上屏」的产品行为；打点中的 KOT! 在 +234ms 被加工
出来但从未到达订阅者，因为 kotlinx 在 +453ms 之前（实测间隙 219ms）顶掉了它。
