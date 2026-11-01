package demo

class MessageBatch(val items: List<Message>) {
    // plus 对应 +：两个批次合并成一个新批次，原批次不动
    operator fun plus(other: MessageBatch): MessageBatch =
        MessageBatch(items + other.items)

    // get 对应下标 []：按位置取消息
    operator fun get(index: Int): Message = items[index]

    // infix 成员函数：单参数，调用时可省点号和括号
    infix fun mergedWith(other: MessageBatch): MessageBatch =
        MessageBatch(items + other.items)
}

fun runInfix() {
    val local = MessageBatch(listOf(TextMessage("T-3", "周报已提交")))
    val remote = MessageBatch(listOf(SystemNotice("N-2", 404)))
    // 中缀调用：省掉点号和括号，读起来像一句话
    val merged = local mergedWith remote
    println("中缀合并后 ${merged.items.size} 条：${merged.items.map { it.id }}")
}