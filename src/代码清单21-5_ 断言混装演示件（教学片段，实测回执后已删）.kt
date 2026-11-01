class MultiAssertBadDemoTest {

  @Test
  fun `一条塞多条断言的坏味道`() {
    val calculator = OverdueFeeCalculator(dailyRate = 0.5, cap = 20.0)
    assertEquals(0.0, calculator.fee(0)) // 第 1 条: 通过
    assertEquals(1.3, calculator.fee(3)) // 第 2 条: 失败, 异常当场抛出
    assertEquals(20.0, calculator.fee(100)) // 第 3 条: 从未执行, 对错无从得知
  }
}