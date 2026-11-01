package functions

// 支付处理：结果不靠返回值表达，而是把「成功后做什么」与「失败后做什么」
// 作为两个函数类型参数交出去，本函数只负责判定走哪条路（对照清单6-1 的三份复制）
fun processPayment(
    orderId: String,
    amount: Double,
    balance: Double,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    // 判定必须确定性：应付金额与账户余额比大小，结果可推演可复测，不掺任何随机
    if (amount <= balance) {
        onSuccess("支付成功：订单 $orderId 扣款 ￥${"%.2f".format(amount)}，余额剩 ￥${"%.2f".format(balance - amount)}")
    } else {
        onFailure("余额不足：订单 $orderId 应付 ￥${"%.2f".format(amount)}，账户仅剩 ￥${"%.2f".format(balance)}")
    }
}

fun main() {
    // 第一发：成功分支。两个回调都具名传 lambda，谁管成功谁管失败一眼可辨
    processPayment(
        orderId = "ORDER-2026-001",
        amount = 129.0,
        balance = 500.0,
        onSuccess = { receipt -> println(receipt) },
        onFailure = { receipt -> println(receipt) }
    )

    // 第二发：故意失败的教材性演示。300 元应付撞上 88 元余额，必走 onFailure，
    // 回执文案就是下面的第二行输出
    processPayment(
        orderId = "ORDER-2026-002",
        amount = 300.0,
        balance = 88.0,
        onSuccess = { receipt -> println(receipt) },
        onFailure = { receipt -> println(receipt) }
    )

    // 第三发：尾随 lambda 只对最后一个参数 onFailure 生效，
    // 前面的 onSuccess 仍须具名写在括号内；本发余额充足，尾随的那个回调不会触发
    processPayment(orderId = "ORDER-2026-003", amount = 45.0, balance = 100.0, onSuccess = { receipt ->
        println(receipt)
    }) { receipt ->
        println("失败回执：$receipt")
    }
}