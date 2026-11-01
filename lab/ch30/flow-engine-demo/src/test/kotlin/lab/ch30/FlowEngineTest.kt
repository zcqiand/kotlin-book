package lab.ch30

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FlowEngineTest {

    private fun freshEngine() = FlowEngine(FlowState.Received("RCPT-T-001", "T-01"))

    /** 主链全通：八站走完落在 completed */
    @Test
    fun mainChainReachesCompleted() {
        val engine = freshEngine()
        engine.dispatch(FlowAction.Assign("李检测"))
        engine.dispatch(FlowAction.EnterData(12))
        engine.dispatch(FlowAction.Review("王复核", "pass"))
        engine.dispatch(FlowAction.Approve("赵主任"))
        engine.dispatch(FlowAction.Issue("RPT-2026-0031"))
        engine.dispatch(FlowAction.Archive("ARC-2026-0007"))
        engine.dispatch(FlowAction.Complete("2026-11-01T17:30"))
        assertEquals("completed", contractStatus(engine.current()))
    }

    /** 退回支线：复核不通过（verdict=reject）走 review → data_entry */
    @Test
    fun reviewRejectReturnsToDataEntry() {
        val engine = freshEngine()
        engine.dispatch(FlowAction.Assign("李检测"))
        engine.dispatch(FlowAction.EnterData(12))
        engine.dispatch(FlowAction.Review("王复核", "reject"))
        assertEquals("review", contractStatus(engine.current()))
        val returned = engine.dispatch(FlowAction.Return("3 条记录超差未标注", 9))
        assertEquals("data_entry", contractStatus(returned))
        assertEquals(9, (returned as FlowState.DataEntered).recordCount) // 携带修订后的记录数
    }

    /** 复核通过（verdict=pass）不允许退回——Return 只对 reject 放行 */
    @Test
    fun returnBlockedWhenVerdictPass() {
        val engine = freshEngine()
        engine.dispatch(FlowAction.Assign("李检测"))
        engine.dispatch(FlowAction.EnterData(12))
        engine.dispatch(FlowAction.Review("王复核", "pass"))
        val e = assertFailsWith<IllegalFlowTransition> {
            engine.dispatch(FlowAction.Return("不应放行", 0))
        }
        assertEquals("review", e.from)
    }

    /** 非法跃迁抛异常：receiving 直跳 issuance 被拒 */
    @Test
    fun illegalTransitionThrows() {
        val engine = freshEngine()
        val e = assertFailsWith<IllegalFlowTransition> {
            engine.dispatch(FlowAction.Issue("RPT-2026-0032"))
        }
        assertEquals("receiving", e.from)
        assertEquals("issue", e.action)
        assertEquals("issuance", e.target)
    }

    /** 越权抛异常：sampler（只有 sample:write）请求 report:issue 被拒 */
    @Test
    fun forbiddenThrows() {
        val sampler = Role("sampler", setOf("sample:write"))
        val e = assertFailsWith<ForbiddenException> {
            requirePermission(sampler, "report:issue")
        }
        assertEquals("sampler", e.role)
        assertEquals("report:issue", e.perm)
    }
}
