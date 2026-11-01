fun Application.module() {
  install(ContentNegotiation) { json() }
  installErrors()
  installAuth()
  val service = TaskService(ExposedTaskRepository(bootDatabase()))
  routing { taskRoutes(service) }
  log.info("event=APP_READY service=task-api endpoints=7")
}