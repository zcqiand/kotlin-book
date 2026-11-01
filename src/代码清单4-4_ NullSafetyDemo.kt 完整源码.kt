package nulls

data class UserProfile(val userId: String, val email: String?, val backupPhone: String?)

// 返回类型也声明成可空：域名可能取不到这件事，要让调用方从签名上就读出来
fun emailDomainOf(email: String?): String? = email?.substringAfter("@")

fun main() {
    val lin = UserProfile("U-001", "lin@example.com", "13800000000")
    val guest = UserProfile("U-002", null, null)

    // 一、安全调用 ?.：email 为 null 时整条表达式短路为 null，程序不崩
    println(emailDomainOf(lin.email))
    println(emailDomainOf(guest.email))

    // 二、Elvis ?: 兜底：把"左边为 null"翻译成业务上的默认文案，而不是技术化的 null
    val domainFallback = emailDomainOf(guest.email) ?: "未绑定邮箱"
    println(domainFallback)

    // 三、Elvis 配 return：关键字段缺失时提前退出，后面整段逻辑都不必再判空
    fun sendVerifyCode(user: UserProfile): String {
        val email = user.email ?: return "未留邮箱，无法发送验证码"
        return "验证码已发送至 $email"
    }
    println(sendVerifyCode(lin))
    println(sendVerifyCode(guest))

    // 四、?.let：只在非空时执行动作；null 那位连一行输出都不产生
    lin.backupPhone?.let { println("备用电话：$it") }
    guest.backupPhone?.let { println("备用电话：$it") }
}