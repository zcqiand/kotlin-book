// 第 34 章认证/SSO/RBAC 菜单授权实测工程：Exposed 1.5.1（R2DBC 面）+ jjwt 0.12.6 + 本机 PG 127.0.0.1:54321。
// 版本口径对码案例仓 saas-identity-platform-kotlin（v0.2.1-20261025）：
// Kotlin jvm 2.4.21 / exposed-bom 1.5.1（BOM 收口禁裸写版本）/ junit-bom 6.1.3 / jjwt 0.12.6。
plugins {
  alias(libs.plugins.kotlin.jvm)
}

group = "io.xr"

version = "0.1.0"

repositories { mavenCentral() }

dependencies {
  // 测试：junit-bom 收口（launcher 必须显式声明，沿 ch32/ch33 口径）
  testImplementation(platform("org.junit:junit-bom:6.1.3"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.junit.platform:junit-platform-launcher")
  testImplementation(kotlin("test"))
  // 数据面：exposed-bom 收口 + r2dbc-postgresql（沿 ch29/ch33 已实证栈）
  implementation(platform(libs.exposed.bom))
  implementation(libs.exposed.core)
  implementation(libs.exposed.kotlin.datetime)
  implementation(libs.exposed.r2dbc)
  implementation(libs.r2dbc.postgresql)
  implementation(libs.kotlinx.coroutines.core)
  // JWT 面：jjwt 三件（api 走 implementation；impl/jackson 是运行期引擎走 runtimeOnly）
  implementation(libs.jjwt.api)
  runtimeOnly(libs.jjwt.impl)
  runtimeOnly(libs.jjwt.jackson)
  // slf4j-api 由 exposed/r2dbc 传递引入，显式钉版本；simple 作绑定
  implementation(libs.slf4j.api)
  runtimeOnly(libs.slf4j.simple)
}

kotlin { jvmToolchain(21) }

tasks.test { useJUnitPlatform() }
