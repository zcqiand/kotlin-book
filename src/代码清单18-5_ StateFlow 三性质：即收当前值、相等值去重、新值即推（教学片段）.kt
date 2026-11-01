val state = MutableStateFlow("首页 v0")
val collector = launch {
    state.collect { tl.mark("收集者收到：$it") }
}
delay(STATE_STEP_MS)   // (a) 订阅即收：collector 一启动就收到当前值 v0
tl.mark("赋与当前相等的值：v0 —— 观察下面 150ms 内有没有新行")
state.value = "首页 v0"   // (b) 相等值去重：收集者不打印
delay(STATE_STEP_MS)
tl.mark("赋新值：v1")
state.value = "首页 v1"   // (c) 新值即推：立即打印
delay(STATE_STEP_MS)
collector.cancel()