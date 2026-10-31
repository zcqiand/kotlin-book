package demo

// 清单13-3：泛型函数与上界约束——T 必须是 Message 或其子类型，
// 编译器因此保证循环体里拿到的每个元素都有 id 可取

// 上界约束：T 必须是 Message 或其子类型，才能安全取 id
fun <T : Message> List<T>.digest(): String =
    joinToString(prefix = "[", postfix = "]") { it.id }

fun runDigest() {
    val texts = listOf(TextMessage("T-1", "会议改到下午"), TextMessage("T-2", "周报已提交"))
    val notices = listOf(SystemNotice("N-1", 500), SystemNotice("N-2", 404))
    // 同一个 digest 分别套在两个不同元素类型的列表上：T 每次调用各自推断
    println("文本消息摘要=${texts.digest()}")
    println("系统通知摘要=${notices.digest()}")
}
