plugins {
    kotlin("jvm") version "2.4.21"
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    // 多个演示共用一个入口，按命令行参数分发到各演示函数
    // 入口文件是 Main.kt，顶层 main 编出的门面类是 MainKt
    mainClass.set("demo.MainKt")
}

repositories {
    mavenCentral()
}
