// 写的是属性，Java 侧看到的是 getRegion/setRegion/getUserCount 方法对——互操作的第一课
class UserRepo {
    // var 属性：Java 侧拿到 getRegion() 与 setRegion() 一对方法
    var region: String = "cn-north"

    // val 属性：Java 侧只有 getUserCount()，写入动作在 Java 里不存在
    val userCount: Int = 2

    fun greet(name: String): String = "你好，$name（来自 Kotlin）"
}