// (a) 调用点 try-catch 接不住：launch 只负责「起」，异常在体内炸，沿 Job 层级掀到根
try {
    runBlocking {
        try {
            launch {
                delay(EXC_DELAY_MS)
                throw IllegalStateException("launch 体内的异常")
            }
        } catch (e: IllegalStateException) {
            tl.mark("(a) 调用点 try-catch：接到了（这行永远不会出现）")
        }
        delay(EXC_WAIT_MS)   // 异常在这窗口内发生，会把整个作用域掀翻
    }
} catch (e: IllegalStateException) {
    tl.mark("(a) 调用点 try-catch：没接到——异常沿 Job 层级一路掀到根（runBlocking）才现身：${e.message}")
}

// (b) 根作用域装 CoroutineExceptionHandler：逃到根的异常由 handler 统一接住
val handler = CoroutineExceptionHandler { _, e ->
    tl.mark("(b) 根作用域 handler：接到了——${e::class.simpleName}: ${e.message}")
}
val rootScope = CoroutineScope(Job() + Dispatchers.Default + handler)

// (c) async 根在独立作用域上：子协程失败会同时沿层级取消父级，
// await 处的 catch 只接住「取值」这条线，隔离出来对照才干净
val deferred = asyncScope.async<Int> {
    delay(EXC_DELAY_MS)
    throw IllegalStateException("async 体内的异常")
}
try {
    deferred.await()
} catch (e: IllegalStateException) {
    tl.mark("(c) await 处 try-catch：接到了——${e.message}")
}