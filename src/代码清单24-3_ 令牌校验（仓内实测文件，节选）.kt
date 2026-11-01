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