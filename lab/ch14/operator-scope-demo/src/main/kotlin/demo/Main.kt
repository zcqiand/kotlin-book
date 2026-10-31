package demo

// 站内消息模型：与第 11-13 章口径一致
sealed class Message {
    abstract val id: String
}

class TextMessage(override val id: String, val content: String) : Message()
class SystemNotice(override val id: String, val code: Int) : Message()

// 消息批次：转发逻辑里成批出现的消息集合
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

// 通知草稿：发送前要填标题、写正文、再汇总摘要
data class NoticeDraft(
    var title: String = "",
    var content: String = "",
)

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

fun runInfix() {
    val local = MessageBatch(listOf(TextMessage("T-3", "周报已提交")))
    val remote = MessageBatch(listOf(SystemNotice("N-2", 404)))
    // 中缀调用：省掉点号和括号，读起来像一句话
    val merged = local mergedWith remote
    println("中缀合并后 ${merged.items.size} 条：${merged.items.map { it.id }}")
}

fun runScopes() {
    // apply：块里都是对 this 的赋值，要的是配置好的对象本身
    val draft = NoticeDraft().apply {
        title = "系统维护"
        content = "今晚 22:00 升级，期间暂停收发"
    }
    // with：对同一个对象连做几步读取，要的是算出来的结果
    val summary = with(draft) { "${title}｜$content" }
    println("摘要=$summary")
    // also：借用 it 记一条旁路日志，返回的还是 draft 本身
    val logged = draft.also { println("草稿就绪：${it.title}") }
    println("also 返回的还是原对象=${logged === draft}")
    // let：可空草稿走安全调用链，非空才进块
    val inbox: NoticeDraft? = draft
    inbox?.let { println("let 收到：${it.content}") }
    // run：对对象做一段计算并交回结果
    val length = draft.run { title.length + content.length }
    println("run 算得总字数=$length")
}

// 演练例（改写后）：三问定一个——apply 配置对象、let 处理可空、also 记旁路日志
fun prepare(userId: String, signature: NoticeDraft?): NoticeDraft =
    NoticeDraft().apply {
        title = "欢迎加入"
        content = "你好，$userId"
        // 可空签名按第三问走 let：非空才拼接
        signature?.let { content = "${content}｜签名：${it.content}" }
    }.also { println("准备完成：${it.title}") }

fun runDrill() {
    val withSign = prepare("U-1", NoticeDraft(content = "研发团队"))
    println("带签名=${withSign.content}")
    val withoutSign = prepare("U-2", null)
    println("不带签名=${withoutSign.content}")
}

fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "batch" -> runBatch()
        "infix" -> runInfix()
        "scopes" -> runScopes()
        "drill" -> runDrill()
        else -> println("用法：batch | infix | scopes | drill")
    }
}
