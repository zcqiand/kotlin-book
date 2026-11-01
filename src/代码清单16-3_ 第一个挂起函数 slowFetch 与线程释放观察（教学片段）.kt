// 标了 suspend 的函数：体内用 delay 等待，等的时候线程让出去干别的活
suspend fun slowFetch(): String {
    delay(1000)
    return "模拟网络数据"
}

fun firstSuspendDemo() {
    val tl = TimeLine("first-suspend")
    runBlocking {                  // 教学桥：把 main 线程变成协程入口
        tl.mark("进入 runBlocking，即将调用 slowFetch()")
        launch {                   // 见证协程：证明等待期间线程没闲着
            delay(200)
            tl.mark("池子里的线程在干别的活（第二个协程插队执行）")
        }
        val result = slowFetch()
        tl.mark("slowFetch() 返回：$result（对比进入/返回两条日志的线程名）")
    }
}