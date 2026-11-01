// (a) replay=2：发 e1..e4 之后，迟到订阅者才加入
val events = MutableSharedFlow<String>(replay = 2)
listOf("e1", "e2", "e3", "e4").forEach {
    events.emit(it)   // 发出时还没有订阅者，只有 replay 缓冲留住尾巴
    delay(EVENT_STEP_MS)
}
tl.mark("四个事件已发完，迟到订阅者现在加入（replay=2）")
val late = launch { events.collect { tl.mark("迟到订阅者收到：$it（重放）") } }
delay(STATE_STEP_MS)
late.cancel()

// (b) replay=0：一次性事件，订阅前发出就等于没人收到
val oneShot = MutableSharedFlow<String>()
oneShot.emit("登录已过期")
tl.mark("事件「登录已过期」已发，此刻订阅者为零（replay=0）")
val waiter = launch { oneShot.collect { tl.mark("迟到订阅者收到：$it") } }
delay(STATE_STEP_MS)
tl.mark("迟到订阅者已就位等了 150ms：一条也没等到（一次性事件不重放）")
waiter.cancel()