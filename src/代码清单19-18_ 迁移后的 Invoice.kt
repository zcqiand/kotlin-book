/**
 * 迁移后的 Invoice（迁移后）：主构造 + val 属性 + 默认参数 + require 校验，
 * 一个类顶掉 LegacyInvoice 的全部样板。构造器上的 @JvmOverloads 不是装饰：
 * 没有它，默认参数不生成重载链，Java 侧当年 new LegacyInvoice(owner, cents)
 * 的两参调用点会直接编译失败。
 */
class Invoice @JvmOverloads constructor(
    val owner: String,          // 只读化：原 getOwner() 语义不变，setter 天然不存在
    val amountCents: Int,       // 以分为单位存整数，避免浮点金额误差
    var memo: String = "",      // 默认参数吃掉原构造器重载；memo 仍可变（原 setMemo 语义不变）
) {
    init {
        // require 替代手写 if-throw：非法金额在构造期就地拒绝，坏对象造不出来
        require(amountCents >= 0) { "金额不能为负：$amountCents" }
    }

    fun describe(): String =
        "Invoice(owner=$owner, amountCents=$amountCents, memo='${memo.ifEmpty { "<空>" }}')"
}