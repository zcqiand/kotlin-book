package demo

// 入口分发：按第一个命令行参数路由到对应演示，
// 一章多个清单共用一个 Gradle 工程、一个 run 任务，与 ch11/ch12 工程同构
fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "digest" -> runDigest()
        "variance" -> runVariance()
        "reified" -> runReified()
        "lazy" -> runLazy()
        // 未带参数时显式失败退出：宁可报错，也不静默跑错演示
        else -> error("用法：gradle run --args=\"digest|variance|reified|lazy\"")
    }
}
