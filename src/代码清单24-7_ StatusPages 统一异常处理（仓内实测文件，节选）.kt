// 统一错误体：所有非 2xx 响应都长这一个样子，客户端只需解析两个字段
@Serializable
data class ApiError(val status: Int, val error: String)

// ……（省略：errorBody() 扩展，把状态码与原因装进 ApiError）

// StatusPages：把没被任何路由接住的异常统一收口成一个错误体。
// 不装它，未处理异常会变成一个没有响应体的裸 500，客户端拿不到
// 任何 JSON——错误面失去统一形态
install(StatusPages) {
    exception<Throwable> { call, cause ->
        call.application.log.error(
            "event=UNHANDLED_500 path=${call.request.uri} exception=${cause::class.simpleName} message=${cause.message}"
        )
        call.respond(
            HttpStatusCode.InternalServerError,
            call.errorBody(HttpStatusCode.InternalServerError, "internal_error")
        )
    }
}