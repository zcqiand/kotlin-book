println("== update (2020 → 2022) ==")
val updated = BooksTable.update({ BooksTable.title eq "数据访问模式" }) {
  it[year] = 2022
}
println("更新 $updated 行")

println("== delete (Ktor 服务端开发) ==")
val deleted = BooksTable.deleteWhere { BooksTable.title eq "Ktor 服务端开发" }
println("删除 $deleted 行")
println("剩余 ${BooksTable.selectAll().toList().size} 行: " +
    BooksTable.selectAll().toList().joinToString { it[BooksTable.title] })