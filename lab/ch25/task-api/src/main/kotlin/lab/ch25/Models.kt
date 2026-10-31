package lab.ch25

import kotlinx.serialization.Serializable

/** 任务三态：合法迁移仅 TODO→DOING、DOING→DONE，其余一律 409。 */
@Serializable
enum class TaskStatus {
  TODO,
  DOING,
  DONE,
}

/** 迁移规则独立成对象：服务层判定「能不能迁」，仓储层只管「怎么落库」， 规则表一眼读出全部合法链路，不用翻状态机文档。 */
object TransitionRules {
  private val allowed =
      mapOf(
          TaskStatus.TODO to TaskStatus.DOING,
          TaskStatus.DOING to TaskStatus.DONE,
      )

  fun canTransition(from: TaskStatus, to: TaskStatus): Boolean = allowed[from] == to
}

/** 领域模型（响应体走 @Serializable DTO——沿 ch24 教训，裸 map 混装会被拒绝序列化）。 */
@Serializable
data class Task(val id: Int, val title: String, val status: TaskStatus, val createdAt: Long)

@Serializable data class TaskCreateRequest(val title: String)

@Serializable data class TransitionRequest(val targetStatus: TaskStatus)

/** GET /stats 响应：按状态计数 + 最近任务两路并发取回后合流。 */
@Serializable
data class StatsSnapshot(val countsByStatus: Map<TaskStatus, Int>, val recentTitles: List<String>)

/** 统一错误体 {status, error}——沿 ch24 ApiError 形态。 */
@Serializable data class ApiError(val status: Int, val error: String)

/** 非法状态迁移：业务异常，StatusPages 映射 409 Conflict。 */
class IllegalTransitionException(val from: TaskStatus, val to: TaskStatus) :
    RuntimeException("illegal transition: $from -> $to")

/** 资源不存在：StatusPages 映射 404 Not Found。 */
class NotFoundException(what: String) : RuntimeException("$what not found")
