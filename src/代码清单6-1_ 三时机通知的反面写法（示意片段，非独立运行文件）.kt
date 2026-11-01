// 反面教材示意片段，非独立运行文件：组装文案、调用发送通道、记一条日志，
// 这套骨架被原样复制三份，三份之间变的只有文案、前缀与事件名。
// 痛点在改动结算：文案改一个词要动三处，日志格式调整也要动三处，
// 漏改任何一份，三个时机的通知口径当场不一致，而编译器对此毫无察觉。
fun sendOrderConfirmed(orderId: String, amount: Double) {
    if (orderId.isBlank()) return
    val title = "订单通知"
    val body = "您的订单 $orderId 已下单成功，应付 ￥${"%.2f".format(amount)}"
    println("【下单成功】[$title] $body")
    println("[通知日志] channel=sms event=ORDER_CONFIRMED order=$orderId")
}

fun sendPaymentReminder(orderId: String, amount: Double) {
    if (orderId.isBlank()) return
    val title = "订单通知"
    val body = "您的订单 $orderId 已超过 30 分钟未支付，应付 ￥${"%.2f".format(amount)}"
    println("【支付提醒】[$title] $body")
    println("[通知日志] channel=sms event=PAYMENT_REMINDER order=$orderId")
}

fun sendShippedNotice(orderId: String, carrier: String) {
    if (orderId.isBlank()) return
    val title = "订单通知"
    val body = "您的订单 $orderId 已由 $carrier 揽收发出"
    println("【发货完成】[$title] $body")
    println("[通知日志] channel=sms event=SHIPPED order=$orderId")
}