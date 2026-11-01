// 业务层：规则与协议无关，只答业务话
class TaskService(private val repo: TaskRepository) {
    suspend fun create(title: String): Task {
        if (title.isBlank()) throw EmptyTitleException()
        return repo.create(title.trim())
    }
}

// 协议面：收请求、调服务、组装状态码
post("/tasks") {
    val req = call.receive<CreateTaskRequest>()
    val task = service.create(req.title)
    call.respond(HttpStatusCode.Created, task)
}