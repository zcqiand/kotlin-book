package demo

// 消息批次：转发逻辑里成批出现的消息集合
class MessageBatch(val items: List<Message>) {
    // plus 对应 +：两个批次合并成一个新批次，原批次不动
    operator fun plus(other: MessageBatch): MessageBatch =
        MessageBatch(items + other.items)

    // get 对应下标 []：按位置取消息
    operator fun get(index: Int): Message = items[index]
}

fun runBatch() {
    val local = MessageBatch(listOf(TextMessage("T-1", "会议改到下午")))
    val remote = MessageBatch(
        listOf(TextMessage("T-2", "周报已提交"), SystemNotice("N-1", 500)),
    )
    // + 背后就是 plus：这一行与 local.plus(remote) 完全等价
    val all = local + remote
    println("合并后 ${all.items.size} 条：${all.items.map { it.id }}")
    // 下标语法背后就是 get
    println("第 2 条=${all[1].id}")
}