package nulls

data class Order(val orderId: String, val couponCode: String?, val remark: String?)

// ?. 配 == true：null 不再触发崩溃，只是让条件不成立而落入 else 按原价结算——
// 业务语义（没填优惠码 = 不打折）与代码语义第一次完全对齐。
fun discountRate(couponCode: String?): Double =
    if (couponCode?.startsWith("SAVE") == true) 0.85 else 1.0

// ?.ifEmpty 兜住空串，?: 兜住 null：两级兜底汇入同一个文案"无"，
// 因为对读订单的人来说，"没写备注"和"写了空白"本来就是同一件事。
fun remarkText(remark: String?): String = remark?.ifEmpty { "无" } ?: "无"

fun settlementSummary(order: Order, amount: Double): String {
    // 与清单4-1 相同的求值顺序，方便读者逐帧对照改造前后的栈帧差异
    val remark = remarkText(order.remark)
    val payable = amount * discountRate(order.couponCode)
    return "订单 ${order.orderId} 应付 $payable 元（备注：$remark）"
}

fun main() {
    println(settlementSummary(Order("A-1024", "SAVE2026", "尽快发货"), 100.0))
    println(settlementSummary(Order("A-1031", null, null), 100.0))
}