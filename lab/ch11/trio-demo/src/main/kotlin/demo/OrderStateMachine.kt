package demo

// 密封类：子类型全部声明在同一个包内、编译期可枚举，
// 非法状态（没有对应子类型的状态）从根上进不了这个类型
sealed interface OrderState {
    // data object：单例子类型且 toString 干净地显示名字，适合无数据的「待处理」态
    data object Pending : OrderState
    data class Paid(val paidAt: String) : OrderState
    data class Shipped(val carrier: String) : OrderState
    data class Delivered(val receivedAt: String) : OrderState
    data class Cancelled(val reason: String) : OrderState
}

// when 作表达式且不写 else：编译器强制穷举全部子类型，
// 日后新增状态时，这里会编译报错提示补分支——穷举由编译器保证，不靠人记
fun process(state: OrderState): String = when (state) {
    OrderState.Pending -> "待支付：等待买家付款"
    is OrderState.Paid -> "已支付：支付时间 ${state.paidAt}，等待发货"
    is OrderState.Shipped -> "运输中：承运方 ${state.carrier}"
    is OrderState.Delivered -> "已签收：签收时间 ${state.receivedAt}"
    is OrderState.Cancelled -> "已取消：原因 ${state.reason}"
}

fun runOrderStateMachine() {
    // 依次喂三种状态：同一份 process 随实际子类型给出各自的文案
    val states = listOf(
        OrderState.Pending,
        OrderState.Paid(paidAt = "2026-10-31 09:30"),
        OrderState.Cancelled(reason = "超时未支付"),
    )
    for (state in states) {
        println(process(state))
    }
}
