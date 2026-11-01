object TokenService {
    // HS256 要求密钥长度不少于 32 字节，短密钥会被 jjwt 直接拒绝
    // DEMO_SECRET 仅为本书教学自签闭环演示用；真实部署必须另行生成并妥善保管密钥，严禁硬编码入库。
    private const val DEMO_SECRET = "demo-only-secret-key-change-me-32-bytes!!"

    private val key: SecretKey = Keys.hmacShaKeyFor(DEMO_SECRET.toByteArray())

    const val ACCESS_TTL_MS: Long = 5 * 60 * 1000L            // access：5 分钟短效
    const val REFRESH_TTL_MS: Long = 7 * 24 * 60 * 60 * 1000L // refresh：7 天长效

    /** 签发 access 令牌：claims = sub + role + iat + exp（exp 为 now + 5 分钟） */
    fun issueAccess(sub: String, role: String): String = issue(sub, role, ACCESS_TTL_MS)

    /** 签发 refresh 令牌：claims 结构与 access 相同，差别只在 exp（now + 7 天） */
    fun issueRefresh(sub: String, role: String): String = issue(sub, role, REFRESH_TTL_MS)

    // ……（省略：ktorVerifier()——用同一把密钥构造 Ktor 插件侧校验器，跨库互认）

    private fun issue(sub: String, role: String, ttlMs: Long): String =
        Jwts.builder()
            .subject(sub)                                          // sub：令牌主体
            .claim("role", role)                                   // role：自定义私有声明
            .issuedAt(Date())                                      // iat：签发时间
            .expiration(Date(System.currentTimeMillis() + ttlMs))  // exp：过期时间
            .signWith(key, Jwts.SIG.HS256)                         // HS256 对称签名
            .compact()
    // ……（省略：verify / verifyStrict 校验段，见清单 24-3）
}