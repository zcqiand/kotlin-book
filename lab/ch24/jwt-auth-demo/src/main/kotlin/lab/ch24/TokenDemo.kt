package lab.ch24

import io.jsonwebtoken.JwtException

/**
 * TokenService 三回合演示入口：签发 / 合法校验 / 篡改校验，控制台逐字回执。
 * 运行：gradle runTokenDemo（独立 JavaExec 任务，不影响 application 主入口）
 */
fun main() {
    // ---- 回合 1：签发 access + refresh 双令牌，打印全文与解码后的 payload claims ----
    println("=== round 1: issue access + refresh ===")
    val access = TokenService.issueAccess("linmu", "admin")
    val refresh = TokenService.issueRefresh("linmu", "admin")
    println("accessToken  = $access")
    println("refreshToken = $refresh")
    fun printClaims(label: String, token: String) {
        val c = TokenService.verify(token)
        println(
            "$label decoded claims = sub=${c?.subject} role=${c?.get("role")} " +
                "iat=${c?.issuedAt} exp=${c?.expiration}"
        )
    }
    printClaims("access", access)
    printClaims("refresh", refresh)

    // ---- 回合 2：合法令牌校验通过，打印解析出的 sub / role / exp ----
    println("=== round 2: verify legit token ===")
    val claims = TokenService.verify(access)
    println(
        "verify(access) = PASS sub=${claims?.subject} role=${claims?.get("role")} " +
            "exp=${claims?.expiration}"
    )

    // ---- 回合 3：篡改载荷段一个字符，校验当场失败，打印异常类名与 message ----
    println("=== round 3: tampered token ===")
    val parts = access.split('.')
    val flipped = if (parts[1][0] != 'X') 'X' else 'Z'
    val tampered = listOf(parts[0], flipped + parts[1].substring(1), parts[2]).joinToString(".")
    println("tampered payload segment head char '$flipped': ${tampered.take(60)}......")
    try {
        TokenService.verifyStrict(tampered)
        println("UNEXPECTED: tampered token passed verification (bug!)")
    } catch (e: JwtException) {
        println("verifyStrict(tampered) FAIL exception=${e::class.qualifiedName}")
        println("message=${e.message}")
    }
}
