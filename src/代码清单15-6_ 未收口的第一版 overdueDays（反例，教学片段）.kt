package library

// 反例：逾期天数未收口，借期内算出负数
fun Borrowed.overdueDays(onDay: Int): Int = onDay - dueDay