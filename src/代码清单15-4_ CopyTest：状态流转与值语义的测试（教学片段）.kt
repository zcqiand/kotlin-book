package library

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class CopyTest {
    // 图书样本：字段相同的两本 Book 视为同一本书
    private val book = Book("978-7-000-00001-1", "Kotlin 项目小册", "张三")

    @Test
    fun bookEqualityFollowsFields() {
        // data class 值语义：字段相同即相等，copy 只换一个字段
        val same = Book("978-7-000-00001-1", "Kotlin 项目小册", "张三")
        assertEquals(book, same)
        val reprinted = book.copy(title = "Kotlin 项目小册（第 2 版）")
        assertEquals("Kotlin 项目小册（第 2 版）", reprinted.title)
    }

    @Test
    fun checkoutMovesStateToBorrowed() {
        // 借出后：状态转为已借出，读者与借出日入账
        val copy = Copy(book)
        copy.checkout(memberId = "M-001", onDay = 1)
        val st = copy.state as LoanState.Borrowed
        assertEquals("M-001", st.memberId)
        assertEquals(1, st.loanDay)
    }

    @Test
    fun checkoutUnavailableCopyIsRejected() {
        // 非「在馆」处境借出：require 拦下，抛 IllegalArgumentException
        val copy = Copy(book)
        copy.checkout(memberId = "M-001", onDay = 1)
        assertFailsWith<IllegalArgumentException> {
            copy.checkout(memberId = "M-002", onDay = 2)
        }
    }

    @Test
    fun giveBackReturnsToAvailable() {
        // 归还后回到在馆
        val copy = Copy(book)
        copy.checkout(memberId = "M-001", onDay = 1)
        copy.giveBack()
        assertEquals(LoanState.Available, copy.state)
    }

    @Test
    fun reportLostMovesStateToLost() {
        // 报失后带申报日期入账
        val copy = Copy(book)
        copy.checkout(memberId = "M-001", onDay = 1)
        copy.reportLost(onDay = 40)
        val st = copy.state as LoanState.Lost
        assertEquals(40, st.reportedDay)
    }
}