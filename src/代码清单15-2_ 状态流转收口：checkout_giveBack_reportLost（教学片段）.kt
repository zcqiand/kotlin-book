// 承清单 15-1 的 Copy 类，以下三个方法即「三道馆内规矩」

// 借出：只放行「在馆」这一种处境，其余一律拒绝
fun checkout(memberId: String, onDay: Int) {
    require(state == LoanState.Available) { "不在馆不能外借：$state" }
    state = LoanState.Borrowed(memberId, onDay)
}

// 归还：借出的册子回到架上；先抄进局部 val 再判断，类型判断才有着力点
fun giveBack() {
    val current = state
    require(current is LoanState.Borrowed) { "只有借出的册子能归还：$state" }
    state = LoanState.Available
}

// 报失：借出的册子可申报遗失
fun reportLost(onDay: Int) {
    val current = state
    require(current is LoanState.Borrowed) { "只有借出的册子能报失：$state" }
    state = LoanState.Lost(onDay)
}