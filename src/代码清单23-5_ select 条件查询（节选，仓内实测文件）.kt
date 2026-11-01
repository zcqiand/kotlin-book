println("== select (year >= 2023) ==")
BooksTable.selectAll()
    .where { BooksTable.year greaterEq 2023 }
    .toList()
    .forEach { row ->
      println("  ${row[BooksTable.id]}: ${row[BooksTable.title]} / ${row[BooksTable.author]} / ${row[BooksTable.year]}")
    }