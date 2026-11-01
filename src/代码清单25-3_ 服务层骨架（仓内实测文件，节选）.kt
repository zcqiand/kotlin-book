class TaskService(private val repo: TaskRepository) {

  // 自持作用域：审计日志是「发射后不管」的后台副作用，不随单个请求协程取消而中止
  private val auditScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  private val statsFlow = MutableStateFlow(StatsSnapshot(emptyMap(), emptyList()))

  suspend fun list(): List<Task> = repo.findAll()

  suspend fun get(id: Int): Task = repo.findById(id) ?: throw NotFoundException("task $id")

  suspend fun create(title: String): Task {
    val task = repo.create(title)
    audit("OPS_CREATE", task)
    return task
  }

  // ……（省略：transition / delete / stats / cachedStats，见清单 25-4）
}