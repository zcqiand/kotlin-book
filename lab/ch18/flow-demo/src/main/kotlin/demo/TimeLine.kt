package demo

// ch16/ch17/ch18 共族的时间线打点器：每行输出统一为「[标签][+偏移毫秒][线程名] 消息」。
// 为什么不用日志框架：教学输出要求逐字稳定可引用，日志框架的时间戳格式不可控；
// 用相对偏移量既可读，又能直接对出先后因果，正文清单可以整段引用。
class TimeLine(private val label: String) {
    private val startedAt = System.currentTimeMillis()

    fun mark(message: String) {
        val offsetMs = System.currentTimeMillis() - startedAt
        println("[$label][+${offsetMs}ms][${Thread.currentThread().name}] $message")
    }
}
