// :app 的构建规则: 展示模块, 自己不写业务, 只面向 :core 的公开 API 编程。
plugins {
    alias(libs.plugins.kotlin.jvm)
    // application 只装在入口模块: gradlew :app:run 的 run 任务由它提供,
    // :core 是纯库模块, 不需要也不应该有可运行身份。
    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // 模块间依赖走 project 坐标: :app 编译期可见 :core 的公开 API。
    implementation(project(":core"))

    // 日志实现只在运行期需要: simple 绑定把 slf4j-api 的调用落到控制台,
    // runtimeOnly 保证它进 classpath 却不参与编译, :app 想在编译期用它也用不了。
    runtimeOnly(libs.slf4j.simple)
}

application {
    // Kotlin 把 hello 包 Main.kt 的顶层 main 编译成 MainKt 类的静态方法
    mainClass.set("hello.MainKt")
}

// 中文输出逐字回执需要确定的编码: JDK 18+ 默认 UTF-8, 但 Windows 控制台链路上
// stdout.encoding 仍可能回落到 GBK, 显式钉死避免回执乱码
tasks.withType<JavaExec>().configureEach {
    jvmArgs("-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
}
