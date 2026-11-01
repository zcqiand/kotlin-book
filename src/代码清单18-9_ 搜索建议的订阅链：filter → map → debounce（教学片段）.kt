@OptIn(kotlinx.coroutines.FlowPreview::class)
private suspend fun CoroutineScope.operatorsDemo() {
    val tl = TimeLine("ops")
    // 上游模拟打字：k → ko → kot 三连发各隔 100ms，短暂停顿后发 kotlinx
    val input = flow {
        listOf("k", "ko", "kot").forEach {
            emit(it)
            delay(OPS_TYPING_MS)
        }
        delay(OPS_GAP_MS)   // 100ms，远小于 debounce 的 300ms 静默窗
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
    delay(OPS_TAIL_MS)   // 兜底余量：debounce 在上游完结时即冲刷队尾值，此延时非冲刷条件
}