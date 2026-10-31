package lab.ch25

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

/** 集成测试：对真实 PostgreSQL（127.0.0.1:54321，库 taskapi_ch25_verify）跑全链路断言——沿 ch23 形态。 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TaskApiIntegrationTest {
  // lazy：先等 setup 建好验证库，再发起连接（连接指向不存在的库会 initializationError）
  private val db: R2dbcDatabase by lazy { connectVerify() }
  private val repo by lazy { ExposedTaskRepository(db) }
  private val service by lazy { TaskService(repo) }

  @BeforeAll
  fun setup(): Unit = runBlocking {
    ensureVerifyDatabase()
    repo.ensureSchema()
    repo.clearAll()
  }

  @Test
  fun `create 落库后 findAll 查得回且初始 TODO`() = runBlocking {
    val task = service.create("集成写周报")
    val all = service.list()
    assertTrue(all.any { it.id == task.id && it.title == "集成写周报" && it.status == TaskStatus.TODO })
  }

  @Test
  fun `迁移链全经真库逐段落库`() = runBlocking {
    val task = service.create("集成迁移链")
    service.transition(task.id, TaskStatus.DOING)
    assertEquals(TaskStatus.DOING, service.get(task.id).status)
    service.transition(task.id, TaskStatus.DONE)
    assertEquals(TaskStatus.DONE, service.get(task.id).status)
  }

  @Test
  fun `非法迁移抛业务异常且真库状态不变`() = runBlocking {
    val task = service.create("集成反例")
    assertThrows<IllegalTransitionException> { service.transition(task.id, TaskStatus.DONE) }
    assertEquals(TaskStatus.TODO, service.get(task.id).status)
  }

  @Test
  fun `delete 真删 查无残留`() = runBlocking {
    val task = service.create("集成待删")
    assertTrue(service.delete(task.id))
    assertNull(repo.findById(task.id))
  }

  @Test
  fun `countByStatus 与 recentTitles 与表内分布对死`(): Unit = runBlocking {
    // 自清场：其他用例的遗留行不计入本用例分布
    repo.clearAll()
    val a = service.create("分布甲")
    val b = service.create("分布乙")

    val c = service.create("分布丙")
    service.transition(b.id, TaskStatus.DOING)
    service.transition(c.id, TaskStatus.DOING)
    service.transition(c.id, TaskStatus.DONE)
    val counts = repo.countByStatus()
    assertEquals(3, counts.values.sum())
    assertEquals(1, counts[TaskStatus.TODO])
    assertEquals(1, counts[TaskStatus.DOING])
    assertEquals(1, counts[TaskStatus.DONE])
    val recent = repo.recentTitles(2)
    assertEquals(listOf("分布丙", "分布乙"), recent)
    // 与计数对死：TODO 那条正是先建的分布甲
    assertEquals(a.id, service.list().first { it.status == TaskStatus.TODO }.id)
  }

  @AfterAll fun cleanup(): Unit = runBlocking { repo.clearAll() }
}
