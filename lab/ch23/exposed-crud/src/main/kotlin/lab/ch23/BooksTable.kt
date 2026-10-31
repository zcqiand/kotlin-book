package lab.ch23

import org.jetbrains.exposed.v1.core.Table

/**
 * books 表对象：Exposed 1.x 的表即「object 单例 + 列声明」。
 * id 用 integer().autoIncrement() 映射 PostgreSQL 的 SERIAL 语义；
 * title 加唯一索引，为第 23 章反例（约束违反）预留触发点。
 */
object BooksTable : Table("books") {
  val id = integer("id").autoIncrement()
  val title = varchar("title", 128).uniqueIndex("books_title_uq")
  val author = varchar("author", 64)
  val year = integer("year")

  override val primaryKey = PrimaryKey(id)
}
