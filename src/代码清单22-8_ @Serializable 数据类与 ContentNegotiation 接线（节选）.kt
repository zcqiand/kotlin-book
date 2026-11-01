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

fun Application.module() {
    // ContentNegotiation + json：客户端 Accept 头带 application/json 时自动序列化响应对象
    install(ContentNegotiation) {
        json()
    }
    // …… 路由登记见清单 22-4 与 22-6