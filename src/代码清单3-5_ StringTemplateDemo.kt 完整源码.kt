package vars

fun main() {
    val userName = "小林"
    val orderCount = 12
    val unitPrice = 46.5

    // 改造前：加号拼接——加号与引号交错，字符串片段与变量界限模糊；
    // 换行续写时 + 号必须留在上一行行尾，挂错位置直接编译失败
    val summaryOld = "用户 " + userName +
        " 本月下单 " + orderCount + " 单，合计消费 " + unitPrice * orderCount + " 元"

    // 改造后：字符串模板——变量用 $ 直嵌，表达式用 ${} 包住，拼接噪音归零
    val summaryNew = "用户 $userName 本月下单 $orderCount 单，合计消费 ${unitPrice * orderCount} 元"

    println(summaryOld)
    println(summaryNew)
}