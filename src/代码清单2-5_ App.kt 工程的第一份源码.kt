package hello

fun main() {
    // 三行输出各验一环: 程序本身能跑、Kotlin 运行库在位、
    // 实际执行 JVM 与工具链约定的版本一致, 骨架是否搭通用输出直接可见
    println("hello-gradle: 第一个 Kotlin 工程已就绪")
    println("Kotlin 运行库版本: ${KotlinVersion.CURRENT}")
    println("JVM 版本: ${System.getProperty("java.version")}")
}