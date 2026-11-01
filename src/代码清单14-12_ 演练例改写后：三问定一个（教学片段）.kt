package demo

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