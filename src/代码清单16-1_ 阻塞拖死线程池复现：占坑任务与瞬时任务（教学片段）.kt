// 线程池版：Thread.sleep 把线程钉在原地
val pool = Executors.newFixedThreadPool(3)
repeat(3) { i ->
    pool.execute {
        tl.mark("占坑任务 $i 开始 Thread.sleep(5000)")
        Thread.sleep(5000)   // 故意用阻塞原语：这就是要复盘的病灶
        tl.mark("占坑任务 $i 结束")
    }
}
Thread.sleep(100)            // 先等三个占坑任务都开跑，才量得出真实排队时长
val submitAt = System.currentTimeMillis()
pool.execute {
    tl.mark("瞬时任务终于开跑，从提交到执行实际等了 ${System.currentTimeMillis() - submitAt}ms")
}
pool.shutdown()

// 协程对照版：同样的三个慢任务，等待写成 delay
runBlocking {
    val limited = Dispatchers.Default.limitedParallelism(3)
    repeat(3) { i ->
        launch(limited) {
            tl2.mark("占坑协程 $i 开始 delay(5000)")
            delay(5000)      // 挂起式等待：占的是并发名额，不是线程
            tl2.mark("占坑协程 $i 结束")
        }
    }
    delay(100)
    val submitAt = System.currentTimeMillis()
    launch(limited) {
        tl2.mark("瞬时协程开跑，从提交到执行实际等了 ${System.currentTimeMillis() - submitAt}ms")
    }
}