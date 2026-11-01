package controlflow

// 改造前的运费计算：会员等级、金额门槛、偏远与否三个维度层层相乘，
// 写成三层嵌套后，每一条运费规则都藏在某个叶子分支里，改一处要在脑内先定位三层。
fun shippingFee(memberLevel: String, amount: Double, isRemote: Boolean): Double {
    // 顶层 var 承载结果：哪个分支最后覆写过它，返回值就是谁，
    // 读代码的人必须把整棵 if 树走完才能确定一笔订单的运费
    var fee = 0.0
    if (memberLevel == "GOLD") {
        if (amount >= 99) {
            if (isRemote) {
                fee = 6.0
            } else {
                fee = 0.0
            }
        } else {
            if (isRemote) {
                fee = 10.0
            } else {
                fee = 4.0
            }
        }
    } else if (memberLevel == "SILVER") {
        if (amount >= 99) {
            // 这是改造前埋的雷：偏远判断在同级其他分支一个不落，唯独这里漏了，
            // SILVER 满 99 的偏远订单只收基础运费——新版清单5-6 会在结构上修掉它
            fee = 4.0
        } else {
            if (isRemote) {
                fee = 12.0
            } else {
                fee = 6.0
            }
        }
    } else {
        if (amount >= 199) {
            if (isRemote) {
                fee = 12.0
            } else {
                fee = 6.0
            }
        } else {
            if (isRemote) {
                fee = 14.0
            } else {
                fee = 8.0
            }
        }
    }
    return fee
}

fun main() {
    // 六笔订单覆盖全部「等级 x 门槛 x 偏远」组合，第三笔专门踩那颗雷
    println("GOLD 满99 非偏远：${shippingFee("GOLD", 150.0, false)} 元")
    println("GOLD 未满99 偏远：${shippingFee("GOLD", 50.0, true)} 元")
    println("SILVER 满99 偏远：${shippingFee("SILVER", 150.0, true)} 元")
    println("SILVER 未满99 非偏远：${shippingFee("SILVER", 30.0, false)} 元")
    println("普通顾客 满199 非偏远：${shippingFee("NORMAL", 250.0, false)} 元")
    println("普通顾客 未满199 偏远：${shippingFee("NORMAL", 80.0, true)} 元")
}