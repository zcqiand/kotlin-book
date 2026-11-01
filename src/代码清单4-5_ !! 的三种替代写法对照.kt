// 对照示意片段，不是独立运行文件；场景沿用清单4-1 的 discountRate(couponCode: String?): Double

// 反面写法（即清单4-1 踩中的雷）：!! 把空检查推迟到运行时，null 一到就地崩溃
// discountRate(couponCode!!)

// 替代一：Elvis 默认值——null 不算异常，只是"没填优惠码"，按原价语义结算
val rate1 = discountRate(couponCode ?: "NONE")

// 替代二：Elvis 配 return——优惠码缺失时提前退出函数，右侧返回值须与所在函数的返回类型匹配，Unit 函数可写裸 ?: return
val rate2 = discountRate(couponCode ?: return 1.0)

// 替代三：?.let——只在确实拿到优惠码时才进入折扣逻辑，拿不到统一按原价
val rate3 = couponCode?.let { discountRate(it) } ?: 1.0