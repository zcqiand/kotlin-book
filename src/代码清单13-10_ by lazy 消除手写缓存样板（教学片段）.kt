package demo

// 样板版：手写缓存字段 + 每次判空
class AttachmentManual(val name: String) {
    private var cached: String? = null
    val preview: String
        get() {
            // 先取局部变量再判空：智能转换认局部 val，不认可变字段
            val existing = cached
            if (existing != null) return existing
            val computed = "预览：${name} 摘要"
            cached = computed
            return computed
        }
}

// 委托版：by lazy 一行交代，线程安全、首次读取才算
class AttachmentLazy(val name: String) {
    val preview: String by lazy {
        println("（现算一次：读取 ${name} 的内容生成预览）")
        "预览：${name} 摘要"
    }
}

fun runLazy() {
    val manual = AttachmentManual("report.pdf")
    val lazyVersion = AttachmentLazy("report.pdf")
    // 到这里两个对象都只存了名字，preview 谁都还没算过
    println("创建完成，两者都还没算过 preview")
    println("手写样板版：${manual.preview}")
    // 第二次读取直接命中缓存，「现算一次」提示不再出现
    println("委托版第一次：${lazyVersion.preview}")
    println("委托版第二次：${lazyVersion.preview}")
}