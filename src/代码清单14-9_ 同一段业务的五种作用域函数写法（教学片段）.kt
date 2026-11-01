package demo

// 通知草稿：发送前要填标题、写正文、再汇总摘要
data class NoticeDraft(
    var title: String = "",
    var content: String = "",
)

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