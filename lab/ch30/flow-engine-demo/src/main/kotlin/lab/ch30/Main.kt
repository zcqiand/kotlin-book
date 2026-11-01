/**
 * ch30 lab｜场景回执：一趟完整流转 + 非法跃迁拦截 + 越权拦截。
 *
 * 模块落点按权限串业务域对齐（对码 DEMO_PERMISSIONS 五域前缀）：
 * 收样→sample、检测→inspection、签发→report、归档→audit。
 */
package lab.ch30

/** 密封类穷举：状态 → 业务域前缀（对码权限串家族），无 else */
fun domainOf(state: FlowState): String = when (state) {
    is FlowState.Received -> "sample"
    is FlowState.TaskAssigned, is FlowState.DataEntered, is FlowState.Reviewed -> "inspection"
    is FlowState.Approved, is FlowState.Issued -> "report"
    is FlowState.Archived, is FlowState.Completed -> "audit"
}

fun printStation(n: Int, state: FlowState) {
    println("[站$n] 状态=${contractStatus(state)} 模块落点=${domainOf(state)} 读数=${describe(state)}")
}

fun main() {
    // ── 回执 a：一趟完整流转（主链八站） ──────────────────────────
    println("== 回执 A｜完整流转（receiving → task_assignment → data_entry → review → approval → issuance → archived → completed） ==")
    val engine = FlowEngine(FlowState.Received("RCPT-2026-1101-001", "T-01"))
    var station = 1
    printStation(station, engine.current())
    val steps = listOf(
        FlowAction.Assign("李检测"),
        FlowAction.EnterData(12),
        FlowAction.Review("王复核", "pass"),
        FlowAction.Approve("赵主任"),
        FlowAction.Issue("RPT-2026-0031"),
        FlowAction.Archive("ARC-2026-0007"),
        FlowAction.Complete("2026-11-01T17:30"),
    )
    for (action in steps) {
        engine.dispatch(action)
        station++
        printStation(station, engine.current())
    }

    // ── 回执 b：非法跃迁被拦（receiving 直跳 issuance） ────────────
    println()
    println("== 回执 B｜非法跃迁拦截 ==")
    val rogue = FlowEngine(FlowState.Received("RCPT-2026-1101-002", "T-01"))
    try {
        rogue.dispatch(FlowAction.Issue("RPT-2026-0032"))
        println("[异常] 不该走到这里：非法跃迁未被拦截")
    } catch (e: IllegalFlowTransition) {
        println("[拦截] 非法跃迁被拒：${e.from} 不直达 issuance")
        println("       （异常原文：${e.message}）")
    }

    // ── 回执 c：越权被拦（sampler 请求 report:issue） ──────────────
    println()
    println("== 回执 C｜越权拦截 ==")
    val sampler = Role("sampler", setOf("sample:write"))
    try {
        requirePermission(sampler, "report:issue")
        println("[异常] 不该走到这里：越权请求未被拦截")
    } catch (e: ForbiddenException) {
        println("[拦截] 越权请求被拒：${e.role} 无 ${e.perm}")
        println("       （异常原文：${e.message}）")
    }

    // 三段回执打完，正常结束
}
