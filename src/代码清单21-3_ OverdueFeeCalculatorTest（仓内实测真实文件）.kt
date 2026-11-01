package library

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

// 演示域共用常量：集中成具名常量，既是测试可读性，也为 detekt MagicNumber 规则让路。
// detekt 默认放过常量声明（ignoreConstantDeclaration），裸写数字才是它要拦的。
private const val DAILY_RATE = 0.5
private const val CAP = 20.0

class OverdueFeeCalculatorTest {

  private val calculator = OverdueFeeCalculator(dailyRate = DAILY_RATE, cap = CAP)

  @Test
  fun `零天逾期不收费`() {
    assertEquals(0.0, calculator.fee(0))
  }

  @Test
  fun `滞纳金按天数线性增长`() {
    assertEquals(1.5, calculator.fee(3))
  }

  @Test
  fun `封顶值生效后金额不再增长`() {
    // 39 天 x 0.5 = 19.5 未触顶；100 天早该是 50 元，但封顶把它按在 20 元。
    assertEquals(19.5, calculator.fee(39))
    assertEquals(CAP, calculator.fee(100))
  }

  @Test
  fun `负数逾期天数直接拒绝`() {
    assertThrows<IllegalArgumentException> { calculator.fee(-1) }
  }

  @Test
  fun `负费率在构造期即被拒绝`() {
    assertThrows<IllegalArgumentException> { OverdueFeeCalculator(dailyRate = -0.5, cap = CAP) }
  }
}