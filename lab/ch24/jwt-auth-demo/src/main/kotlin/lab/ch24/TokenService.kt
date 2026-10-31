package lab.ch24

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.util.Date
import javax.crypto.SecretKey

/**
 * 演示用令牌服务：jjwt 自签 HS256，签发 access / refresh 双令牌。
 *
 * 为什么分开两枚令牌：access 令牌有效期短（这里 5 分钟），即使泄露，
 * 攻击窗口也有限；refresh 令牌有效期长（这里 7 天），只用来换发新的
 * access 令牌，不直接访问业务接口。两者互不通用，是同一密钥签出的
 * 两种「寿命」而不是两种权限。
 *
 * 注意：真实服务的密钥必须来自配置或密钥管理系统，且不能提交进代码仓。
 * 这里硬编码仅为本章演示签发与校验链路，读者切勿照搬进真实项目。
 */
object TokenService {

    // HS256 要求密钥长度不少于 32 字节，短密钥会被 jjwt 直接拒绝
    private const val DEMO_SECRET = "demo-only-secret-key-change-me-32-bytes!!"

    private val key: SecretKey = Keys.hmacShaKeyFor(DEMO_SECRET.toByteArray())

    const val ACCESS_TTL_MS: Long = 5 * 60 * 1000L        // access：5 分钟短效
    const val REFRESH_TTL_MS: Long = 7 * 24 * 60 * 60 * 1000L // refresh：7 天长效

    /** 签发 access 令牌：claims = sub + role + iat + exp（exp 为 now + 5 分钟） */
    fun issueAccess(sub: String, role: String): String = issue(sub, role, ACCESS_TTL_MS)

    /** 签发 refresh 令牌：claims 结构与 access 相同，差别只在 exp（now + 7 天） */
    fun issueRefresh(sub: String, role: String): String = issue(sub, role, REFRESH_TTL_MS)

    /**
     * 提供给 Ktor jwt 插件的校验器。
     *
     * 为什么这里出现第二个 JWT 库：Ktor 的 jwt 插件底层的校验器类型是
     * auth0 java-jwt 的 JWTVerifier，而本工程的签发走 version-lock 钉定的
     * jjwt。JWT 是跨库互认的标准格式——只要算法（HS256）与密钥字节一致，
     * A 库签出的令牌 B 库照样验得过。这里就是用同一把密钥构造插件侧校验器
     */
    fun ktorVerifier(): com.auth0.jwt.interfaces.JWTVerifier =
        com.auth0.jwt.JWT.require(com.auth0.jwt.algorithms.Algorithm.HMAC256(DEMO_SECRET))
            .build()

    private fun issue(sub: String, role: String, ttlMs: Long): String =
        Jwts.builder()
            .subject(sub)                                          // sub：令牌主体
            .claim("role", role)                                   // role：自定义私有声明
            .issuedAt(Date())                                      // iat：签发时间
            .expiration(Date(System.currentTimeMillis() + ttlMs))  // exp：过期时间
            .signWith(key, Jwts.SIG.HS256)                         // HS256 对称签名
            .compact()

    /**
     * 校验令牌：签名不对、格式不对、已过期都统一返回 null。
     *
     * 为什么不向外抛异常：调用方（认证插件）只关心「这枚令牌能不能用」，
     * 校验失败的细分原因（坏签名/过期/格式）交给日志层去区分，接口面
     * 保持单一布尔语义，避免调用方逐类捕获。
     */
    fun verify(token: String): Claims? = try {
        Jwts.parser()
            .verifyWith(key)          // 同时校验签名与签发方密钥
            .build()
            .parseSignedClaims(token) // 签名/过期/格式任一不过即抛 JwtException
            .payload
    } catch (_: JwtException) {
        null
    }

    /**
     * 演示用严格校验：与 verify 同一条解析链，但不吞异常——JwtException 原样
     * 抛给调用方，供 TokenDemo 打印失败的具体原因（坏签名/过期/格式各自异常
     * 类名不同）。verify 本体就是它的 null 包裹，两者不是两条链路
     */
    fun verifyStrict(token: String): Claims =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
}
