package demo

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