// 工程登记处: ch2 的单体 hello-gradle 只有 rootProject.name 一行,
// 拆成多模块后, 子工程列表也在这里声明——Gradle 靠这份清单决定哪些目录参与构建。
rootProject.name = "multi-module-demo"

// 业务逻辑模块: 问候服务与版本横幅的生成规则, 不含程序入口
include(":core")

// 展示模块: main 入口, 只面向 :core 的公开 API 编程
include(":app")

// 仓库声明收敛到 settings: 多模块工程若每个模块各写一份 repositories,
// 新模块容易漏带, 仓库地址一换就要逐个模块改。这里一处声明,
// 所有子工程共享同一下载来源, 与版本目录同属「单一事实源」思路。
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
