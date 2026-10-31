plugins {
    kotlin("jvm") version "2.4.21"
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    // 多个演示共用一个入口，按命令行参数分发到各演示函数
    // 入口文件是 ExtMain.kt，顶层 main 编出的门面类是 ExtMainKt（区别于 ch11 的 Main.kt）
    mainClass.set("demo.ExtMainKt")
}

repositories {
    mavenCentral()
}
