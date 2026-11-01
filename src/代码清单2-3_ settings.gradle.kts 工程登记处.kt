// settings.gradle.kts 是 Gradle 的工程登记处: 构建一启动就最先读它,
// 用来确定根工程叫什么名字、包含哪些子工程。
// 本章是单工程, 只需登记名字; 后续多模块工程会在这里继续声明子工程列表。
rootProject.name = "hello-gradle"