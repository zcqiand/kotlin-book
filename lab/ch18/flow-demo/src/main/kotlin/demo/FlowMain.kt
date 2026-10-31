package demo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// ---------- 演示时长常量：全部集中在此，正文读数可按这里复算 ----------
// 演示 1：仓库每次写入后留 150ms 让订阅回调先落地，保证「写入一行、收到一行」逐字可对齐。
private const val LIVE_STEP_MS = 150L
// 演示 2：冷流两次收集各发 3 个值、间隔 150ms；热流生产者间隔 200ms，B 在第 2 个值发出后 150ms 加入。
private const val COLD_EMIT_MS = 150L
private const val HOT_EMIT_MS = 200L
// 演示 3：每步之间留 150ms，给收集回调留出落地窗口；相等值那步专门空等 150ms 证明真没打印。
private const val STATE_STEP_MS = 150L
// 演示 4：事件间隔 80ms；迟到订阅者各留 150/200ms 观察窗，证明「该收的重放、不该收的等不到」。
private const val EVENT_STEP_MS = 80L
// 演示 5：k/ko/kot 三连发间隔 100ms；kot 与 kotlinx 之间 100ms（< debounce 窗口 300ms；
// 见 receipts.md 修复记录：500ms 会被 debounce 冲刷出 KOT!，200ms 在 Windows 定时器过冲下
// 实测走了 312ms 仍越窗，100ms 实测约 110-160ms 才稳）；尾部 500ms 给 KOTLINX! 落地窗。
private const val OPS_TYPING_MS = 100L
private const val OPS_GAP_MS = 100L
private const val OPS_TAIL_MS = 500L
private const val OPS_DEBOUNCE_MS = 300L
// 演示 6：三个小场景各留 100-150ms 观察窗，结论行在现象之后打。
private const val DRILL_STATE_MS = 100L
private const val DRILL_SHARED_MS = 150L

fun main() = runBlocking {
    runDemo(1, "liveChainDemo（界面实时链路：数据一变，订阅方立即收到）") { liveChainDemo() }
    runDemo(2, "coldHotDemo（P07 冷热对照：上游跑两遍 vs 订阅者共享一遍）") { coldHotDemo() }
    runDemo(3, "stateFlowDemo（P08 状态承载三性质一次演全）") { stateFlowDemo() }
    runDemo(4, "sharedFlowDemo（事件广播：replay 重放与一次性事件）") { sharedFlowDemo() }
    runDemo(5, "operatorsDemo（操作符订阅链：filter/map/debounce）") { operatorsDemo() }
    runDemo(6, "selectionDrillDemo（选型决策表逐行演练）") { selectionDrillDemo() }
}

// 演示头与耗时尾行统一在这里打：头行格式沿 ch16/ch17 族，尾行供正文引用实测耗时。
private suspend fun CoroutineScope.runDemo(no: Int, title: String, body: suspend CoroutineScope.() -> Unit) {
    println("========== 演示 $no: $title ==========")
    val startAt = System.currentTimeMillis()
    body()
    println("---------- 演示 $no 结束，本段耗时 ${System.currentTimeMillis() - startAt}ms ----------")
}

// 演示 1：MutableStateFlow 当数据仓库，「界面」协程订阅 collect；主流程连续写三次值，
// 每次写入订阅者立即打印。要体现的就一句话：数据一变、订阅方立即收到，不需要谁去「查」。
private suspend fun CoroutineScope.liveChainDemo() {
    val tl = TimeLine("live")
    val repo = MutableStateFlow("库存快照 v0")
    val ui = launch {
        repo.collect { tl.mark("界面收到：$it") }
    }
    delay(LIVE_STEP_MS) // 留出订阅就位窗口（此值打印的是初始值 v0，正文引用时说明这是 StateFlow 必有初始值）
    tl.mark("仓库写入 v1")
    repo.value = "库存快照 v1"
    delay(LIVE_STEP_MS)
    tl.mark("仓库写入 v2")
    repo.value = "库存快照 v2"
    delay(LIVE_STEP_MS)
    tl.mark("仓库写入 v3")
    repo.value = "库存快照 v3"
    delay(LIVE_STEP_MS)
    ui.cancel()
}

// 演示 2：(a) 冷流 flow{}：上游生产代码在每次 collect 时各跑一遍，两个收集者各拿全量序列；
// (b) 热流 MutableStateFlow：上游只「生产」（赋值）一遍，订阅者 A 全程在收，
// B 在第 2 个值发出后才订阅——错过 1，只从当前值起收。冷热分野由此一幕看清。
private suspend fun CoroutineScope.coldHotDemo() {
    println("--- (a) 冷流 flow{}：两个收集者先后 collect，上游跑两遍 ---")
    val coldTl = TimeLine("cold")
    val cold = flow {
        coldTl.mark("冷流上游：开始生产")
        emit(1)
        delay(COLD_EMIT_MS)
        emit(2)
        delay(COLD_EMIT_MS)
        emit(3)
    }
    coldTl.mark("收集者一 collect")
    cold.collect { coldTl.mark("收集者一拿到：$it") }
    coldTl.mark("收集者二 collect")
    cold.collect { coldTl.mark("收集者二拿到：$it") }

    println("--- (b) 热流 MutableStateFlow：上游只生产一遍，订阅者共享 ---")
    val hotTl = TimeLine("hot")
    val hot = MutableStateFlow(0)
    val a = launch {
        hot.collect { hotTl.mark("订阅者 A 收到：$it") }
    }
    delay(HOT_EMIT_MS) // A 先就位（A 已收到初始值 0）
    hot.value = 1
    delay(HOT_EMIT_MS)
    hot.value = 2
    delay(HOT_EMIT_MS) // 此刻第 2 个值已发出，B 才来订阅
    val b = launch {
        hot.collect { hotTl.mark("订阅者 B 收到：$it") }
    }
    delay(HOT_EMIT_MS)
    hot.value = 3
    delay(HOT_EMIT_MS)
    a.cancel()
    b.cancel()
}

// 演示 3：StateFlow 状态承载三性质一次演全：
// (a) 新订阅者订阅后立即收到当前值（不用等下一次变更）；
// (b) 对 value 赋一个与当前相等的值——收集者不打印（相等值去重，Conflated 平板把重复状态挡住）；
// (c) 赋新值——收集者立即打印。
private suspend fun CoroutineScope.stateFlowDemo() {
    val tl = TimeLine("state")
    val state = MutableStateFlow("首页 v0")
    val collector = launch {
        state.collect { tl.mark("收集者收到：$it") }
    }
    delay(STATE_STEP_MS) // (a)：collector 一启动就收到当前值 v0
    tl.mark("赋与当前相等的值：v0 —— 观察下面 150ms 内有没有新行")
    state.value = "首页 v0" // (b)：相等值去重，收集者不打印
    delay(STATE_STEP_MS)
    tl.mark("赋新值：v1")
    state.value = "首页 v1" // (c)：立即打印
    delay(STATE_STEP_MS)
    collector.cancel()
}

// 演示 4：(a) MutableSharedFlow(replay=2)：发 e1..e4 后迟到订阅者加入，只收到 e3/e4 的重放；
// (b) 对照 MutableSharedFlow()（replay=0）：订阅前发出的事件，迟到订阅者永远等不到——
// 一次性事件（如「跳转登录页」）就靠这个「不重放、错过即错过」。
private suspend fun CoroutineScope.sharedFlowDemo() {
    val tl = TimeLine("shared")
    println("--- (a) replay=2：迟到订阅者收到最后两条的重放 ---")
    val events = MutableSharedFlow<String>(replay = 2)
    listOf("e1", "e2", "e3", "e4").forEach {
        events.emit(it) // 此刻还没有订阅者，SharedFlow 不留全员回放，只有 replay 缓冲留住尾巴
        delay(EVENT_STEP_MS)
    }
    tl.mark("四个事件已发完，迟到订阅者现在加入（replay=2）")
    val late = launch {
        events.collect { tl.mark("迟到订阅者收到：$it（重放）") }
    }
    delay(STATE_STEP_MS)
    late.cancel()

    println("--- (b) replay=0：订阅前发的事件，迟到者永远收不到 ---")
    val oneShot = MutableSharedFlow<String>()
    oneShot.emit("登录已过期")
    tl.mark("事件「登录已过期」已发，此刻订阅者为零（replay=0）")
    val waiter = launch {
        oneShot.collect { tl.mark("迟到订阅者收到：$it") }
    }
    delay(STATE_STEP_MS)
    tl.mark("迟到订阅者已就位等了 150ms：一条也没等到（一次性事件不重放）")
    waiter.cancel()
}

// 演示 5：搜索输入建议的订阅链。上游模拟打字：k → ko → kot 三连发（间隔 100ms），
// 短停后发 kotlinx。链路 filter(length>=2) → map(大写+!) → debounce(300)：
// debounce 只放行「越过 300ms 静默」的值——三连发被它合并顶掉，只有 kotlinx 落到订阅者。
// filter/map 内的打点行展示中间产物，订阅者收到的单独成行。
@OptIn(kotlinx.coroutines.FlowPreview::class)
private suspend fun CoroutineScope.operatorsDemo() {
    val tl = TimeLine("ops")
    val input = flow {
        listOf("k", "ko", "kot").forEach {
            emit(it)
            delay(OPS_TYPING_MS)
        }
        delay(OPS_GAP_MS) // 100ms 实测 << 300ms：kotlinx 顶掉 KOT!（参数演化见 receipts.md 修复记录）
        emit("kotlinx")
    }
    val chained = input
        .filter {
            val keep = it.length >= 2
            tl.mark("filter 判断：\"$it\"（length=${it.length}）→ 放行=$keep")
            keep
        }
        .map {
            val r = it.uppercase() + "!"
            tl.mark("map 加工：\"$it\" → \"$r\"")
            r
        }
        .debounce(OPS_DEBOUNCE_MS)
    tl.mark("订阅链就位，开始输入……")
    chained.collect { tl.mark("订阅者收到：$it") }
    delay(OPS_TAIL_MS) // 留足静默窗，让 debounce 把最后的值冲刷落地
}

// 演示 6：选型决策表逐行演练——三个真实场景各跑一小段，现象之后紧跟一行选型结论。
private suspend fun CoroutineScope.selectionDrillDemo() {
    val tl = TimeLine("drill")
    // 场景①：每次进详情页重新拉一遍列表 → 冷流：收集才执行、各次独立
    val detailList = flow {
        tl.mark("场景① 上游开跑：拉取列表")
        emit("商品A")
        emit("商品B")
    }
    repeat(2) { n ->
        tl.mark("场景① 第 ${n + 1} 次进详情页，collect")
        detailList.collect { tl.mark("场景① 拿到：$it") }
    }
    tl.mark("选型：flow{} 冷流——收集才执行、各次独立，两次进入各拉一遍全新数据")
    // 场景②：界面要始终显示最新状态 → StateFlow：新订阅者立即拿当前值
    val page = MutableStateFlow("最新状态 v1")
    val viewer = launch {
        page.collect { tl.mark("场景② 订阅者收到：$it") }
    }
    delay(DRILL_STATE_MS)
    tl.mark("选型：StateFlow——新订阅者立即拿当前值，界面永远显示最新状态")
    viewer.cancel()
    // 场景③：登出后发一次性「跳转登录页」事件 → SharedFlow(replay=0)：不重放、错过即错过
    val bus = MutableSharedFlow<String>()
    bus.emit("跳转登录页")
    tl.mark("场景③ 「跳转登录页」事件已发，此刻无人订阅（replay=0）")
    val latecomer = launch {
        bus.collect { tl.mark("场景③ 迟到订阅者收到：$it") }
    }
    delay(DRILL_SHARED_MS)
    tl.mark("场景③ 迟到订阅者就位等待 150ms：一无所获")
    tl.mark("选型：SharedFlow(replay=0)——不重放、错过即错过，一次性导航事件专用")
    latecomer.cancel()
}
