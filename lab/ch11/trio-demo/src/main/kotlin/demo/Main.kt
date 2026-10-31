package demo

// 入口分发：按第一个命令行参数路由到对应演示，
// 让一章的多个清单共用一个 Gradle 工程、一个 run 任务，与 ch10 例集同构
fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "flagchaos" -> runFlagChaos()
        "dataclass" -> runDataClassDemo()
        "machine" -> runOrderStateMachine()
        "objectd" -> runObjectDemo()
        // 未带参数时直接报错退出：宁可显式失败，也不静默跑错演示
        else -> error("用法：gradle run --args=\"flagchaos|dataclass|machine|objectd\"")
    }
}
