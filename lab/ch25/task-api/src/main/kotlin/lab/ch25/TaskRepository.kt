package lab.ch25

import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.deleteAll
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update

/** 仓储接口：服务层依赖它做单元测试替身，路由层则完全不摸这层。 全挂起函数——上层是 Ktor 挂起处理，链路自上而下全程协程。 */
interface TaskRepository {
  suspend fun findAll(): List<Task>

  suspend fun findById(id: Int): Task?

  suspend fun create(title: String): Task

  suspend fun updateStatus(id: Int, status: TaskStatus): Boolean

  suspend fun delete(id: Int): Boolean

  suspend fun countByStatus(): Map<TaskStatus, Int>

  suspend fun recentTitles(limit: Int): List<String>

  /** 建表/清表只给测试与启动引导用，不进服务层视野。 */
  suspend fun ensureSchema()

  suspend fun clearAll()
}

/**
 * Exposed 仓储实现：SQL 细节全部封在 suspendTransaction 块内（沿 ch23 v1 纪律： 一切 DSL/查询执行必须在 suspendTransaction
 * 上下文内），服务层只见领域对象。
 */
class ExposedTaskRepository(private val db: R2dbcDatabase) : TaskRepository {

  override suspend fun findAll(): List<Task> =
      suspendTransaction(db) {
        TasksTable.selectAll().orderBy(TasksTable.id to SortOrder.ASC).toList().map(::rowToTask)
      }

  override suspend fun findById(id: Int): Task? =
      suspendTransaction(db) {
        TasksTable.selectAll()
            .where { TasksTable.id eq id }
            .toList()
            .firstOrNull()
            ?.let(::rowToTask)
      }

  override suspend fun create(title: String): Task =
      suspendTransaction(db) {
        val now = System.currentTimeMillis()
        val newId =
            TasksTable.insert {
              it[TasksTable.title] = title
              it[TasksTable.status] = TaskStatus.TODO.name
              it[TasksTable.createdAt] = now
            } get TasksTable.id
        Task(id = newId, title = title, status = TaskStatus.TODO, createdAt = now)
      }

  override suspend fun updateStatus(id: Int, status: TaskStatus): Boolean =
      suspendTransaction(db) {
        TasksTable.update({ TasksTable.id eq id }) { it[TasksTable.status] = status.name } > 0
      }

  override suspend fun delete(id: Int): Boolean =
      suspendTransaction(db) { TasksTable.deleteWhere { TasksTable.id eq id } > 0 }

  override suspend fun countByStatus(): Map<TaskStatus, Int> =
      suspendTransaction(db) {
        TasksTable.selectAll()
            .toList()
            .groupingBy { TaskStatus.valueOf(it[TasksTable.status]) }
            .eachCount()
      }

  override suspend fun recentTitles(limit: Int): List<String> =
      suspendTransaction(db) {
        TasksTable.selectAll().orderBy(TasksTable.id to SortOrder.DESC).limit(limit).toList().map {
          it[TasksTable.title]
        }
      }

  override suspend fun ensureSchema(): Unit =
      suspendTransaction(db) { SchemaUtils.create(TasksTable) }

  override suspend fun clearAll(): Unit = suspendTransaction(db) { TasksTable.deleteAll() }

  private fun rowToTask(row: org.jetbrains.exposed.v1.core.ResultRow): Task =
      Task(
          id = row[TasksTable.id],
          title = row[TasksTable.title],
          status = TaskStatus.valueOf(row[TasksTable.status]),
          createdAt = row[TasksTable.createdAt],
      )
}
