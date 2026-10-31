plugins {
    // 版本口径与案例仓 lab-management-system-kotlin 对齐（version-lock.json: kotlin 2.4.21）
    kotlin("jvm") version "2.4.21"
    application
}

group = "io.xr.book.ch16"
version = "0.1.0"

repositories { mavenCentral() }

dependencies {
    // 版本出处：version-lock.json 无显式 coroutines 条目，取案例仓 ktor-bom 3.6.0
    // 依赖树实际解析到的 kotlinx-coroutines-core 1.11.0（gradle dependencies 实测，非记忆）
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
}

kotlin { jvmToolchain(21) }

application { mainClass.set("lab.ch16.MainKt") }

// 中文输出逐字回执需要确定的编码：JDK 18+ 默认 UTF-8，但 Windows 控制台链路上
// stdout.encoding 仍可能回落到 GBK，显式钉死避免回执乱码
tasks.withType<JavaExec>().configureEach {
    jvmArgs("-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
}
