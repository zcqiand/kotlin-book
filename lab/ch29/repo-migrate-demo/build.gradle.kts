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
  // R2DBC 驱动：BOM 外钉 exact（沿 ch23 exposed-crud 同款）
  implementation(libs.r2dbc.postgresql)
  implementation(libs.kotlinx.coroutines.core)
  // 种子 JSON 解析：版本沿 version-lock 口径（kotlinx-serialization 1.9.0，只当库用、不开编译器插件）
  implementation(libs.kotlinx.serialization.json)
  // slf4j-api 由 exposed/r2dbc 传递引入，显式钉版本对齐 version-lock；simple 作绑定
  implementation(libs.slf4j.api)
  runtimeOnly(libs.slf4j.simple)
}

kotlin { jvmToolchain(21) }

application { mainClass.set("lab.ch29.MainKt") }
