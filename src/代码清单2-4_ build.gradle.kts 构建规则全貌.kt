// build.gradle.kts 回答"这个工程怎么构建": 装哪些插件、用哪个 JDK、
// 依赖从哪里下载、程序入口在哪。Gradle 读到它, 才知道每条命令背后该干什么。

plugins {
    // Kotlin JVM 插件: 注册编译 Kotlin 的任务与依赖配置。
    // 版本必须写成精确值 "2.4.21", 插件解析按精确版本定位, 写 "2.4.x" 无法运行。
    kotlin("jvm") version "2.4.21"

    // application 插件: 赋予工程"可运行应用"的身份, ./gradlew run 靠它才有。
    application
}

java {
    toolchain {
        // 工具链把 JDK 版本钉死在工程里: Gradle 会自动探测本机已安装的 JDK 21 并用它编译运行,
        // 编译不随执行命令那台机器的默认 JDK 漂移。
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    // 入口指向编译产物中的类: Kotlin 把 hello 包里 App.kt 的顶层 main
    // 编译成 AppKt 类的静态方法, 所以这里写 hello.AppKt 而非 hello.App。
    mainClass = "hello.AppKt"
}

repositories {
    // 声明依赖的下载来源: Maven Central 是 Kotlin/Java 生态的默认公共仓库,
    // 先声明坐标再谈依赖, Gradle 才知道去哪里找 jar。
    mavenCentral()
}

dependencies {
    // 本章工程零第三方依赖, 此段先留位:
    // 后续章节引入测试框架与各类库时, 坐标统一写在这里。
}