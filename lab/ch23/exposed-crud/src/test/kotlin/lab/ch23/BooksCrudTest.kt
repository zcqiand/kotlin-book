package lab.ch23

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.deleteAll
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/** 集成测试：对真实 PostgreSQL（127.0.0.1:54321）跑增删改查断言。 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BooksCrudTest {
  private val db = connectVerify()

  @BeforeAll
  fun setup(): Unit = runBlocking {
    suspendTransaction(db) { SchemaUtils.create(BooksTable) }
    suspendTransaction(db) { BooksTable.deleteAll() }
  }

  @Test
  fun `insert 后能按条件查出`() = runBlocking {
    suspendTransaction(db) {
      BooksTable.insert {
        it[title] = "Exposed 入门"
        it[author] = "林牧"
        it[year] = 2026
      }
    }
    val rows = suspendTransaction(db) {
      BooksTable.selectAll().where { BooksTable.year eq 2026 }.toList()
    }
    assertEquals(1, rows.size)
    assertEquals("Exposed 入门", rows[0][BooksTable.title])
    assertEquals("林牧", rows[0][BooksTable.author])
  }

  @Test
  fun `update 改得动 delete 删得掉`() = runBlocking {
    suspendTransaction(db) {
      BooksTable.insert {
        it[title] = "待改的书"
        it[author] = "沈砚"
        it[year] = 2023
      }
    }
    val updated = suspendTransaction(db) {
      BooksTable.update({ BooksTable.title eq "待改的书" }) { it[year] = 2025 }
    }
    assertEquals(1, updated)

    val deleted = suspendTransaction(db) {
      BooksTable.deleteWhere { BooksTable.title eq "待改的书" }
    }
    assertEquals(1, deleted)
    val rows = suspendTransaction(db) {
      BooksTable.selectAll().where { BooksTable.title eq "待改的书" }.toList()
    }
    assertTrue(rows.isEmpty())
  }

  @Test
  fun `唯一约束违反会回滚整块写入`() = runBlocking {
    suspendTransaction(db) {
      BooksTable.insert {
        it[title] = "同题两行不可能"
        it[author] = "周临"
        it[year] = 2024
      }
    }
    // 整块事务里插两行同题 → 违反 books_title_uq → 抛异常
    assertThrows(Exception::class.java) {
      runBlocking {
        suspendTransaction(db) {
          BooksTable.insert {
            it[title] = "同题两行不可能"
            it[author] = "谁都不是"
            it[year] = 2025
          }
        }
      }
    }
    // 回滚后表里只有第一行，且没有「谁都不是」
    val authors = suspendTransaction(db) {
      BooksTable.selectAll().where { BooksTable.title eq "同题两行不可能" }
          .toList().map { it[BooksTable.author] }
    }
    assertEquals(listOf("周临"), authors)
  }
}
