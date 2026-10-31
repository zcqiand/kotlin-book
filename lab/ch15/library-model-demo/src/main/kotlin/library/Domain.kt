package library

// 图书：只描述书本身（书种），与馆藏中的具体某一册无关
data class Book(val isbn: String, val title: String, val author: String)

// 借阅状态机：一册藏书的全部合法处境（密封接口，子类型全部住在本包内）
sealed interface LoanState {
    // 在馆可借：无数据可带，data object 单例
    data object Available : LoanState
    // 已借出：记录借给谁、第几天借出
    data class Borrowed(val memberId: String, val loanDay: Int) : LoanState
    // 已挂失：读者申报遗失，暂停外借
    data class Lost(val reportedDay: Int) : LoanState
}

// 藏书单册：状态机长在「册」上；同一书种可以有多册，各走各的状态
class Copy(val book: Book) {
    companion object {
        // 借期：全馆统一的借阅天数，类级常量
        const val LOAN_DAYS = 30
    }

    // 状态对外只读：改动只能走 checkout/giveBack/reportLost 三道馆内规矩
    var state: LoanState = LoanState.Available
        private set

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
}
