package lab.ch25

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** GET /stats 取最近任务条数——具名常量沿 ch21 纪律。 */
private const val RECENT_LIMIT = 3

/** 结构化运维日志专用 logger：后台副作用（OPS_*）全部从这里走，与框架 logger 分开。 */
val opsLog: Logger = LoggerFactory.getLogger("lab.ch25.OpsAudit")

/**
 * 服务层：业务规则在这里，SQL 细节在仓储层之下，HTTP 形态在路由层之上。 方法全挂起——Ktor 挂起处理一路承接下来（沿 ch22/ch23 口径）。
 *
 * 协程三线合流各占一处：
 * - async：stats() 把「按状态计数」与「最近任务」两路并发拆出再 await 合流（取值 → async）；
 * - launch：变更操作落库后派一个后台副作用任务写 OPS_* 审计日志，无返回值（副作用 → launch）；
 * - StateFlow：statsFlow 持有进程内统计快照，变更时 update，路由层读 value——沿 ch18 状态容器口径。
 */
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

  /** 状态迁移：合法性在这里判，非法即抛业务异常（由 StatusPages 统一收口成 409）。 */
  suspend fun transition(id: Int, target: TaskStatus): Task {
    val current = get(id)
    if (!TransitionRules.canTransition(current.status, target)) {
      throw IllegalTransitionException(current.status, target)
    }
    repo.updateStatus(id, target)
    val updated = current.copy(status = target)
    audit("OPS_TRANSITION", updated)
    return updated
  }

  suspend fun delete(id: Int): Boolean {
    val task = repo.findById(id) ?: return false
    val deleted = repo.delete(id)
    if (deleted) {
      audit("OPS_DELETE", task)
    }
    return deleted
  }

  /**
   * 统计：两路并发拆分取值再合流；结果既返回给调用方，也 update 进 StateFlow 快照。 两个 async
   * 各自包一次事务查询，互不阻塞——顺序写法要串行等两趟，合流写法只要一趟最慢的。
   */
  suspend fun stats(): StatsSnapshot = coroutineScope {
    val counts = async { repo.countByStatus() }
    val recent = async { repo.recentTitles(RECENT_LIMIT) }
    val snapshot = StatsSnapshot(countsByStatus = counts.await(), recentTitles = recent.await())
    statsFlow.update { snapshot }
    snapshot
  }

  /** 路由层读快照走这里：不触发重算，只取 StateFlow 当前 value。 */
  fun cachedStats(): StatsSnapshot = statsFlow.value

  /** 后台副作用：launch 派发、无返回值——选型判据沿 ch17（副作用 launch / 取值 async）。 */
  private fun audit(event: String, task: Task) {
    auditScope.launch {
      opsLog.info("event=$event id=${task.id} title=${task.title} status=${task.status}")
    }
  }
}
