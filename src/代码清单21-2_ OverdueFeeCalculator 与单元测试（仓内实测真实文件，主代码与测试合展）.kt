package library

/**
 * 图书逾期滞纳金计算器（衔接 ch15 图书馆域的借阅规则）。
 *
 * 规则：滞纳金 = 逾期天数 x 日费率；超过封顶值按封顶值收取。 封顶的意义：读者损失有上界，金额可预期；而规则要可预期，前提是它是一个纯函数——同样的输入永远得到同样的输出，才谈得上测得准。
 */
class OverdueFeeCalculator(
    private val dailyRate: Double,
    private val cap: Double,
) {
  init {
    require(dailyRate >= ZERO) { "日费率不能为负: $dailyRate" }
    require(cap >= ZERO) { "封顶值不能为负: $cap" }
  }

  fun fee(overdueDays: Int): Double {
    require(overdueDays >= 0) { "逾期天数不能为负: $overdueDays" }
    val raw = overdueDays * dailyRate
    return if (raw > cap) cap else raw
  }

  companion object {
    private const val ZERO = 0.0
  }
}