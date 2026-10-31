@file:OptIn(ExperimentalCoroutinesApi::class, ObsoleteCoroutinesApi::class, DelicateCoroutinesApi::class)

package lab.ch16

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 时间线工具：每条输出带 [标签][+偏移ms][线程名] 前缀。
 * 为什么统一走这一个出口：本书正文要引用实测时间线，「带线程名+带耗时」是判据演示
 * （P05：挂起不占线程 / 阻塞占死线程）的最低证据标准，散落各处的裸 println 做不到。
 */
class TimeLine(private val label: String) {
    private val t0 = System.currentTimeMillis()

    fun mark(msg: String) {
        val offset = System.currentTimeMillis() - t0
        println("[$label][+${offset}ms][${Thread.currentThread().name}] $msg")
    }

    fun elapsedMs(): Long = System.currentTimeMillis() - t0
}

fun main() {
    println("========== 演示 1: blockedDemo（阻塞拖死线程池 vs 挂起让路） ==========")
    blockedDemo()
    println()
    println("========== 演示 2: firstSuspendDemo（首个 suspend 函数 + 线程释放观察） ==========")
    firstSuspendDemo()
    println()
    println("========== 演示 3: manyCoroutinesDemo（单线程 10 万协程） ==========")
    manyCoroutinesDemo()
    println()
    println("========== 演示 4: p05DrillDemo（P05 判据演练：正例/反例） ==========")
    p05DrillDemo()
}

/**
 * 演示 1：固定 3 线程池被 3 个 sleep 占满后，瞬时任务排队约 5 秒；
 * 对照组：限并发 3 的 3 个 delay 协程占着「名额」但占不着线程，瞬时协程毫秒级开跑。
 */
fun blockedDemo() {
    // ---- 线程池版：Thread.sleep 占住线程本身 ----
    val tl = TimeLine("blocked")
    val pool = Executors.newFixedThreadPool(3)
    repeat(3) { i ->
        pool.execute {
            tl.mark("占坑任务 $i 开始 Thread.sleep(5000)")
            // 故意用阻塞原语：这就是要复盘的病灶，线程被独占而非让出
            Thread.sleep(5000)
            tl.mark("占坑任务 $i 结束")
        }
    }
    // 先歇 100ms 确保三个占坑任务都已开跑，否则瞬时任务可能抢在前面排队，测不出真实等待
    Thread.sleep(100)
    val submitAt = System.currentTimeMillis()
    pool.execute {
        val waited = System.currentTimeMillis() - submitAt
        tl.mark("瞬时任务终于开跑，从提交到执行实际等了 ${waited}ms")
    }
    pool.shutdown()
    pool.awaitTermination(30, TimeUnit.SECONDS)

    // ---- 协程版：delay 只占 limitedParallelism 的并发名额，不占线程 ----
    val tl2 = TimeLine("co")
    runBlocking {
        val limited = Dispatchers.Default.limitedParallelism(3)
        val jobs = mutableListOf<Job>()
        repeat(3) { i ->
            jobs += launch(limited) {
                tl2.mark("占坑协程 $i 开始 delay(5000)")
                delay(5000)
                tl2.mark("占坑协程 $i 结束")
            }
        }
        delay(100) // 与线程池版同构：等三个占坑协程都进入挂起态再提交瞬时协程
        val submitAt = System.currentTimeMillis()
        jobs += launch(limited) {
            val waited = System.currentTimeMillis() - submitAt
            tl2.mark("瞬时协程开跑，从提交到执行实际等了 ${waited}ms")
        }
        // join 全部是为了让时间线完整打印（不提前退出 runBlocking），不影响计时口径
        jobs.joinAll()
    }
}

/** 演示 2 的挂起函数：内部用 delay 做非阻塞等待，等待期间线程被释放回调度器 */
suspend fun slowFetch(): String {
    delay(1000)
    return "模拟网络数据"
}

/**
 * 演示 2：同一个 runBlocking（单线程事件循环）里，slowFetch 挂起等待的 1 秒内，
 * 第二个协程在同一线程上插进来跑；恢复后线程名与进入时相同——原地恢复。
 */
fun firstSuspendDemo() {
    val tl = TimeLine("first-suspend")
    runBlocking {
        tl.mark("进入 runBlocking，即将调用 slowFetch()")
        // delay(200) 保证第二个协程的输出落在 slowFetch 的挂起窗口 [0,1000ms) 内
        launch {
            delay(200)
            tl.mark("池子里的线程在干别的活（第二个协程插队执行）")
        }
        val result = slowFetch()
        tl.mark("slowFetch() 返回：$result（对比进入/返回两条日志的线程名）")
    }
}

/**
 * 演示 3：单线程调度器上 launch 10 万个各 delay(1000) 的协程。
 * 挂起协程只占内存不占线程，总耗时约 1 秒出头——数量级演示，不求精确。
 */
fun manyCoroutinesDemo() {
    val tl = TimeLine("many")
    val single = newSingleThreadContext("ch16-single")
    try {
        runBlocking {
            val n = 100_000
            val jobs = ArrayList<Job>(n)
            val launchStart = System.currentTimeMillis()
            repeat(n) { i ->
                jobs += launch(single) {
                    // 只让 0 号协程说话：10 万条打印本身会淹没时间线
                    if (i == 0) tl.mark("第 0 号协程开始 delay(1000)")
                    delay(1000)
                }
            }
            tl.mark(
                "$n 个协程 launch 完成，launch 阶段耗时 ${System.currentTimeMillis() - launchStart}ms" +
                    "（此刻该单线程上挂起的活跃协程数量级：10 万，线程数仍是 1）"
            )
            jobs.joinAll()
            tl.mark("$n 个协程全部 join 完毕，总耗时 ${tl.elapsedMs()}ms")
        }
    } finally {
        // newSingleThreadContext 的线程非守护，不 close 会挂着不走，JVM 无法退出
        single.close()
    }
}

/** P05 正例用的挂起等待：delay 期间线程让出，同线程的其他协程照常推进 */
suspend fun suspendingWait() {
    delay(1000)
}

/**
 * P05 反例用的伪挂起：签名是 suspend 但体内是阻塞原语。
 * 为什么保留这个反例函数：suspend 修饰符只约束「调用方可挂起」，不保证体内不阻塞——
 * 这是 P05 判据要抓的真坑（编译器不会报错，只有运行时时间线能暴露）。
 */
suspend fun blockingWait() {
    Thread.sleep(1000)
}

/**
 * 演示 4：同一单线程调度器上，watcher 协程每 200ms 打一个 tick；
 * 伴跑 suspendingWait 时 tick 间隔保持约 200ms（没被饿住）；
 * 换 blockingWait 时线程被独占 1 秒，tick 出现约 1000ms 的空洞（被饿住）。
 */
fun p05DrillDemo() {
    // ---- 正例：suspendingWait ----
    drillOneCase("p05-ok", TimeLine("p05-ok")) { suspendingWait() }
    println()
    // ---- 反例：blockingWait ----
    drillOneCase("p05-bad", TimeLine("p05-bad")) { blockingWait() }
}

/**
 * 单场景抽取：watcher 与被测等待在同一单线程调度器上并发，测 watcher tick 的最大间隔。
 * 最大间隔 ≈ 200ms 说明等待期间线程在正常轮转；出现 ≈1000ms 空洞即被饿住。
 */
private fun drillOneCase(caseName: String, tl: TimeLine, wait: suspend () -> Unit) {
    val single = newSingleThreadContext("$caseName-single")
    try {
        runBlocking {
            val t0 = System.currentTimeMillis()
            val ticks = mutableListOf<Long>()
            val watcher = launch(single) {
                while (isActive) {
                    ticks += System.currentTimeMillis() - t0
                    tl.mark("watcher tick @ ${ticks.last()}ms")
                    delay(200)
                }
            }
            launch(single) {
                tl.mark("被测等待开始（${caseName}）")
                wait()
                tl.mark("被测等待返回")
                watcher.cancel()
            }.join()
            // 最大相邻间隔就是「最长没人管 watcher」的时长，直接给判据读数。
            // 反例被饿住时 watcher 在空洞前往往只来得及打一个 tick（本次实测正是如此），
            // 只按相邻 tick 算会得出 0ms 的假读数——必须把「等待返回」时刻并入测点序列，
            // 空洞才能被量出来。
            val points = ticks + listOf(System.currentTimeMillis() - t0)
            val maxGap = points.zipWithNext { a, b -> b - a }.maxOrNull() ?: 0L
            tl.mark("watcher 最大 tick 间隔 = ${maxGap}ms（tick 名义周期 200ms；含等待返回边界测点）")
        }
    } finally {
        single.close()
    }
}
