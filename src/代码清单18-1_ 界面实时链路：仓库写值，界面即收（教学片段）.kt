val repo = MutableStateFlow("库存快照 v0")   // StateFlow 必有初始值
val ui = launch {
    repo.collect { tl.mark("界面收到：$it") }   // 界面订阅：一有值就执行
}
delay(LIVE_STEP_MS)
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