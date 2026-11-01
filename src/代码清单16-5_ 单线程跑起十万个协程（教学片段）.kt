fun manyCoroutinesDemo() {
    val tl = TimeLine("many")
    val single = newSingleThreadContext("ch16-single")   // 一条专属线程
    try {
        runBlocking {
            val n = 100_000
            val jobs = ArrayList<Job>(n)
            val launchStart = System.currentTimeMillis()
            repeat(n) { i ->
                jobs += launch(single) {
                    // 只让 0 号协程说话：十万条打印本身会淹没时间线
                    if (i == 0) tl.mark("第 0 号协程开始 delay(1000)")
                    delay(1000)
                }
            }
            tl.mark("$n 个协程 launch 完成，launch 阶段耗时 " +
                "${System.currentTimeMillis() - launchStart}ms")
            jobs.joinAll()
            tl.mark("$n 个协程全部 join 完毕，总耗时 ${tl.elapsedMs()}ms")
        }
    } finally {
        single.close()   // 专属线程非守护，不 close 会挂住 JVM 不退场
    }
}