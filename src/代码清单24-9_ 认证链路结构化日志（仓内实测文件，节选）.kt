// 结构化日志专用 logger：事件面（AUTH_401/AUTH_200）全部从这里走，
// 与框架自身的 logger 分开，回放时按 logger 名一眼可筛
val authLog: Logger = LoggerFactory.getLogger("lab.ch24.AuthAudit")

/**
 * 路由层取身份的统一入口：每次合法访问在此落一行 AUTH_200 结构化日志，
 * 与 challenge 里的 AUTH_401 共用同一个 logger，认证成败可在日志里对齐回放
 */
private fun ApplicationCall.adminIdentity(): AdminPrincipal {
    val p = principal<AdminPrincipal>()!!
    authLog.info("event=AUTH_200 path=${request.uri} reason=ok sub=${p.sub} role=${p.role}")
    return p
}