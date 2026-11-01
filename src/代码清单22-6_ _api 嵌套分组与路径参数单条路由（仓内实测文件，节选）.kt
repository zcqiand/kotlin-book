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