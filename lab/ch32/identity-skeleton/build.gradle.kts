// 项目二工程骨架最小可运行件：零 Ktor/零数据库/零服务器。
// 版本口径对码案例仓 saas-identity-platform-kotlin（v0.2.1-20261025）：
// Kotlin jvm 2.4.21 / exposed-bom 1.5.1（禁裸写版本）/ junit-bom 6.1.3。
plugins {
  kotlin("jvm") version "2.4.21"
}

group = "io.xr"

version = "0.1.0"

repositories { mavenCentral() }

dependencies {
  // 测试：junit-bom 收口（JUnit 6 起 platform 是 jupiter 的非传递依赖，launcher 必须显式声明）
  testImplementation(platform("org.junit:junit-bom:6.1.3"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.junit.platform:junit-platform-launcher")
  // kotlin.test 注解/断言（useJUnitPlatform() 下自动走 junit5 变体，沿 ch30 口径）
  testImplementation(kotlin("test"))
  // 实体层骨架编译面：exposed-bom 收口，只要编译得过，不起库不连库
  implementation(platform("org.jetbrains.exposed:exposed-bom:1.5.1"))
  implementation("org.jetbrains.exposed:exposed-core")
  implementation("org.jetbrains.exposed:exposed-kotlin-datetime")
}

kotlin { jvmToolchain(21) }

tasks.test { useJUnitPlatform() }
