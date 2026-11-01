package nulls

// 为什么用 data class：订单只承载数据，equals/copy 等由编译器生成，
// 让读者把注意力放在两个可空字段如何引爆 NPE，而不是淹没在样板代码里。
data class Order(val orderId: String, val couponCode: String?, val remark: String?)

// 隐患一：参数老实声明成可空，函数体却用 !! 硬断言非空——
// 未填优惠码是合法业务状态，调用方传 null 时，!! 所在行就地抛 NPE。
fun discountRate(couponCode: String?): Double {
    // 业务意图：以 SAVE 开头的优惠码享 85 折；但 null 在这里不是"不匹配"，而是直接崩溃
    return if (couponCode!!.startsWith("SAVE")) 0.85 else 1.0
}

// 隐患二：作者只想防"空串显示为无"，顺手补了个 !!，
// 结果把 null（顾客没留备注）也拦在了 ifEmpty 之外——防了空串，漏了 null。
fun remarkText(remark: String?): String {
    // ifEmpty 只治空串；前面的 !! 让 null 根本走不到 ifEmpty 这一步
    return remark!!.ifEmpty { "无" }
}

fun settlementSummary(order: Order, amount: Double): String {
    // 先取备注再算折扣：求值顺序决定了第二单的崩溃帧落在隐患二上
    val remark = remarkText(order.remark)
    val payable = amount * discountRate(order.couponCode)
    return "订单 ${order.orderId} 应付 $payable 元（备注：$remark）"
}

fun main() {
    // 第一单：优惠码与备注齐全，两处 !! 都侥幸过关
    println(settlementSummary(Order("A-1024", "SAVE2026", "尽快发货"), 100.0))
    // 第二单：两个字段均为 null——合法业务状态，却成了 !! 的死刑判决
    println(settlementSummary(Order("A-1031", null, null), 100.0))
}