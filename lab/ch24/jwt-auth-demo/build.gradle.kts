// ch24 JWT 认证演示工程：jjwt 自签 HS256 + Ktor Authentication jwt 插件 + StatusPages 统一错误体。
// 版本全部经 gradle/libs.versions.toml 目录引用，Ktor 侧再由 ktor-bom 平台收口——沿 ch22 体例。
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
  application
}

// 入口走 EngineMain：由它读 application.conf（端口/模块），而不是代码里写死
application { mainClass = "io.ktor.server.netty.EngineMain" }

group = "jwt-auth-demo"

version = "0.1.0"

repositories { mavenCentral() }

dependencies {
  // Ktor 版本由 BOM 一处收口，模块本身不写版本号
  implementation(platform(libs.ktor.bom))
  implementation(libs.ktor.server.netty)
  implementation(libs.ktor.server.content.negotiation)
  implementation(libs.ktor.serialization.kotlinx.json)
  implementation(libs.ktor.server.auth)
  implementation(libs.ktor.server.auth.jwt)
  implementation(libs.ktor.server.status.pages)
  implementation(libs.kotlinx.serialization.json)

  // jjwt：api 提供编译期 API（builder/parser），impl 与 jackson 序列化只在运行期需要，
  // 闭包面不进调用方 classpath（version-lock 口径：api 显式 + impl/jackson runtimeOnly）
  implementation(libs.jjwt.api)
  runtimeOnly(libs.jjwt.impl)
  runtimeOnly(libs.jjwt.jackson)

  // slf4j：api 钉版本对齐 version-lock，simple 作控制台绑定——沿 ch23 先例
  implementation(libs.slf4j.api)
  runtimeOnly(libs.slf4j.simple)
}

kotlin { jvmToolchain(21) }

// TokenService 三回合演示入口（签发/合法校验/篡改校验）：独立 JavaExec 任务，
// 不占用 application 的 EngineMain 主入口，逐字回执见 receipts.md
tasks.register<JavaExec>("runTokenDemo") {
  mainClass.set("lab.ch24.TokenDemoKt")
  classpath(sourceSets["main"].runtimeClasspath)
}
