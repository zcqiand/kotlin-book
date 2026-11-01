package demo

// object 声明：类定义与唯一实例一体成形，
// 全局只有这一份，发号器这类「状态必须有且只有一份」的组件正合适
object IdCounter {
    private var current = 0

    fun next(): Int = ++current
}

class TaskItem(val id: Int, val title: String) {
    companion object {
        // 编译期常量：不依赖实例就能取，工厂与常量都挂在类名上调用
        const val DEFAULT_TITLE = "未命名任务"

        // 伴生工厂：把「一行 CSV 解析成对象」的构造细节收进类内，
        // 调用方写 TaskItem.fromRow(...) 即可，不必知道拆分规则
        fun fromRow(row: String): TaskItem {
            val (idText, title) = row.split(",", limit = 2)
            return TaskItem(id = idText.toInt(), title = title.ifBlank { DEFAULT_TITLE })
        }
    }
}

fun runObjectDemo() {
    // 两次「取单例」拿到的是同一个对象：=== 比较引用，为真即证明全局仅此一份
    val first = IdCounter
    val second = IdCounter
    // 两次发号连续自增：状态在唯一实例上累积，不存在第二份副本
    println("两次引用相同=${first === second}，连续发号=${first.next()}、${second.next()}")
    val item = TaskItem.fromRow("7,补交报销单")
    println("伴生工厂产出：id=${item.id}，title=${item.title}，默认标题=${TaskItem.DEFAULT_TITLE}")
}