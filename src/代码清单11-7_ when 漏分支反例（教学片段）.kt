package demo

// 反例：与清单 11-5 的正例同构，只是漏掉 Cancelled 一个分支——
// when 作为表达式且不写 else 时，Kotlin 强制穷举密封类的全部子类型，
// 这份代码必须在编译期失败，运行不到
fun brokenProcess(state: OrderState): String = when (state) {
    OrderState.Pending -> "待支付"
    is OrderState.Paid -> "已支付 ${state.paidAt}"
    is OrderState.Shipped -> "运输中 ${state.carrier}"
    is OrderState.Delivered -> "已签收 ${state.receivedAt}"
}