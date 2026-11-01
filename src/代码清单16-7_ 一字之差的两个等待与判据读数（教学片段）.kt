// 正例：delay 的挂起式等待，等的时候线程是自由的
suspend fun suspendingWait() {
    delay(1000)
}

// 反例：签名挂起，体内阻塞。编译器不报错，只有运行时间线能抓到它
suspend fun blockingWait() {
    Thread.sleep(1000)
}

// 判据读数：watcher 每 200ms 一个 tick，把「等待返回」时刻并入测点序列，
// 反例被饿住时只来得及打一个 tick，不并入会得出 0ms 的假读数
val points = ticks + listOf(System.currentTimeMillis() - t0)
val maxGap = points.zipWithNext { a, b -> b - a }.maxOrNull() ?: 0L