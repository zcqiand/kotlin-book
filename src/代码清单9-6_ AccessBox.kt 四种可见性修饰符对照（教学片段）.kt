// 四种可见性：默认 public，其余三种逐级收窄
class AccessBox {
    val open = "public：哪里都能读"              // 不写修饰符就是 public
    internal val inModule = "internal：同模块可见" // 同一模块内通行，模块外不可见
    protected val forChildren = "protected：类与子类专属" // 继承者的份额，第 10 章兑现
    private val secret = "private：只有本类能碰"   // 类墙以内才可见

    fun readAll(): String = "$open | $inModule | $forChildren | $secret"
}