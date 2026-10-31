package library

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

private const val CAP = 20.0

/**
 * 参数化测试：同一套断言逻辑，被一列 (天数, 费率, 期望值) 数据驱动。 好处：断言只写一遍（一处改对处处对），补一组边界只加一行数据，不用复制粘贴测试方法。 第 6/7
 * 行是封顶触发与触顶后的"不再增长"——这正是参数表里最该有的边界行。
 */
class OverdueFeeParameterizedTest {

  @ParameterizedTest(name = "逾期{0}天, 日费率{1} => 滞纳金应为{2}元")
  @CsvSource(
      "0, 0.5, 0.0",
      "1, 0.5, 0.5",
      "3, 0.5, 1.5",
      "10, 0.5, 5.0",
      "39, 0.5, 19.5",
      "40, 0.5, 20.0",
      "100, 0.5, 20.0",
      "0, 2.0, 0.0",
      "5, 2.0, 10.0",
      "20, 2.0, 20.0",
  )
  fun `滞纳金按公式或封顶值结算`(
      overdueDays: Int,
      dailyRate: Double,
      expected: Double,
  ) {
    val calculator = OverdueFeeCalculator(dailyRate = dailyRate, cap = CAP)
    assertEquals(expected, calculator.fee(overdueDays))
  }
}
