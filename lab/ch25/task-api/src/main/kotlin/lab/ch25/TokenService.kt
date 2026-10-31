package lab.ch25

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.util.Date
import javax.crypto.SecretKey

/**
 * 令牌服务：jjwt 自签 HS256，签发 access / refresh 双令牌——沿 ch24 形态照抄，只换包名。 access 短效（5 分钟）直接访问业务接口，refresh
 * 长效（7 天）只用来换发， 两者是同一密钥签出的两种「寿命」而不是两种权限。 演示密钥硬编码仅为本章签发/校验链路，读者切勿照搬进真实项目。
 */
object TokenService {

  // HS256 要求密钥长度不少于 32 字节，短密钥会被 jjwt 直接拒绝
  private const val DEMO_SECRET = "demo-only-secret-key-change-me-32-bytes!!"

  private val key: SecretKey = Keys.hmacShaKeyFor(DEMO_SECRET.toByteArray())

  const val ACCESS_TTL_MS: Long = 5 * 60 * 1000L // access：5 分钟短效
  const val REFRESH_TTL_MS: Long = 7 * 24 * 60 * 60 * 1000L // refresh：7 天长效

  /** 签发 access 令牌：claims = sub + role + iat + exp（exp 为 now + 5 分钟） */
  fun issueAccess(sub: String, role: String): String = issue(sub, role, ACCESS_TTL_MS)

  /** 签发 refresh 令牌：claims 结构与 access 相同，差别只在 exp（now + 7 天） */
  fun issueRefresh(sub: String, role: String): String = issue(sub, role, REFRESH_TTL_MS)

  /**
   * 提供给 Ktor jwt 插件的校验器：插件底层校验器类型是 auth0 java-jwt 的 JWTVerifier， 签发走 jjwt。JWT
   * 是跨库互认的标准格式——算法（HS256）与密钥字节一致， A 库签出的令牌 B 库照样验得过
   */
  fun ktorVerifier(): com.auth0.jwt.interfaces.JWTVerifier =
      com.auth0.jwt.JWT.require(com.auth0.jwt.algorithms.Algorithm.HMAC256(DEMO_SECRET)).build()

  private fun issue(sub: String, role: String, ttlMs: Long): String =
      Jwts.builder()
          .subject(sub) // sub：令牌主体
          .claim("role", role) // role：自定义私有声明
          .issuedAt(Date()) // iat：签发时间
          .expiration(Date(System.currentTimeMillis() + ttlMs)) // exp：过期时间
          .signWith(key, Jwts.SIG.HS256) // HS256 对称签名
          .compact()

  /** 校验令牌：签名不对、格式不对、已过期统一返回 null。 调用方（认证插件）只关心「这枚令牌能不能用」，细分原因交给日志层区分 */
  fun verify(token: String): Claims? =
      try {
        Jwts.parser()
            .verifyWith(key) // 同时校验签名与签发方密钥
            .build()
            .parseSignedClaims(token) // 签名/过期/格式任一不过即抛 JwtException
            .payload
      } catch (_: JwtException) {
        null
      }
}
