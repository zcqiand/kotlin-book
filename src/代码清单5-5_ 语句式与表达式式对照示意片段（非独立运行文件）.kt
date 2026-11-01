// 对照示意片段，非独立运行文件——同一取文案逻辑的两种写法并排放，请对照阅读
fun statementForm(status: Int): String {
    // 语句式：先给 var 一个兜底值，再让每个分支轮流覆写——
    // 漏写某个状态编译器不会吭声，错的全靠运行时才暴露
    var text = "状态未知"
    if (status == 0) {
        text = "已下单"
    } else if (status == 1) {
        text = "打包中"
    } else if (status == 2 || status == 3) {
        text = "运输中"
    } else if (status == 4) {
        text = "已签收"
    }
    return text
}

fun expressionForm(status: Int): String {
    // 表达式式：when 自己产出值，val 一次接住，else 只兜真正的未知状态；
    // 缺 else 编译器直接报错——但 Int 主语下有了 else，漏写的具体分支会被兜进 else 拿到「状态未知」；
    // 要连「漏了哪个值」都拦住，得等第 11 章密封类的穷举检查
    val label = when (status) {
        0 -> "已下单"
        1 -> "打包中"
        2, 3 -> "运输中"
        4 -> "已签收"
        else -> "状态未知"
    }
    return label
}