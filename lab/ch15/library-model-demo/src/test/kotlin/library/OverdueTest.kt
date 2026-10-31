package library

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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
