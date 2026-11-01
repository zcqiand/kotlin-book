plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

repositories { mavenCentral() }

dependencies {
    // 零第三方依赖：测试框架走 kotlin-test（版本目录收口，沿 ch29 目录方式）
    testImplementation(libs.kotlin.test)
}

kotlin { jvmToolchain(21) }

application { mainClass.set("lab.ch30.MainKt") }
