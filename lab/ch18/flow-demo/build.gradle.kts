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

dependencies {
    // 版本出处：主仓 output/xr-know-020/version-lock.json 已回填 kotlinx-coroutines = "1.11.0"
    // （2026-11-01，双源实证：android 案例仓 libs.versions.toml 实锁 + ch16/ch17 lab 依赖树实测解析同值）。
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
}

application {
    // 顶层 main 写在 FlowMain.kt，Kotlin 编出的门面类是 FlowMainKt
    mainClass.set("demo.FlowMainKt")
}
