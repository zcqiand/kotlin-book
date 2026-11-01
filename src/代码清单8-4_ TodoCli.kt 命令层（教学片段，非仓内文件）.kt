package todo

// 用法提示：三引号内换行原样保留
private const val USAGE = """用法：todo-cli <命令> [参数]
  add <内容>   新增一条待办
  list         列出全部待办并统计
  done <编号>  标记指定待办为已完成
  rm <编号>    删除指定待办"""

// 命令层：解析参数、分派命令、打印回执，数据的事全部交给 TodoStore
fun main(args: Array<String>) {
    // 空参兜底：getOrNull 先问「第 1 个参数在吗」，问不出就打印用法收工
    val command = args.getOrNull(0)
    if (command == null) {
        println(USAGE)
        return
    }

    val store = TodoStore("todos.json")
    // 带主语 when（第 5 章形态一）：四操作各占一个分支，else 兜住未知命令
    when (command) {
        "add" -> {
            // 第 2 位起的参数全部收编，多词内容拼回一句
            val text = args.drop(1).joinToString(" ")
            if (text.isBlank()) {
                println("用法：add <内容>")
            } else {
                val todo = store.add(text)
                println("已添加 #${todo.id}：${todo.text}")
            }
        }
        "list" -> {
            val todos = store.loadAll()
            if (todos.isEmpty()) {
                println("清单还是空的，先用 add 添加一条试试")
            } else {
                // 行为当参数：一颗就地 Lambda 递给标准库的 forEach（第 6、7 章同一条路）
                todos.forEach { todo ->
                    val mark = if (todo.done) "[x]" else "[ ]"   // if 表达式直接产出标记
                    println("$mark #${todo.id} ${todo.text}")
                }
                // 第 7 章预告的岗位：filter 数已完成，fold 把未完成折成一个数
                val doneCount = todos.filter { it.done }.size
                val openCount = todos.fold(0) { acc, todo -> if (!todo.done) acc + 1 else acc }
                println("共 ${todos.size} 条：未完成 $openCount，已完成 $doneCount")
            }
        }
        "done" -> {
            // 缺参（getOrNull 交回 null）与非数字（toIntOrNull 交回 null）
            // 都被 Elvis 接住转成用法提示，全程不必出现 !!
            val id = args.getOrNull(1)?.toIntOrNull()
            if (id == null) {
                println("用法：done <编号>")
            } else if (store.markDone(id)) {
                println("已完成 #$id")
            } else {
                println("找不到 #$id，清单未改动")
            }
        }
        "rm" -> {
            val id = args.getOrNull(1)?.toIntOrNull()
            if (id == null) {
                println("用法：rm <编号>")
            } else if (store.remove(id)) {
                println("已删除 #$id")
            } else {
                println("找不到 #$id，清单未改动")
            }
        }
        else -> println("未知命令：$command（可用：add / list / done / rm）")
    }
}