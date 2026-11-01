package library

// 复绿：coerceAtLeast(0) 把负数压回零，借期内逾期一律为 0
fun Borrowed.overdueDays(onDay: Int): Int = (onDay - dueDay).coerceAtLeast(0)