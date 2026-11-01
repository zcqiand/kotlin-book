// Application.kt 中的 main（EngineMain 入口）
fun main(args: Array<String>) = EngineMain.main(args)

// build.gradle.kts 中的 application 块（run 任务指向 EngineMain）
application { mainClass = "io.ktor.server.netty.EngineMain" }