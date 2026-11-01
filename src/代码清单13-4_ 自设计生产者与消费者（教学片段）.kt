package demo

// 生产者：只产出 T，声明 out——MessageSource<SystemNotice> 也是 MessageSource<Message>
class MessageSource<out T>(private val items: List<T>) {
    fun next(): T? = items.firstOrNull()
}

// 消费者：只消费 T，声明 in——MessageSink<Message> 可当 MessageSink<TextMessage> 用
// received 是私有字段，不进公共 API，型变检查不拦它收 T
class MessageSink<in T> {
    private val received = mutableListOf<T>()
    fun receive(item: T) {
        received.add(item)
    }
    fun size(): Int = received.size
}

fun runVariance() {
    val notices: MessageSource<SystemNotice> = MessageSource(listOf(SystemNotice("N-1", 500)))
    // 协变方向：子类型实参的源可以交给基类型实参的引用
    val anySource: MessageSource<Message> = notices
    println("生产者协变赋值成功：${anySource.next()?.id}")
    // 逆变方向：能吃下任何 Message 的槽，当然能当只吃文本消息的槽用
    val messageSink: MessageSink<Message> = MessageSink()
    val textSink: MessageSink<TextMessage> = messageSink
    textSink.receive(TextMessage("T-1", "你好"))
    println("消费者逆变赋值成功：收到 ${textSink.size()} 条文本消息")
}