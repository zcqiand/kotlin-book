package demo

/**
 * @Jvm 系列注解的展示类（清单 19-7 / 19-8 / 19-9 的 Kotlin 一侧）。
 * 注解的效果全部发生在 Java 侧调用点，Kotlin 侧自己看不出差别，实测放 JavaCaller 里。
 */

class TokenStore {
    companion object {
        // 不加 @JvmStatic：Java 侧必须写 TokenStore.Companion.defaultIssuer()，门面多一层
        fun defaultIssuer(): String = "kotlin-book"
    }
}

class Session {
    companion object {
        // @JvmStatic：静态方法同时长在 Session 类上，Java 侧直接 Session.create()
        @JvmStatic
        fun create(): Session = Session()

        // const val 本身就是类级常量（编译即静态字段），不需要注解加持
        const val MAX_RETRY: Int = 3
    }

    fun describe(): String = "Session 实例（maxRetry=$MAX_RETRY）"
}

// @JvmOverloads：按默认参数生成完整重载链，Java 侧才用得起 1/2/3 参构造
class InvoiceQuery @JvmOverloads constructor(
    val owner: String,
    val page: Int = 1,
    val pageSize: Int = 20,
) {
    fun describe(): String = "InvoiceQuery(owner=$owner, page=$page, pageSize=$pageSize)"
}

class DeviceInfo {
    // @JvmField：绕过 getter/setter，以公共字段形态直接暴露给 Java
    @JvmField
    val serial: String = "SN-2026-0042"
}
