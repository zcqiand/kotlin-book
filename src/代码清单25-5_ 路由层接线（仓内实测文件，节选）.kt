  authenticate("admin-jwt") {
    get("/tasks") {
      call.adminIdentity()
      call.respond(service.list())
    }
    post("/tasks") {
      call.adminIdentity()
      val req = call.receive<TaskCreateRequest>()
      call.respond(HttpStatusCode.Created, service.create(req.title))
    }
    get("/tasks/{id}") {
      call.adminIdentity()
      call.respond(service.get(call.pathId()))
    }
    post("/tasks/{id}/transition") {
      call.adminIdentity()
      val req = call.receive<TransitionRequest>()
      call.respond(service.transition(call.pathId(), req.targetStatus))
    }
    delete("/tasks/{id}") {
      call.adminIdentity()
      val id = call.pathId()
      if (!service.delete(id)) throw NotFoundException("task $id")
      call.respond(ApiError(status = HttpStatusCode.OK.value, error = "deleted"))
    }
    get("/stats") {
      call.adminIdentity()
      service.stats()
      call.respond(service.cachedStats())
    }
  }