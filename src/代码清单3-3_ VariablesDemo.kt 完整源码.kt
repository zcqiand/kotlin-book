package vars

fun main() {
    // 只读引用正例：language 声明后不会再指向别的对象，val 让编译器替我们守住这条底线
    val language = "Kotlin"

    // 确需重赋值才轮到 var：重试计数天然会变，这是 var 的正当场景
    var retryCount = 0
    retryCount += 1

    // maxAttempts 声明后从不重新赋值——一开始就该用 val；
    // var 声明却从不重赋值，是选型失误的信号
    val maxAttempts = 3

    // val 只锁引用不锁对象：scores 始终指向同一个列表，但列表内部仍可增删
    val scores = mutableListOf(88)
    scores.add(95)

    // 类型推断：右侧字面量已足够编译器认定类型，左侧无须手写
    val count = 42             // 编译器认定：Int
    val price = 9.9            // 编译器认定：Double
    val total = 3_000_000_000  // 编译器认定：Long（超出 Int 上限，自动升位）
    val ready = true           // 编译器认定：Boolean

    // 可空类型最小面：显式允许 null；空安全机制第 4 章兑现
    val nickname: String? = null

    println("language=$language")
    println("retryCount=$retryCount maxAttempts=$maxAttempts")
    println("scores=$scores")
    println("count=$count price=$price")
    println("total=$total")
    println("ready=$ready")
    println("nickname=$nickname")
}