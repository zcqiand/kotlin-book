plugins {
  kotlin("jvm") version "2.4.21"
  id("com.ncorti.ktfmt.gradle") version "0.27.0"
  id("io.gitlab.arturbosch.detekt") version "1.23.8"
  application
}

group = "io.xr"

version = "0.1.0"

repositories { mavenCentral() }

dependencies {
  // 四级门控第③级：JUnit 6.1.3（junit-bom 收口，沿案例仓 version-lock 口径）
  testImplementation(platform("org.junit:junit-bom:6.1.3"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.junit.platform:junit-platform-launcher")
  // db-check 冒烟：PG JDBC 驱动。案例仓运行面走 r2dbc-postgresql 1.1.3.RELEASE，
  // 教学最小工程取同步 JDBC 线（版本 lab 自钉 42.7.4，mavenCentral 实测可得）
  implementation("org.postgresql:postgresql:42.7.4")
}

kotlin { jvmToolchain(21) }

application { mainClass.set("lab.ch31.MainKt") }

tasks.test { useJUnitPlatform() }

detekt {
  buildUponDefaultConfig = true
  config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

ktfmt {
  // 案例仓同款：kotlin语源格式门走 ktfmt 插件，默认风格即门内合法形态
}
