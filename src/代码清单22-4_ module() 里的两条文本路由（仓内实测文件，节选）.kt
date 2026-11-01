// 模块函数：EngineMain 按 application.conf 里 modules 指到的入口，本进程内所有装配从这里开始
fun Application.module() {
    routing {
        // 根路径返回纯文本，演示最小可用的端点
        get("/") {
            call.respondText("Hello, Ktor! Hot reload works: this text changed without restarting the process.")
        }
        // 路径参数用花括号占位，call.parameters 取值
        get("/hello/{name}") {
            val name = call.parameters["name"] ?: "stranger"
            call.respondText("Hello, $name! Welcome to Ktor routing.")
        }
    }
    // …… /api 嵌套分组与内容协商接线见清单 22-6、22-8
}