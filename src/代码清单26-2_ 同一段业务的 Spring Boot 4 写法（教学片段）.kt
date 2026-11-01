// 业务层：与清单 26-1 逐行同构，只换了异常类型与装配声明
@Service
class TaskService(private val repo: TaskRepository) {
    suspend fun create(title: String): Task {
        if (title.isBlank()) throw InvalidTitleException()
        return repo.create(title.trim())
    }
}

// 协议面：路径与方法改注解声明，装配交给容器，挂起标记原样保留
@RestController
class TaskController(private val service: TaskService) {
    @PostMapping("/tasks")
    suspend fun create(@RequestBody req: CreateTaskRequest): ResponseEntity<Task> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.create(req.title))
}