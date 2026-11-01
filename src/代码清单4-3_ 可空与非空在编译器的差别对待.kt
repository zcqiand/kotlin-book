package nulls

fun main() {
    // 非空类型：编译器担保它任何时刻都不持有 null，成员随便调
    val realName: String = "林知远"
    println("注册名长度：${realName.length}")

    // 可空类型：可能持有 null，编译器把"直接访问成员"列为编译期错误
    val nickname: String? = null
    // println(nickname.length)
    // ↑ 这一行无法通过编译：Only safe (?.) or non-null asserted (!!) calls are allowed on a nullable receiver
    // 编译器宁可逼你当场表态，也不愿让程序在用户手机上崩。

    // 安全调用 ?.：null 直接短路成 null，非空才真正取 length——唯一不冒运行时风险的通道
    println("昵称长度：${nickname?.length}")
    // !! 是另一条通道，代价是把空检查推迟到运行时，4.6 节专门清算它
}