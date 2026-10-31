package demo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// ---------- 演示时长常量：全部集中在此，正文读数可按这里复算 ----------
// 演示 1：泄漏协程 tick 节奏。限定 4 声是为工程可复跑、进程能退出；真实事故没有这道闸。
private const val LEAK_TICKS = 4
private const val LEAK_TICK_INTERVAL_MS = 500L
private const val LEAK_BUSINESS_MS = 200L
// 演示 2：子协程 tick 节奏与 cancel 前观察窗。400ms 窗口内每个子恰出 2 声 tick，读数可预期。
private const val TREE_TICK_MS = 150L
private const val TREE_OBSERVE_MS = 400L
// 演示 3：单次取值耗时。三个任务各 1s，并发版总耗时约 1s、顺序版 3s，对比读数由此而来。
private const val P06_FETCH_MS = 1000L
private val P06_SUBJECTS = listOf("语文", "数学", "英语")
private val P06_SCORES = mapOf("语文" to 92, "数学" to 88, "英语" to 95)
// 演示 4：cancel 前空转窗口、协作检查密度、对照组强停时限。
// CHECK_EVERY 取 20 万轮：纯 CPU 循环下两次检查间隔远小于 1ms，取消几乎即时生效。
private const val CANCEL_SPIN_MS = 300L
private const val CANCEL_CHECK_EVERY = 200_000L
private const val CONTROL_STOP_MS = 2000L
// 演示 5：协程体内抛异常前的挂起时长，以及 (a) 里等异常发生的外层等待窗。
private const val EXC_DELAY_MS = 100L
private const val EXC_WAIT_MS = 500L

fun main() {
    runDemo(1, "leakDemo（GlobalScope 泄漏复盘：作用域退了协程还活着）") { leakDemo() }
    runDemo(2, "scopeTreeDemo（父子层级：作用域一退协程全收）") { scopeTreeDemo() }
    runDemo(3, "p06DrillDemo（P06 选型演练：async 要值 / launch 干活）") { p06DrillDemo() }
    runDemo(4, "cancelChainDemo（取消传播与 ensureActive 协作式检查）") { cancelChainDemo() }
    runDemo(5, "exceptionBoundaryDemo（异常边界三对照）") { exceptionBoundaryDemo() }
}

// 演示头与耗时尾行统一在这里打：头行格式沿 ch16 族，尾行供正文引用实测耗时。
private fun runDemo(no: Int, title: String, body: () -> Unit) {
    println("========== 演示 $no: $title ==========")
    val startAt = System.currentTimeMillis()
    body()
    println("---------- 演示 $no 结束，本段耗时 ${System.currentTimeMillis() - startAt}ms ----------")
}

// 演示 1：反模式现场。GlobalScope 的生命周期与进程同寿，起出去就没人管得着；
// DelicateCoroutinesApi 的 opt-in 只是让调用者「确认过风险」，泄漏发生时它一声不吭。
@OptIn(DelicateCoroutinesApi::class)
private fun leakDemo() {
    val tl = TimeLine("leak")
    val leaked: Job = GlobalScope.launch {
        repeat(LEAK_TICKS) { i ->
            delay(LEAK_TICK_INTERVAL_MS)
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
        // 演示限定：等 4 声 tick 打完再进下一场，避免演示间输出串台；
        // 真实事故里没有任何人持有 leaked 去 join 它，它只会一直 tick 到把内存吃穿。
        leaked.join()
    }
}

// 演示 2：一个作用域，三个子协程。cancel 作用域一嗓子，三个子全部收工，
// children 计数从 3 归 0，joinAll 见证「父等子」的收尾面。
private fun scopeTreeDemo() {
    val tl = TimeLine("tree")
    runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val children = (1..3).map { id ->
            scope.launch {
                repeat(10) { round ->
                    delay(TREE_TICK_MS)
                    tl.mark("子协程 $id tick ${round + 1}")
                }
            }
        }
        delay(TREE_OBSERVE_MS)
        tl.mark("cancel 前：作用域名下 children 计数 = ${scope.coroutineContext[Job]?.children?.count()}")
        scope.cancel()
        tl.mark("scope.cancel() 已调用，joinAll 等三个子协程收工（父等子）")
        children.joinAll()
        tl.mark("cancel 后：children 计数 = ${scope.coroutineContext[Job]?.children?.count()}")
        tl.mark("结论：作用域一退，3 个子协程全部收工，一个不漏")
    }
}

// 三个各约 1 秒的取值任务：返回值定死，正文引用读数时不会漂。
private suspend fun fetchScore(subject: String): Int {
    delay(P06_FETCH_MS)
    return P06_SCORES.getValue(subject)
}

// 演示 3：P06 选型演练。同一场景双实现——
// (a) async 并发取值：总耗时约最慢的一个，不是三个相加；
//     演示内附顺序 await 对照组，直观给出「选错了就退化成串行 3s」。
// (b) launch 副作用：无返回值可接，join 只等收尾。
private fun p06DrillDemo() {
    runBlocking {
        val tl = TimeLine("p06")
        var startAt = System.currentTimeMillis()
        val deferred = P06_SUBJECTS.map { subject -> async { fetchScore(subject) } }
        val total = deferred.sumOf { it.await() }
        tl.mark("(a) async 并发：三科总分 $total，总耗时 ${System.currentTimeMillis() - startAt}ms（约等于一个任务的 1s，不是三个相加）")

        startAt = System.currentTimeMillis()
        var serialTotal = 0
        for (subject in P06_SUBJECTS) {
            serialTotal += async { fetchScore(subject) }.await()
        }
        tl.mark("(a-对照) 顺序 await：总分 $serialTotal，总耗时 ${System.currentTimeMillis() - startAt}ms（若顺序 await 则 3s：选型错了，并发白给）")

        startAt = System.currentTimeMillis()
        val jobs = P06_SUBJECTS.map { subject ->
            launch { tl.mark("(b) launch 副作用任务「$subject」干完，无返回值可接") }
        }
        jobs.joinAll()
        tl.mark("(b) launch 版：joinAll 收工，总耗时 ${System.currentTimeMillis() - startAt}ms（毫秒级：没有等待可重叠，join 只是等收尾）")
    }
}

// 演示 4：取消是协作式的。cancel 只递个话，收不收工看循环里有没有 ensureActive 这样的检查点；
// 对照组裸循环里 cancel 传到了但没人理，只能靠演示限定的 2 秒时限自停止损。
private fun cancelChainDemo() {
    val tl = TimeLine("cancel")
    runBlocking {
        // 为什么显式上 Dispatchers.Default：runBlocking 里的 launch 不带调度器时
        // 继承它的单线程事件循环，子协程的 CPU 死循环会把主线程堵死，父协程的
        // delay 永远无法恢复（工程首跑实测自锁，见 receipts.md 备注）；上到 Default
        // 后子协程空转占的是 worker 线程，delay 与 cancel 走调度器，两不相堵。
        val parent = launch(Dispatchers.Default) {
            val child = launch {
                var rounds = 0L
                try {
                    while (true) {
                        rounds++
                        if (rounds % CANCEL_CHECK_EVERY == 0L) ensureActive()
                    }
                } catch (e: CancellationException) {
                    tl.mark("子协程：在第 $rounds 轮后的 ensureActive() 接到取消，收工")
                    throw e // 取消异常必须重抛：让父层知道这是取消收场，不是正常跑完
                }
            }
            delay(CANCEL_SPIN_MS)
            tl.mark("父协程：调用 child.cancel()")
            child.cancel()
            child.join()
            tl.mark("父协程感知：child.join() 返回，子协程已收工")
        }
        parent.join()
    }
    runBlocking {
        // 为什么显式上 Dispatchers.Default：runBlocking 里的 launch 不带调度器时
        // 继承它的单线程事件循环，子协程的 CPU 死循环会把主线程堵死，父协程的
        // delay 永远无法恢复（工程首跑实测自锁，见 receipts.md 备注）；上到 Default
        // 后子协程空转占的是 worker 线程，delay 与 cancel 走调度器，两不相堵。
        val parent = launch(Dispatchers.Default) {
            val child = launch {
                var rounds = 0L
                val deadline = System.currentTimeMillis() + CONTROL_STOP_MS
                while (System.currentTimeMillis() < deadline) { rounds++ }
                tl.mark("对照组子协程：裸循环转了 $rounds 轮，到时限自停——cancel 传到了但没人理")
            }
            delay(CANCEL_SPIN_MS)
            tl.mark("对照组父协程：调用 child.cancel()")
            child.cancel()
            child.join()
            tl.mark("对照组父协程感知：join() 返回——但子协程不是被取消停的，是自己跑满时限")
        }
        parent.join()
    }
}

// 演示 5：异常边界三对照。
// (a) try-catch 包住 launch 调用点：launch 只负责「起」，异常在体内炸，catch 接不住，
//     只能沿 Job 层级一路掀到根；外层 try 在 runBlocking 处接到，恰证明调用点的 catch 全程没进。
// (b) 根作用域装 CoroutineExceptionHandler：逃到根的异常由 handler 统一接住。
// (c) async：异常憋在 Deferred 里，await 取值时在取处重新抛出，包住 await 的 catch 接得住。
private fun exceptionBoundaryDemo() {
    val tl = TimeLine("exc")
    try {
        runBlocking {
            try {
                launch {
                    delay(EXC_DELAY_MS)
                    tl.mark("(a) 协程体内：即将抛出 IllegalStateException")
                    throw IllegalStateException("launch 体内的异常")
                }
            } catch (e: IllegalStateException) {
                tl.mark("(a) 调用点 try-catch：接到了（这行永远不会出现）")
            }
            delay(EXC_WAIT_MS) // 异常在这窗口内发生，会把整个作用域掀翻
            tl.mark("(a) 这行打不出来：作用域已被子协程的异常掀翻")
        }
    } catch (e: IllegalStateException) {
        tl.mark("(a) 调用点 try-catch：没接到——异常沿 Job 层级一路掀到根（runBlocking）才现身：${e.message}")
    }

    val handler = CoroutineExceptionHandler { _, e ->
        tl.mark("(b) 根作用域 handler：接到了——${e::class.simpleName}: ${e.message}")
    }
    val rootScope = CoroutineScope(Job() + Dispatchers.Default + handler)
    runBlocking {
        val job = rootScope.launch {
            delay(EXC_DELAY_MS)
            throw IllegalStateException("同款异常")
        }
        job.join() // 异常由 handler 接住后 Job 完结，join 才能正常返回
        tl.mark("(b) 结论：handler 接住了，作用域没有崩，进程继续")
    }

    // (c) 让 async 根在独立作用域上：async 的子协程失败会同时沿 Job 层级取消父级，
    // await 处的 catch 只是「取到值」，拦不住父作用域被掀翻——首跑实测把 async 直接
    // 放进 runBlocking，末尾整个 runBlocking 仍被炸出（见 receipts.md 备注）。隔离出
    // 「Deferred 交付」这一条线，对照才干净。
    val asyncScope = CoroutineScope(Job() + Dispatchers.Default)
    runBlocking {
        val deferred = asyncScope.async<Int> {
            delay(EXC_DELAY_MS)
            throw IllegalStateException("async 体内的异常")
        }
        try {
            deferred.await()
        } catch (e: IllegalStateException) {
            tl.mark("(c) await 处 try-catch：接到了——${e.message}（async 的异常憋在 Deferred 里，谁来取就在取处抛）")
        }
    }
}
