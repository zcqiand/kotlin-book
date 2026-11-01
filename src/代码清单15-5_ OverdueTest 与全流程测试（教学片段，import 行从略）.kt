class OverdueTest {
    // 借出第 1 天、借期 30 天：到期日为第 31 天
    private fun borrowed(): LoanState.Borrowed =
        LoanState.Borrowed(memberId = "M-001", loanDay = 1)

    @Test
    fun dueDayIsLoanDayPlusLoanDays() {
        assertEquals(31, borrowed().dueDay)
    }

    @Test
    fun overdueDaysIsZeroBeforeDueDay() {
        // 借期内（含到期当天）：逾期天数为 0
        assertEquals(0, borrowed().overdueDays(onDay = 20))
        assertEquals(0, borrowed().overdueDays(onDay = 31))
    }

    @Test
    fun overdueDaysCountsDaysBeyondDueDay() {
        // 第 32 天起开始计逾期，超一天算一天
        assertEquals(1, borrowed().overdueDays(onDay = 32))
        assertEquals(15, borrowed().overdueDays(onDay = 46))
    }

    @Test
    fun isOverdueTracksOverdueDays() {
        assertTrue(borrowed().isOverdue(onDay = 46))
        assertFalse(borrowed().isOverdue(onDay = 31))
    }
}

class LifecycleTest {
    @Test
    fun fullLifecycleWalkthrough() {
        // 全流程逐站验收：在馆 → 借出 → 逾期 15 天 → 归还回架
        val book = Book("978-7-000-00002-8", "数据结构", "李四")
        val copy = Copy(book)
        assertEquals(LoanState.Available, copy.state)          // 第 0 站：新书在馆
        copy.checkout(memberId = "M-007", onDay = 1)
        assertTrue(copy.state is LoanState.Borrowed)           // 第 1 站：借出
        val st = copy.state as LoanState.Borrowed
        assertEquals(15, st.overdueDays(onDay = 46))           // 第 2 站：逾期 15 天
        copy.giveBack()
        assertEquals(LoanState.Available, copy.state)          // 第 3 站：归还回架
        assertEquals("在馆可借", copy.state.label())            // 第 4 站：文案随处境切换
    }
}