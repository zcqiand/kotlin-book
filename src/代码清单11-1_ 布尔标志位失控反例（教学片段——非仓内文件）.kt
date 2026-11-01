package demo

// 布尔标志位的失控现场：var 标志各自为政、互不约束，
// 类型系统既不知道「已删除」意味着什么，也拦不住互斥标志同时为真
class Notification(val id: Int) {
    var isRead = false
    var isArchived = false
    var isDeleted = false
}

fun runFlagChaos() {
    val notice = Notification(101)
    // 一条消息走完「删除」之后，任何代码仍可对另外两个标志各改各的——没有一道门检查当前状态
    notice.isDeleted = true
    notice.isRead = true
    notice.isArchived = true
    println("消息#${notice.id}：已删除=${notice.isDeleted}，已读=${notice.isRead}，已归档=${notice.isArchived}")
    println("矛盾一：已删除的消息被继续标记为已读——回收站里不存在「未读」，标志却照改不误，全程静默通过")
    println("矛盾二：已归档与已删除同时为真，两个互斥的存放位同时成立，编译器一行报错都没有")
}