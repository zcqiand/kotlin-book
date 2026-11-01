package controlflow

// 形态一：带主语的 when。主语只出现一次，分支全部围绕它做相等匹配，
// 多个值命中同一结果时用逗号合并一行，不必写一串 ||
fun deliveryTrack(status: Int): String = when (status) {
    0 -> "已下单"
    1 -> "打包中"
    2, 3 -> "运输中"
    4 -> "已签收"
    else -> "状态未知"
}

// 形态二：不带主语的 when。每个分支是独立的布尔条件，从上往下取第一个命中的分支，
// 适合条件彼此不同源的场景（密码长度、验证码、弱口令各查各的）
fun loginHint(password: String, captchaOk: Boolean): String = when {
    password.length < 8 -> "密码至少 8 位"
    !captchaOk -> "验证码未通过"
    password == "12345678" -> "密码过于常见"
    else -> "可以登录"
}

// 形态三：is 类型判断。分支问的是「主语是不是这个类型」，
// 命中后分支内直接把它当该类型用，无须手动强转
fun couponLabel(coupon: Any): String = when (coupon) {
    is String -> "券码 ${coupon.uppercase()}，去券码箱核销"
    is Int -> "满减额度 $coupon 元"
    is Double -> "折扣率 $coupon，直接乘总价"
    else -> "未知券种"
}

// 形态四：in 区间匹配。分数天然是连续段，用 in 逐段收口，
// 区间从高到低排列且互不重叠，else 兜住范围外的脏数据
fun gradeOf(score: Int): String = when (score) {
    in 90..100 -> "优秀"
    in 80..89 -> "良好"
    in 60..79 -> "及格"
    in 0..59 -> "不及格"
    else -> "成绩超出范围"
}

fun main() {
    // 每个形态挑两三笔有代表性的输入：命中常规分支、命中合并分支、落进 else
    println(deliveryTrack(0))
    println(deliveryTrack(3))
    println(deliveryTrack(9))
    println(loginHint("abc", true))
    println(loginHint("12345678", true))
    println(loginHint("kotlin2026", false))
    println(couponLabel("save2026"))
    println(couponLabel(30))
    println(couponLabel(0.85))
    println(gradeOf(92))
    println(gradeOf(73))
    println(gradeOf(105))
}