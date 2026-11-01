package lab.ch31

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoginValidatorTest {
  @Test
  fun `合法登录通过`() {
    val result = LoginValidator.validate("李工程师", "secret1")
    assertEquals(LoginResult.Valid("李工程师"), result)
  }

  @Test
  fun `空用户名被拒`() {
    val result = LoginValidator.validate("", "secret1")
    assertTrue(result is LoginResult.Invalid)
  }

  @Test
  fun `过短密码被拒`() {
    val result = LoginValidator.validate("李工程师", "123")
    assertTrue(result is LoginResult.Invalid)
  }

  @Test
  fun `过短用户名被拒`() {
    val result = LoginValidator.validate("ab", "secret1")
    assertTrue(result is LoginResult.Invalid)
  }

  @Test
  fun `DATABASE_URL 解析五字段齐全`() {
    val parsed = parseDatabaseUrl("postgresql://lab:pw@127.0.0.1:15439/lab_ch31_verify")
    assertEquals("127.0.0.1", parsed.host)
    assertEquals("15439", parsed.port)
    assertEquals("lab_ch31_verify", parsed.db)
    assertEquals("lab", parsed.user)
    assertEquals("pw", parsed.password)
  }
}
