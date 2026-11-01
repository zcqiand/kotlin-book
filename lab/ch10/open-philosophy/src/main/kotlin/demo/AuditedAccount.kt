package demo

// 兑现第 9 章表 9-1 的伏笔：protected = 「家族内部可见」——
// 子类内随便用，类墙之外（哪怕同包）编译期一律拦下
open class AuditedAccount(owner: String) {
    // protected 成员：审计痕迹只允许继承体系内部读写，
    // 外部只能走 auditHistory() 的只读出口
    protected val auditLog = mutableListOf("$owner：账户创建")

    protected fun audit(event: String) {
        auditLog.add(event)
    }

    // 对外出口：暴露只读视图，写入口仍留在家族内
    fun auditHistory(): List<String> = auditLog.toList()
}

class SavingsAccount(owner: String) : AuditedAccount(owner) {
    fun deposit(amount: Int) {
        // require 校验沿 ch9 口径：非法输入在入口处先拦
        require(amount > 0) { "存款必须为正数，收到 $amount" }
        // 子类内调用 protected 成员：合法，这正是 protected 的设计意图
        audit("存款 $amount")
    }
}
