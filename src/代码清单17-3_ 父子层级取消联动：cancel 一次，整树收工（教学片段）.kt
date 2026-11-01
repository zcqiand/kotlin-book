val scope = CoroutineScope(Job() + Dispatchers.Default)
val children = (1..3).map { id ->
    scope.launch {
        repeat(10) { round ->
            delay(TREE_TICK_MS)        // 每 150ms 报一声到
            tl.mark("子协程 $id tick ${round + 1}")
        }
    }
}
delay(TREE_OBSERVE_MS)   // 400ms 观察窗：够每个子协程出两声 tick
tl.mark("cancel 前：作用域名下 children 计数 = ${scope.coroutineContext[Job]?.children?.count()}")
scope.cancel()
tl.mark("scope.cancel() 已调用，joinAll 等三个子协程收工（父等子）")
children.joinAll()
tl.mark("cancel 后：children 计数 = ${scope.coroutineContext[Job]?.children?.count()}")