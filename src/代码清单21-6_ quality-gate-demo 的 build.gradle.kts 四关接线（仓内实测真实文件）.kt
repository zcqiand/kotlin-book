// ch21 四级门控演示工程：JUnit 单元测试 + 参数化测试 + ktfmt + detekt，四关全部挂在 check 下。
// 接线方式照抄案例仓 lab-management-system-kotlin（同一套版本锁），只换演示域。
plugins {
  kotlin("jvm") version "2.4.21"
  id("com.ncorti.ktfmt.gradle") version "0.27.0"
  id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

group = "library"

version = "0.1.0"

repositories { mavenCentral() }

dependencies {
  // JUnit 6 口径：版本全部由 junit-bom 一处收口，禁裸写。
  // launcher 必须显式声明：Gradle 9 不再自动提供 test launcher，
  // jupiter 6 起 platform engine 也改为非传递依赖，缺一行测试就跑不起来。
  testImplementation(platform("org.junit:junit-bom:6.1.3"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.junit.platform:junit-platform-engine")
  testImplementation("org.junit.platform:junit-platform-launcher")
}

kotlin { jvmToolchain(21) }

tasks.test {
  useJUnitPlatform()
  // 回执需要逐条 PASSED/FAILED 行：Gradle 默认只在测试失败时给摘要，
  // 全绿时一个字都不提，无法从控制台直接数出测试条数。
  testLogging { events("passed", "failed", "skipped") }
}