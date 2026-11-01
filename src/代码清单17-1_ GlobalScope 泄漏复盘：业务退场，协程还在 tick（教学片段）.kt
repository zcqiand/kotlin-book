// 反模式现场：GlobalScope 的生命周期与进程同寿，起出去就没人管得着
@OptIn(DelicateCoroutinesApi::class)
private fun leakDemo() {
    val leaked: Job = GlobalScope.launch {
        repeat(LEAK_TICKS) { i ->
            delay(LEAK_TICK_INTERVAL_MS)   // 每半秒报一次到；真实事故里连这道闸都没有
            tl.mark("泄漏协程 tick ${i + 1}：业务作用域早已退场，我还活着")
        }
    }
    val business = CoroutineScope(Dispatchers.Default)
    val work = business.launch {
        delay(LEAK_BUSINESS_MS)
        tl.mark("业务作用域：活干完了，退场")
    }
    runBlocking {
        work.join()
        tl.mark("业务作用域 Job 完结。GlobalScope 里的协程无人管辖，tick 还会继续出现")
        leaked.join()   // 演示限定的闸：真实事故里没人 join 它，它只会 tick 到把内存吃穿
    }
}