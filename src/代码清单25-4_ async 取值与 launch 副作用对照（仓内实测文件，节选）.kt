  suspend fun stats(): StatsSnapshot = coroutineScope {
    val counts = async { repo.countByStatus() }
    val recent = async { repo.recentTitles(RECENT_LIMIT) }
    val snapshot = StatsSnapshot(countsByStatus = counts.await(), recentTitles = recent.await())
    statsFlow.update { snapshot }
    snapshot
  }

  // ……（省略：cachedStats，读快照当前值）

  private fun audit(event: String, task: Task) {
    auditScope.launch {
      opsLog.info("event=$event id=${task.id} title=${task.title} status=${task.status}")
    }
  }