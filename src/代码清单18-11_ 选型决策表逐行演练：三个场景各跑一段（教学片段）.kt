// 场景①（表 18-1 第 1 行）：每次进详情页重新拉列表 → 冷流
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

// 场景②（表 18-1 第 2 行）：界面要始终显示最新状态 → StateFlow
val page = MutableStateFlow("最新状态 v1")
val viewer = launch { page.collect { tl.mark("场景② 订阅者收到：$it") } }
delay(DRILL_STATE_MS)
tl.mark("选型：StateFlow——新订阅者立即拿当前值，界面永远显示最新状态")
viewer.cancel()

// 场景③（表 18-1 第 3 行）：登出后发一次性导航事件 → SharedFlow(replay=0)
val bus = MutableSharedFlow<String>()
bus.emit("跳转登录页")
tl.mark("场景③ 「跳转登录页」事件已发，此刻无人订阅（replay=0）")
val latecomer = launch { bus.collect { tl.mark("场景③ 迟到订阅者收到：$it") } }
delay(DRILL_SHARED_MS)
tl.mark("场景③ 迟到订阅者就位等待 150ms：一无所获")
tl.mark("选型：SharedFlow(replay=0)——不重放、错过即错过，一次性导航事件专用")
latecomer.cancel()