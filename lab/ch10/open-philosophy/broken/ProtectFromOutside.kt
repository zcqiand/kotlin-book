package demo

// 反例：外部代码（非子类）直接触碰 SavingsAccount 的 protected 成员——
// protected 的「家族内部可见」承诺应在编译期把这份代码拦下
fun touchProtectedFromOutside() {
    val account = SavingsAccount("外人")
    account.audit("私自记账")     // 发①：audit 是 protected，类墙外不可见
    println(account.auditLog)    // 发②：auditLog 是 protected，类墙外不可见
}
