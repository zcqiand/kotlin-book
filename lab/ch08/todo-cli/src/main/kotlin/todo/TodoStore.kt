package todo

import java.io.File

// 待办只承载数据：data class 的生成成员第 11 章正式主讲，
// 这里先借用，done 落库时改一条数据靠 copy 一行完成
data class Todo(val id: Int, val text: String, val done: Boolean)

// 数据层：文件的读写细节全收在这里，
// 命令层只认 loadAll/add/markDone/remove 四个动词，不关心文件长什么样
class TodoStore(private val filePath: String) {

    private val file = File(filePath)

    // 读：文件不存在（第一次运行）不算错误，交回空列表，
    // 空态在数据层就被安排了去处，口径来自第 4 章
    fun loadAll(): List<Todo> =
        if (!file.exists()) {
            emptyList()
        } else {
            file.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull(::parseLine)   // 装不回三个字段的行（空行、残行）在此弃掉
        }

    // 增：新编号取「现有最大编号加一」，空库从 1 起算；
    // 追加只碰文件尾部，前面已落盘的行原封不动
    fun add(text: String): Todo {
        val nextId = (loadAll().maxOfOrNull { it.id } ?: 0) + 1
        val todo = Todo(nextId, text, done = false)
        file.appendText(toLine(todo) + System.lineSeparator())
        return todo
    }

    // 改：编号不存在交回 false，提示什么由命令层决定；改完全量重写
    fun markDone(id: Int): Boolean {
        val todos = loadAll()
        val target = todos.firstOrNull { it.id == id } ?: return false
        rewriteAll(todos.map { if (it.id == id) target.copy(done = true) else it })
        return true
    }

    // 删：过滤后条数没变，说明编号不存在
    fun remove(id: Int): Boolean {
        val todos = loadAll()
        val remaining = todos.filterNot { it.id == id }
        if (remaining.size == todos.size) return false
        rewriteAll(remaining)
        return true
    }

    private fun rewriteAll(todos: List<Todo>) {
        file.writeText(todos.joinToString("") { toLine(it) + System.lineSeparator() })
    }

    // 一条待办在文件里占一行，形如 {"id":2,"done":true,"text":"写周报"}
    private fun toLine(todo: Todo): String =
        "{\"id\":${todo.id},\"done\":${todo.done},\"text\":\"${todo.text}\"}"

    // 解析是写入的逆操作：沿同一套字段名反向截取；
    // 卷一没有序列化库，内容含英文双引号时会失手，正式方案第 22 章交接
    private fun parseLine(line: String): Todo? {
        val id = line.substringAfter("\"id\":").substringBefore(",").toIntOrNull() ?: return null
        val done = line.substringAfter("\"done\":").substringBefore(",").toBooleanStrictOrNull() ?: return null
        val text = line.substringAfter("\"text\":\"").removeSuffix("\"}")
        if (text.isEmpty()) return null
        return Todo(id, text, done)
    }
}
