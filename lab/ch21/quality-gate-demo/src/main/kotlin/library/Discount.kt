package library

/**
 * 会员折扣计算：超过阈值打九折。 修复 MagicNumber 的正解不是压制规则，而是把业务数字升格为具名常量—— 10.0 变成折扣门槛，0.9 变成折扣率，业务含义回到代码里，规则自然放行。
 */
private const val DISCOUNT_THRESHOLD = 10.0
private const val DISCOUNT_RATE = 0.9

fun discountFee(amount: Double): Double {
  return if (amount > DISCOUNT_THRESHOLD) amount * DISCOUNT_RATE else amount
}
