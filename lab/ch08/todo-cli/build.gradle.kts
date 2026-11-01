plugins {
    // 与第 2 章同一套骨架：Kotlin JVM 插件按精确版本定位，写法沿用清单 2-4 的交代
    kotlin("jvm") version "2.4.21"
    application
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    // 与 hello-gradle 的差异一：入口从 hello.AppKt 换成 todo.TodoCliKt
    mainClass = "todo.TodoCliKt"
}

repositories {
    mavenCentral()
}
