package helloktor

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

// 数据模型标注 @Serializable，内容协商插件才能把它序列化成 JSON
@Serializable
data class Book(val id: Int, val title: String, val author: String)

// 错误响应也是教材演示的一部分：404 分支返回结构化错误而不是裸文本
@Serializable
data class ErrorResponse(val error: String)

// 种子数据放顶层 val：热重载演示时改这一处即可，不必触碰路由代码
private val books = listOf(
    Book(1, "Kotlin in Action", "Dmitry Jemerov"),
    Book(2, "Atomic Kotlin", "Bruce Eckel"),
    Book(3, "Programming Kotlin", "Stephen Samuel"),
)

fun main(args: Array<String>) = EngineMain.main(args)

// 模块函数：EngineMain 按 application.conf 里 modules 指到的入口，本进程内所有装配从这里开始
fun Application.module() {
    // ContentNegotiation + json：客户端 Accept 头带 application/json 时自动序列化响应对象
    install(ContentNegotiation) {
        json()
    }
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
        // 嵌套分组：/api 前缀下再挂 /books，路由表随资源增长而保持扁平可读
        route("/api") {
            route("/books") {
                // 列表端点：返回整个集合
                get {
                    call.respond(books)
                }
                // 单条端点：查到返回对象，查不到返回 404 + 结构化错误
                get("/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull()
                    val book = books.find { it.id == id }
                    if (book == null) {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("Book $id not found"))
                    } else {
                        call.respond(book)
                    }
                }
            }
        }
    }
}
