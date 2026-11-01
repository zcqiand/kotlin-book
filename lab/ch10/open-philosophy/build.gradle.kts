plugins {
    kotlin("jvm") version "2.4.21"
    application
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "demo.DemoKt"
}

repositories {
    mavenCentral()
}
