package hello

import java.time.LocalDate
import kotlin.KotlinVersion

fun main() {
    // Kotlin 1.3 起 main 可以不写 args 参数，这行省略是合法的；
    // 本章用不到命令行输入，少写一个参数就少解释一个概念
    //
    // KotlinVersion.CURRENT 读的是随 jar 一起打包的 Kotlin 运行库版本：
    // 用它而非硬编码字符串，是为了确认「跑起来的」确实是新运行库，
    // 而不是源码里写死的自我介绍
    val runtimeVersion = KotlinVersion.CURRENT.toString()

    // 用 java.time.LocalDate 而非老的 Date 类：
    // 后者的大多数构造器与方法自 JDK 1.1 起就被标记为过时，
    // 官方推荐用 java.time 包替代，新代码没有理由再碰它
    val today = LocalDate.now()

    println("Hello, Kotlin $runtimeVersion!")
    println("今天：$today")
    println("JVM 版本：${System.getProperty("java.version")}")
}