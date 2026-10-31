// ch25 task-api：迷你任务管理 API——Ktor + Exposed(r2dbc) + jjwt + 协程四线合流工程。
// 四级门控沿 ch21 接线（ktfmt→detekt→test→build 全挂 check）；版本全走 gradle/libs.versions.toml
// 目录引用（本书第五个 version catalog），Ktor/exposed 两侧由各自 BOM 收口——沿 ch22/ch23 体例。
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.detekt)
  application
}

// 入口走 EngineMain：由它读 application.conf（端口 8082/模块），而不是代码里写死
application { mainClass = "io.ktor.server.netty.EngineMain" }

group = "task-api"

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

  // Exposed 各模块版本走 exposed-bom 收口——沿 ch23 先例
  implementation(platform(libs.exposed.bom))
  implementation(libs.exposed.core)
  implementation(libs.exposed.r2dbc)
  implementation(libs.r2dbc.postgresql)

  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.serialization.json)

  // jjwt：api 显式 + impl/jackson runtimeOnly——version-lock 口径，沿 ch24
  implementation(libs.jjwt.api)
  runtimeOnly(libs.jjwt.impl)
  runtimeOnly(libs.jjwt.jackson)

  // slf4j：api 钉版本对齐 version-lock，simple 作控制台绑定——沿 ch23/ch24
  implementation(libs.slf4j.api)
  runtimeOnly(libs.slf4j.simple)

  // JUnit 6：junit-bom 收口 + launcher 显式声明（Gradle 9 不再自动提供）——沿 ch21/ch23
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.junit.platform.engine)
  testImplementation(libs.junit.platform.launcher)
}

kotlin { jvmToolchain(21) }

tasks.test {
  useJUnitPlatform()
  // 回执需要逐条 PASSED/FAILED 行——沿 ch21/ch23 同款配置
  testLogging { events("passed", "failed", "skipped") }
}

// 验收收尾用：清掉 tasks 表（验证库 taskapi_ch25_verify 保留盘上），
// 供 receipts.md「PG 库准备/清理记录」段——独立 JavaExec，不占 EngineMain 主入口
tasks.register<JavaExec>("dropTasksTable") {
  mainClass.set("lab.ch25.CleanupKt")
  classpath(sourceSets["main"].runtimeClasspath)
}
