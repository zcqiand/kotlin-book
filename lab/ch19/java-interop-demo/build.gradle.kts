plugins {
    kotlin("jvm") version "2.4.21"
    application
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

// 混编关键一处：Gradle 默认让 javac（compileJava）跑在 Kotlin 编译之前，
// Java 侧代码看不见 Kotlin 类，Java→Kotlin 方向直接「找不到符号」。
// 这里显式把 compileKotlin 的产物挂进 javac 的 classpath 并调整任务顺序，
// 同一 source set 里的两个方向才都能编译。
tasks.named<JavaCompile>("compileJava") {
    dependsOn(tasks.named("compileKotlin"))
    classpath += files(tasks.named("compileKotlin").map { it.outputs })
}

dependencies {
    // 本章纯 Kotlin 标准库 + JDK，无第三方依赖
}

application {
    // 顶层 main 写在 InteropMain.kt，Kotlin 编出的门面类是 InteropMainKt
    mainClass.set("demo.InteropMainKt")
}
