plugins {
    kotlin("jvm") version "2.4.21"
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    // 多个演示共用一个入口，按命令行参数分发到各演示函数
    mainClass.set("demo.MainKt")
}

repositories {
    mavenCentral()
}
