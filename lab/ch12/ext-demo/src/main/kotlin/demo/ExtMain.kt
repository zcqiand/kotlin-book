package demo

// 清单12-1：扩展函数与扩展属性——不碰 String 源码一行，从外部给它长出站内业务方法

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

// 清单12-3：静态解析——扩展按变量的声明类型分派，与第 10 章成员函数的运行期分派相对
// 订单状态沿第 11 章的密封类口径，这里只取两个子类型
sealed interface OrderState {
    data object Pending : OrderState
    data class Cancelled(val reason: String) : OrderState
}

// 同名扩展一：声明类型 OrderState 能看到的版本
fun OrderState.tag(): String = "OrderState 扩展：全部状态通用"

// 同名扩展二：只有声明类型精确到 Cancelled 才看得到
fun OrderState.Cancelled.tag(): String = "Cancelled 扩展：仅已取消可见"

// 同名成员与扩展并存：成员胜出
class Channel {
    fun tag(): String = "成员函数"
}

// 与 Channel.tag() 同名的扩展：编译能过，但调用永远选中成员
fun Channel.tag(): String = "扩展函数"

fun runStaticDispatch() {
    val state: OrderState = OrderState.Cancelled("超时未支付")
    // 声明类型是 OrderState：即便此刻装着 Cancelled，选中的仍是 OrderState 上的扩展
    println("声明 OrderState、实际 Cancelled：${state.tag()}")
    val cancelled = OrderState.Cancelled("超时未支付")
    // 声明类型精确到 Cancelled，才会选中 Cancelled 上的那份扩展
    println("声明 Cancelled：${cancelled.tag()}")
    val channel = Channel()
    // 同名成员与扩展并存：编译器选成员
    println("成员与扩展同名：${channel.tag()}")
}

// 入口分发：按第一个命令行参数路由到对应演示，
// 让一章的多个清单共用一个 Gradle 工程、一个 run 任务，与 ch11 trio-demo 同构
fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "string" -> runStringExt()
        "resolve" -> runStaticDispatch()
        // 未带参数时直接报错退出：宁可显式失败，也不静默跑错演示
        else -> error("用法：gradle run --args=\"string|resolve\"")
    }
}
