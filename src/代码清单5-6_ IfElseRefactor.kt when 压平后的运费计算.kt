package controlflow

// 改造终版：带主语 when 把清单5-1 的「会员等级 + 金额门槛」两层压成一层，
// 每个分支用 if 表达式直接产出基础运费；偏远加收收敛到 return 处的一行，
// 只此一处、没有例外，清单5-1 那颗 SILVER 满 99 的雷在这种结构里无处可埋
fun shippingFee(memberLevel: String, amount: Double, isRemote: Boolean): Double {
    val baseFee = when (memberLevel) {
        "GOLD" -> if (amount >= 99) 0.0 else 4.0
        "SILVER" -> if (amount >= 99) 4.0 else 6.0
        else -> if (amount >= 199) 6.0 else 8.0
    }
    // 偏远加收只写这一次：第三笔订单由此从 4.0 修正为 4.0 + 6.0 = 10.0
    return baseFee + if (isRemote) 6.0 else 0.0
}

fun main() {
    // 与清单5-1 完全同一组六笔订单，逐行对照才能看出雷修在了哪里
    println("GOLD 满99 非偏远：${shippingFee("GOLD", 150.0, false)} 元")
    println("GOLD 未满99 偏远：${shippingFee("GOLD", 50.0, true)} 元")
    println("SILVER 满99 偏远：${shippingFee("SILVER", 150.0, true)} 元")
    println("SILVER 未满99 非偏远：${shippingFee("SILVER", 30.0, false)} 元")
    println("普通顾客 满199 非偏远：${shippingFee("NORMAL", 250.0, false)} 元")
    println("普通顾客 未满199 偏远：${shippingFee("NORMAL", 80.0, true)} 元")
}