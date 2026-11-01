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