package collections

// 订单只装数据：data class 自动生成 equals/copy 等样板，
// 后面验证「两版一致」时直接用 == 比较整张列表，靠的就是它
data class Order(
    val orderId: String,
    val channel: String,   // 下单渠道：app / web / mini
    val amount: Double,
    val paid: Boolean,
)

// 数据写死而非随机生成：教学代码的第一要务是「跑一百遍输出都一样」，
// 读者对着清单核对输出行时不需要运气
val orders: List<Order> = listOf(
    Order("O-1001", "app", 328.00, paid = true),
    Order("O-1002", "web", 89.90, paid = true),
    Order("O-1003", "mini", 45.00, paid = false),
    Order("O-1004", "app", 599.00, paid = true),   // 第一笔「大额且已支付」，给 first 命中
    Order("O-1005", "web", 129.00, paid = false),
    Order("O-1006", "mini", 268.50, paid = true),
    Order("O-1007", "app", 79.99, paid = true),
    Order("O-1008", "web", 688.00, paid = false),  // 金额过线但未支付：first 的条件必须两项同时成立
    Order("O-1009", "mini", 156.00, paid = true),
)

fun main() {
    // ---------- 1. filter：挑出已支付订单 ----------
    val paidByOperator = orders.filter { it.paid }
    println("1) 已支付订单笔数: ${paidByOperator.size}")
    val paidByLoop = mutableListOf<Order>()
    for (order in orders) {
        if (order.paid) paidByLoop.add(order)
    }
    println("   [验证] filter 两版一致: ${paidByOperator == paidByLoop}")

    // ---------- 2. map：已支付订单的金额列表 ----------
    val amountsByOperator = paidByOperator.map { it.amount }
    println("2) 已支付订单金额列表: $amountsByOperator")
    val amountsByLoop = mutableListOf<Double>()
    for (order in paidByOperator) {
        amountsByLoop.add(order.amount)
    }
    println("   [验证] map 两版一致: ${amountsByOperator == amountsByLoop}")

    // ---------- 3. fold：已支付总金额 ----------
    // 累加从 0.0 起步、按列表顺序逐个加上去——for 版刻意保持同一顺序，
    // 否则浮点求和的舍入顺序不同，两个版本可能差在最后一 bit 上
    val totalByOperator = paidByOperator.fold(0.0) { acc, order -> acc + order.amount }
    println("3) 已支付总金额: ${"%.2f".format(totalByOperator)} 元")
    var totalByLoop = 0.0
    for (order in paidByOperator) {
        totalByLoop += order.amount
    }
    println("   [验证] fold 两版一致: ${totalByOperator == totalByLoop}")

    // ---------- 4. reduce：最大单笔金额 ----------
    // fold 与 reduce 只差一件事：reduce 的初值取第一个元素，
    // 所以它适合「在同类元素里归并出最大/最小/拼接」这类没有天然零值的场景
    val maxByOperator = amountsByOperator.reduce { acc, amount -> if (amount > acc) amount else acc }
    println("4) 最大单笔金额: ${"%.2f".format(maxByOperator)} 元")
    var maxByLoop = amountsByOperator.first()
    for (amount in amountsByOperator.drop(1)) {
        if (amount > maxByLoop) maxByLoop = amount
    }
    println("   [验证] reduce 两版一致: ${maxByOperator == maxByLoop}")

    // ---------- 5. groupBy：各渠道已支付笔数 ----------
    // groupBy 先把元素按渠道分桶，mapValues 再对每桶求计数——
    // 「分组 + 每组统计」两步走，比一个循环里塞三种逻辑更好改
    val countByChannelByOperator =
        paidByOperator.groupBy { it.channel }.mapValues { (_, channelOrders) -> channelOrders.size }
    println("5) 各渠道已支付笔数: $countByChannelByOperator")
    val countByChannelByLoop = mutableMapOf<String, Int>()
    for (order in paidByOperator) {
        // Elvis 写法：键不存在按 0 起算再写回，和 groupBy 内部的分桶动作一一对应
        countByChannelByLoop[order.channel] = (countByChannelByLoop[order.channel] ?: 0) + 1
    }
    println("   [验证] groupBy 两版一致: ${countByChannelByOperator == countByChannelByLoop}")

    // ---------- 6. first：第一笔大额已支付订单 ----------
    // first 找到第一个满足条件的元素就返回，不必扫完整表；
    // 数据量大且命中靠前时，这比先 filter 再取第一个省掉整张中间列表
    val bigOrderByOperator = orders.first { it.amount > 500.0 && it.paid }
    println("6) 第一笔大额已支付订单: ${bigOrderByOperator.orderId}" +
        "（${bigOrderByOperator.channel} 渠道，${"%.2f".format(bigOrderByOperator.amount)} 元）")
    var bigOrderByLoop: Order? = null
    for (order in orders) {
        if (order.amount > 500.0 && order.paid) {
            bigOrderByLoop = order
            break   // for 版要自己记得 break；first 把这件事收进了语义里
        }
    }
    println("   [验证] first 两版一致: ${bigOrderByOperator == bigOrderByLoop}")
}