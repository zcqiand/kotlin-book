package hello

import hello.core.GreetingService
// :core 改 api 后, 这行 import 不再报错——slf4j-api 随 :core 对下游编译可见
import org.slf4j.LoggerFactory

fun main() {
    val service = GreetingService()
    val appLogger = LoggerFactory.getLogger("hello.MainKt")

    appLogger.info("多模块演示开始")
    println("========== multi-module-demo: :app -> :core ==========")
    println(service.greetBlocking("ch20"))
    // 横幅的版本值取运行时读数而非源码字面量: 源码里写死的版本串正是
    // 本章要消灭的「漂移点」, 演示自身也不能违例
    println(service.buildBanner("Kotlin", KotlinVersion.CURRENT.toString()))
    println("JVM 版本: ${System.getProperty("java.version")}")
}
