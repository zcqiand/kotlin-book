package demo

fun prepare(userId: String, signature: NoticeDraft?): NoticeDraft {
    val temp = NoticeDraft()
    temp.title = "欢迎加入"
    temp.content = "你好，$userId"
    // 签名可空：先判空，再拼回去
    if (signature != null) {
        temp.content = temp.content + "｜签名：" + signature.content
    }
    println("准备完成：${temp.title}")
    return temp
}