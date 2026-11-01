install(Authentication) {
    jwt("admin-jwt") {
        realm = "admin-api"
        // 校验器与签发共用同一把密钥：两边密钥不同则签名校验必然失败
        verifier(TokenService.ktorVerifier())
        validate { credential ->
            // 签名/过期校验已过再补业务面检查；返回 null 即认证失败，走 challenge
            val sub = credential.payload.subject
            val role = credential.payload.getClaim("role")?.asString()
            if (sub != null && role != null) AdminPrincipal(sub, role) else null
        }
        challenge { _, _ ->
            // 缺令牌（没带）与坏令牌（不对）分开记因：排查时是两类完全不同的问题；
            // 插件的 challenge 回调不带失败原因，这里用请求头有无自判
            val reason =
                if (call.request.headers[HttpHeaders.Authorization] == null) "missing_token"
                else "bad_token"
            authLog.warn(
                "event=AUTH_401 path=${call.request.uri} reason=$reason sub=-"
            )
            // ……（省略：追加 WWW-Authenticate 质询头并回 401 统一错误体）
        }
    }
}

authenticate("admin-jwt") {
    get("/admin/audit") {
        val p = call.adminIdentity()
        call.respond(AuditResponse(lastLoginBy = p.sub, records = 42))
    }
    // ……（省略：/admin/stats、/admin/users 两条同构路由）
}