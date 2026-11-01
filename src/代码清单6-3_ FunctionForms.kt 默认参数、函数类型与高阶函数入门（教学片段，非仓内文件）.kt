package functions

// 价格格式化：两个默认参数把「约定值」收敛到签名一处，
// 清单6-2 里 Java 三个重载接力才能模拟的事，这里一个函数说完
fun formatPrice(amount: Double, symbol: String = "￥", decimals: Int = 2): String {
    // 字符串模板回指第 3 章："%.${decimals}f" 先把精度拼进格式串，再交给 format 格式化
    return symbol + "%.${decimals}f".format(amount)
}

// 函数类型变量：订单号统一转大写这段「行为」存进变量，
// 将来要换成加前缀、转小写，换的是变量的值，调用处一行不动；
// 花括号里的 it 是单参数 Lambda 的隐式参数名，无须自己另起名
val toOrderCode: (String) -> String = { it.uppercase() }

// 可空函数类型：审计钩子默认没挂。类型整体可空（注意外层括号），
// 表示「这个行为可能还没有」，与第 4 章可空类型是同一套心智
var auditHook: ((String) -> Unit)? = null

// 高阶函数入门：循环骨架写一次，每一步做什么由调用方注入，
// step 参数的类型 (Int) -> Unit 读作「给一个步数，无返回值的一段行为」
fun eachStep(times: Int, step: (Int) -> Unit) {
    for (stepIndex in 0 until times) {
        step(stepIndex)
    }
}

fun main() {
    // 调用一：symbol 与 decimals 全部走默认值，日常调用里最常见的形态之一
    println(formatPrice(129.0))
    // 调用二：decimals 保持默认但 symbol 要改。默认值按位置排在中间，
    // 跳过它改后面的参数必须具名，否则 "$" 会被按位置塞进 decimals 编译报错
    println(formatPrice(amount = 99.5, symbol = "$"))
    // 调用三：全具名，实参与形参一一对应，参数一多时这种写法最不易出错——每个实参都有名可查
    println(formatPrice(amount = 8.0, symbol = "€", decimals = 3))

    // 函数类型变量照普通变量一样参与运算与调用
    println(toOrderCode("order-a1b2c3"))

    // 钩子未挂时 ?.invoke 整句短路（安全调用回指第 4 章），null 不会引爆异常
    auditHook?.invoke("ORDER-1001")
    println("第一发审计：钩子未挂，上面无输出属预期")

    // 挂上钩子再发同一单号：这次行为真正被执行
    auditHook = { orderId -> println("[审计] 订单 $orderId 已进入风控留痕") }
    auditHook?.invoke("ORDER-1001")

    // 尾随 lambda：唯一的函数类型参数挪出括号，调用处读起来像一段流程描述
    eachStep(times = 3) { step ->
        println("打包进度 第${step + 1}步完成")
    }
}