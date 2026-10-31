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
    // 版本出处：家族案例仓 lab-management-system-android 的 gradle/libs.versions.toml
    // 实锁 coroutines = "1.11.0"（该仓 version-lock.json 同口径记 "kotlinx-coroutines": "1.11.0"）。
    // 主仓 output/xr-know-020/version-lock.json 未单列 kotlinx-coroutines（仅 kotlinx-serialization 1.9.0），
    // 属 version-lock 缺口，待回填后此处版本号与主仓口径对齐。
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
}

application {
    // 顶层 main 写在 StructuredMain.kt，Kotlin 编出的门面类是 StructuredMainKt
    mainClass.set("demo.StructuredMainKt")
}
