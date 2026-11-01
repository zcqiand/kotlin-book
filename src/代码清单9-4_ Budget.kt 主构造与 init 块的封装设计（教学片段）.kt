// 预算账本：怎么花由外界发起，怎么记只有本类知道
class Budget(
    val limit: Int,               // 只读对外：谁都能看，谁也改不了
    private var spent: Int = 0    // 可变对内：外界连看都看不到
) {
    init {
        // 出生验收：非法参数在这里拦下，坏对象没有出生的机会
        require(limit > 0) { "limit 必须为正数，收到 $limit" }
        require(spent >= 0) { "spent 不能为负，收到 $spent" }
    }

    val remaining: Int
        get() = limit - spent     // 每次读取现算，不占存储

    val isOverdrawn: Boolean
        get() = spent > limit

    fun spend(amount: Int) {
        require(amount >= 0) { "spend 不能为负，收到 $amount" }
        spent += amount           // 花钱是唯一动账入口
    }
}