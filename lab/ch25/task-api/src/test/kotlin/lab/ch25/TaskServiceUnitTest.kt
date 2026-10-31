package lab.ch25

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** 单元测试：TaskService 纯业务逻辑，仓储用内存测试替身——零数据库、毫秒级跑完。 */
class TaskServiceUnitTest {

  /** 测试替身：内存 map 仿真仓储，只按接口契约行为，不碰 Exposed。 */
  private class FakeTaskRepository : TaskRepository {
    private val seq = AtomicInteger(0)
    private val store = ConcurrentHashMap<Int, Task>()

    override suspend fun findAll(): List<Task> = store.values.sortedBy { it.id }

    override suspend fun findById(id: Int): Task? = store[id]

    override suspend fun create(title: String): Task {
      val task = Task(seq.incrementAndGet(), title, TaskStatus.TODO, System.currentTimeMillis())
      store[task.id] = task
      return task
    }

    override suspend fun updateStatus(id: Int, status: TaskStatus): Boolean {
      val current = store[id] ?: return false
      store[id] = current.copy(status = status)
      return true
    }

    override suspend fun delete(id: Int): Boolean = store.remove(id) != null

    override suspend fun countByStatus(): Map<TaskStatus, Int> =
        store.values.groupingBy { it.status }.eachCount()

    override suspend fun recentTitles(limit: Int): List<String> =
        store.values.sortedByDescending { it.id }.take(limit).map { it.title }

    override suspend fun ensureSchema() = Unit

    override suspend fun clearAll() = store.clear()
  }

  private val repo = FakeTaskRepository()
  private val service = TaskService(repo)

  @Test
  fun `创建任务初始状态为 TODO`() = runBlocking {
    val task = service.create("写周报")
    assertEquals(TaskStatus.TODO, task.status)
    assertTrue(task.id > 0)
  }

  @Test
  fun `合法迁移链 TODO 到 DOING 到 DONE 全程放行`() = runBlocking {
    val task = service.create("写周报")
    val doing = service.transition(task.id, TaskStatus.DOING)
    assertEquals(TaskStatus.DOING, doing.status)
    val done = service.transition(task.id, TaskStatus.DONE)
    assertEquals(TaskStatus.DONE, done.status)
    assertEquals(TaskStatus.DONE, service.get(task.id).status)
  }

  @Test
  fun `非法迁移 TODO 直接跳 DONE 抛 IllegalTransitionException`() = runBlocking {
    val task = service.create("跳步任务")
    val e =
        assertThrows<IllegalTransitionException> { service.transition(task.id, TaskStatus.DONE) }
    assertEquals(TaskStatus.TODO, e.from)
    assertEquals(TaskStatus.DONE, e.to)
    // 状态未被破坏：仍是 TODO
    assertEquals(TaskStatus.TODO, service.get(task.id).status)
  }

  @Test
  fun `DONE 是终态 无任何合法迁移`(): Unit = runBlocking {
    val task = service.create("终态任务")
    service.transition(task.id, TaskStatus.DOING)
    service.transition(task.id, TaskStatus.DONE)
    assertThrows<IllegalTransitionException> { service.transition(task.id, TaskStatus.TODO) }
    assertThrows<IllegalTransitionException> { service.transition(task.id, TaskStatus.DOING) }
  }

  @Test
  fun `stats 两路并发合流 计数与最近任务对死`() = runBlocking {
    service.create("任务甲")
    val t2 = service.create("任务乙")
    service.transition(t2.id, TaskStatus.DOING)
    val snapshot = service.stats()
    assertEquals(2, snapshot.countsByStatus.values.sum())
    assertEquals(1, snapshot.countsByStatus[TaskStatus.DOING])
    assertEquals(1, snapshot.countsByStatus[TaskStatus.TODO])
    // 最近任务按 id 倒序：任务乙最新在前
    assertTrue(snapshot.recentTitles.first() == "任务乙")
    // StateFlow 快照与返回值一致（update 已生效）
    assertEquals(snapshot, service.cachedStats())
  }

  @Test
  fun `删除后查无此任务 再删返回 false`() = runBlocking {
    val task = service.create("待删任务")
    assertTrue(service.delete(task.id))
    assertFalse(service.delete(task.id))
    assertEquals(emptyList<Task>(), service.list())
  }
}
