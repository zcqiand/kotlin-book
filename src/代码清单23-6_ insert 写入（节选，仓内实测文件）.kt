val kotlinId = BooksTable.insert {
  it[title] = "Kotlin 实战"
  it[author] = "林牧"
  it[year] = 2024
} get BooksTable.id
val ktorId = BooksTable.insert {
  it[title] = "Ktor 服务端开发"
  it[author] = "沈砚"
  it[year] = 2025
} get BooksTable.id
BooksTable.insert {
  it[title] = "数据访问模式"
  it[author] = "周临"
  it[year] = 2020
}
println("写入 3 行，自增 id: $kotlinId, $ktorId")