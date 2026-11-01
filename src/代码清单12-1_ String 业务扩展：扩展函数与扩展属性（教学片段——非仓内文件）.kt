package demo

// 扩展函数：String 即接收者类型，函数体内 this 就是调用者那个字符串
fun String.maskPhone(): String =
    // 只有 11 位手机号才脱敏：中间四位打码；其余原样交还，防误伤短号
    if (length == 11) "${take(3)}****${takeLast(4)}" else this

// 扩展属性：只能现算（getter），不能存值；把「展示尾号」挂在 String 名下
val String.maskedTail: String
    get() = if (length >= 4) takeLast(4) else this

fun runStringExt() {
    val phone = "13812345678"
    val shortNo = "95588"
    println("脱敏=${phone.maskPhone()}，短号原样=${shortNo.maskPhone()}，短信尾号=${phone.maskedTail}")
}