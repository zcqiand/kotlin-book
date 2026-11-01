/** 回滚分支：事务内抛异常 → 整块不落库；正常分支：无异常 → 落库。 */
private suspend fun txDemo() {
  val db = connectVerify()
  suspendTransaction(db) { BooksTable.deleteAll() }

  runCatching {
    suspendTransaction(db) {
      BooksTable.insert {
        it[title] = "本该回滚的书"
        it[author] = "佚名"
        it[year] = 1900
      }
      error("事务中途炸了：这行不该留下来")
    }
  }
  val afterRollback = suspendTransaction(db) { BooksTable.selectAll().toList().size }
  println("回滚分支：事务内写入后抛异常，表内行数 = $afterRollback（期望 0）")

  suspendTransaction(db) {
    BooksTable.insert {
      it[title] = "正常落库的书"
      it[author] = "林牧"
      it[year] = 2024
    }
  }
  val afterCommit = suspendTransaction(db) { BooksTable.selectAll().toList().size }
  println("提交分支：无异常正常退出事务，表内行数 = $afterCommit（期望 1）")
}