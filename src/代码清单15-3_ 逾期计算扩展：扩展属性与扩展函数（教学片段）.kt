package library

import library.LoanState.Available
import library.LoanState.Borrowed

// 状态文案：when 穷举三种处境，日后新增状态这里编译报错
fun LoanState.label(): String = when (this) {
    Available -> "在馆可借"
    is Borrowed -> "已借出：读者 ${memberId}，第 ${loanDay} 天借出"
    is LoanState.Lost -> "已挂失：第 ${reportedDay} 天申报"
}

// 到期日 = 借出日 + 借期：纯派生值，不占存储，现算现取（扩展属性）
val Borrowed.dueDay: Int
    get() = loanDay + Copy.LOAN_DAYS

// 逾期天数：超过到期日的天数；借期内一律收口为 0，绝不出现负数
fun Borrowed.overdueDays(onDay: Int): Int = (onDay - dueDay).coerceAtLeast(0)

// 是否逾期：逾期天数大于零
fun Borrowed.isOverdue(onDay: Int): Boolean = overdueDays(onDay) > 0