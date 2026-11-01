package demo

// reified 实化：inline 函数体内可用 is T 与 T::class
inline fun <reified T : Message> List<Message>.ofType(): List<T> =
    filter { it is T }.map { it as T }

inline fun <reified T : Message> countOf(messages: List<Message>): Int {
    // T::class.simpleName 在普通泛型函数里拿不到：类型参数运行期早已被擦掉
    println("正在按 ${T::class.simpleName} 筛选")
    return messages.ofType<T>().size
}

fun runReified() {
    val inbox: List<Message> = listOf(
        TextMessage("T-1", "会议改到下午"),
        SystemNotice("N-1", 500),
        TextMessage("T-2", "周报已提交"),
    )
    println("文本消息=${countOf<TextMessage>(inbox)} 条")
    println("系统通知=${countOf<SystemNotice>(inbox)} 条")
}