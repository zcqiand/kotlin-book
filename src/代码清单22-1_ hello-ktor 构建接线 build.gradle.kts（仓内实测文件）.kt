// ch22 Ktor 演示工程：EngineMain + HOCON 配置 + 内容协商 + 路由嵌套分组。
// 版本全部经 gradle/libs.versions.toml 目录引用，Ktor 侧再由 ktor-bom 平台收口。
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
  application
}

// 入口走 EngineMain：由它读 application.conf（端口/模块/开发模式），而不是代码里写死
application { mainClass = "io.ktor.server.netty.EngineMain" }

group = "hello-ktor"

version = "0.1.0"

repositories { mavenCentral() }

dependencies {
  // Ktor 版本由 BOM 一处收口，模块本身不写版本号
  implementation(platform(libs.ktor.bom))
  implementation(libs.ktor.server.netty)
  implementation(libs.ktor.server.content.negotiation)
  implementation(libs.ktor.serialization.kotlinx.json)
  implementation(libs.kotlinx.serialization.json)
}

kotlin { jvmToolchain(21) }

// 供热重载实验直启 JVM：把运行时依赖拷到固定目录，classpath 才好拼
tasks.register<Copy>("copyRuntimeDeps") {
  from(configurations.runtimeClasspath)
  into(layout.buildDirectory.dir("runtime-deps"))
}