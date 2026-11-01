// 正例：CPU 死循环，每 20 万轮主动查一次取消标记（纯 CPU 循环下间隔远小于 1ms）
launch(Dispatchers.Default) {
    val child = launch {
        var rounds = 0L
        try {
            while (true) {
                rounds++
                if (rounds % CANCEL_CHECK_EVERY == 0L) ensureActive()
            }
        } catch (e: CancellationException) {
            tl.mark("子协程：在第 $rounds 轮后的 ensureActive() 接到取消，收工")
            throw e   // 取消异常必须重抛：让父层知道这是取消收场，不是正常跑完
        }
    }
    delay(CANCEL_SPIN_MS)
    tl.mark("父协程：调用 child.cancel()")
    child.cancel()
    child.join()
    tl.mark("父协程感知：child.join() 返回，子协程已收工")
}
// 对照组：同样的循环，唯独没有 ensureActive。cancel 传到了，但没人理，
// 子协程只能空转到演示限定的 2 秒时限自停止损