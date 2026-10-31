package library

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LifecycleTest {
    @Test
    fun fullLifecycleWalkthrough() {
        // 全流程逐站验收：在馆 → 借出 → 逾期 15 天 → 归还回架
        val book = Book("978-7-111-00002-8", "数据结构", "李四")
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
