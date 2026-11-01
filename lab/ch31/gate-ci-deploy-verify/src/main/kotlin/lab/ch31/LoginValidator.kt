package lab.ch31

/** 登录校验结果：有效带用户名，无效带原因。 */
sealed interface LoginResult {
  data class Valid(val username: String) : LoginResult

  data class Invalid(val reason: String) : LoginResult
}

/** 登录参数校验：四级门控流水线的示例被测单元（①格式门 ②静态门 ③测试门的靶子）。 */
object LoginValidator {
  private const val MIN_USERNAME_LENGTH = 3
  private const val MIN_PASSWORD_LENGTH = 6

  fun validate(username: String, password: String): LoginResult =
      when {
        username.isBlank() -> LoginResult.Invalid("用户名不能为空")
        username.length < MIN_USERNAME_LENGTH ->
            LoginResult.Invalid("用户名过短（少于 $MIN_USERNAME_LENGTH 字符）")
        password.isBlank() -> LoginResult.Invalid("密码不能为空")
        password.length < MIN_PASSWORD_LENGTH ->
            LoginResult.Invalid("密码过短（少于 $MIN_PASSWORD_LENGTH 字符）")
        else -> LoginResult.Valid(username)
      }
}
