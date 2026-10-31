package demo

import legacy.JavaCaller
import legacy.LegacyUserStore

// 全章共用一份 Java 存量实例：kotlin 顶层属性即单例，四个演示各取所需
private val legacyStore = LegacyUserStore()

/**
 * 演示 2：双向互调。Kotlin 调 Java 的调用点在本函数，
 * Java 调 Kotlin 的调用点在 legacy.JavaCaller，两头都真实编译运行。
 */
fun twoWayDemo() {
    println("========== 演示 2: twoWayDemo（双向互调：Kotlin 调 Java / Java 调 Kotlin） ==========")
    // Kotlin 调 Java：getter/setter 在这里就是属性语法，赋值/取值两行编译后走 set/get 对
    legacyStore.displayName = "  老陈  "   // 实际走 setDisplayName，Java 侧会 trim
    legacyStore.level = 9                  // 实际走 setLevel(9)
    println("[two-way] Kotlin 写 Java 属性：displayName=${legacyStore.displayName}，level=${legacyStore.level}")
    // 静态工具方法：Kotlin 侧直接类名调用，跟调用 Kotlin object 没有手感差别
    println("[two-way] Java 静态方法直调：LegacyUserStore.versionTag() = ${LegacyUserStore.versionTag()}")
    // 反方向（Java 调 Kotlin）的调用点在 JavaCaller 里，这里只收结果
    println("[two-way] Java 调 Kotlin：${JavaCaller.callKotlinRepo()}")
    println("---------- 演示 2 结束 ----------")
}

/**
 * 演示 3：平台类型防御演练（P09）。同一个可空性未知的 Java 方法，三种处置对照。
 */
fun p09DrillDemo() {
    println("========== 演示 3: p09DrillDemo（平台类型防御：String! 的坑位地图） ==========")
    // (a) 裸奔：平台类型当非空用，编译器不拦，null 在成员访问处当场炸 NPE
    try {
        val length = legacyStore.findNickname("u404").length  // findNickname 返回 String!，这行照常编译
        println("[p09] (a) 裸奔侥幸成功：length=$length")
    } catch (e: NullPointerException) {
        println("[p09] (a) 裸奔下场：${e::class.simpleName} —— null 喂进非空位，运行期才暴露")
    }
    // (b) 防御性收窄：在赋值点显式标注 String?，之后 ?. 与 ?: 接管（第 4 章安全调用决策框架口径）
    val nickname: String? = legacyStore.findNickname("u404")
    println("[p09] (b) 防御收窄：${nickname?.let { "昵称=$it" } ?: "查无此人，走兜底昵称「访客」"}")
    // (c) 边界收口：包装函数签名上显式声明可空性，平台类型被封死在这一个函数里
    println("[p09] (c) 边界收口 findNicknameOrNull(\"u1001\")：${findNicknameOrNull("u1001")}")
    println("---------- 演示 3 结束 ----------")
}

// 边界收口的落点：可空性写进签名，调用方从签名就读得到真相，再也接触不到平台类型
fun findNicknameOrNull(userId: String): String? = legacyStore.findNickname(userId)

/**
 * 演示 4：@Jvm 系列注解。注解生效在 Java 调用点，实测全在 JavaCaller 三个方法里。
 */
fun jvmAnnotationsDemo() {
    println("========== 演示 4: jvmAnnotationsDemo（@Jvm 注解：给 Java 调用方一份好接口） ==========")
    // companion 成员两态：TokenStore（无注解，走 Companion 门面）对照 Session（@JvmStatic，类上直调）
    println("[jvm] companion 两态：${JavaCaller.callCompanionMembers()}")
    // @JvmOverloads：默认参数在 Java 侧退化成重载链，三种实参数量各 new 一次
    println("[jvm] @JvmOverloads 重载链：${JavaCaller.callInvoiceQueryOverloads()}")
    // @JvmField：Java 侧绕过 getter 直接摸字段
    println("[jvm] @JvmField 字段直访：${JavaCaller.readDeviceField()}")
    println("---------- 演示 4 结束 ----------")
}

/**
 * 演示 5：增量迁移。LegacyInvoice → Invoice，行为等价、调用方零感知。
 */
fun migrateDemo() {
    println("========== 演示 5: migrateDemo（增量迁移：LegacyInvoice → Invoice） ==========")
    // Kotlin 侧新写法：默认参数让两档构造合一，不用再写构造器委托
    val migrated = Invoice(owner = "老陈", amountCents = 12900)
    val migratedFull = Invoice(owner = "老陈", amountCents = 12900, memo = "十一月书费")
    println("[migrate] Kotlin 侧两档构造：${migrated.describe()}")
    println("[migrate]             　　${migratedFull.describe()}")
    // Java 侧复调：调用形态与当年调 LegacyInvoice 完全同形，一处没改照样编译运行
    println("[migrate] Java 侧复调（调用形态不变）：${JavaCaller.callInvoice()}")
    // 增强项抽查：require 在构造期拒绝负数金额，坏对象造不出来
    try {
        Invoice(owner = "老陈", amountCents = -1)
    } catch (e: IllegalArgumentException) {
        println("[migrate] require 校验接管：${e.message}")
    }
    println("---------- 演示 5 结束 ----------")
}

fun main() {
    twoWayDemo()
    p09DrillDemo()
    jvmAnnotationsDemo()
    migrateDemo()
}
