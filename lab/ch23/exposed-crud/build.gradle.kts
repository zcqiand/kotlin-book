plugins {
  alias(libs.plugins.kotlin.jvm)
  application
}

repositories { mavenCentral() }

dependencies {
  // Exposed 各模块版本走 exposed-bom 收口（ADR-0046 同款：禁裸写版本）
  implementation(platform(libs.exposed.bom))
  implementation(libs.exposed.core)
  implementation(libs.exposed.r2dbc)
  // R2DBC 驱动：BOM 外钉 exact（与案例仓 saas-identity-platform-kotlin 同款）
  implementation(libs.r2dbc.postgresql)
  implementation(libs.kotlinx.coroutines.core)
  // slf4j-api 由 exposed/r2dbc 传递引入，显式钉版本对齐 version-lock；simple 作绑定
  implementation(libs.slf4j.api)
  runtimeOnly(libs.slf4j.simple)

  // JUnit 5.11+/6 起 platform 是 jupiter 的非传递依赖，Gradle 9 也不再自动提供
  // launcher——必须显式声明，版本由 junit-bom 管（案例仓 build.gradle.kts 同款注释口径）
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.junit.platform.engine)
  testImplementation(libs.junit.platform.launcher)
}

kotlin { jvmToolchain(21) }

application { mainClass.set("lab.ch23.MainKt") }

tasks.test {
  useJUnitPlatform()
  testLogging { events("passed", "failed", "skipped") }
}
