/**
 * ch30 lab｜合法跃迁表 + 动作处理。
 *
 * 主链（对码案例仓 FlowStatus 八态）：receiving → task_assignment → data_entry
 * → review → approval → issuance → archived → completed；
 * 退回支线：review → data_entry（复核不通过退回录入，verdict=reject 才放行）。
 *
 * 实现要点：dispatch 用 when(state) 穷举外层状态（无 else，密封类穷举检查生效），
 * 内层 when(action) 只放行该状态的合法后继，其余一律抛 IllegalStateException
 * 子类 [IllegalFlowTransition]。
 */
package lab.ch30

/** 非法跃迁：from=当前状态（契约串），action=动作名，target=该动作想去的后继态 */
class IllegalFlowTransition(
    val from: String,
    val action: String,
    val target: String,
) : IllegalStateException("非法跃迁：$from 不允许执行 $action（$from 不可直达 $target）")

/** 动作：label=动作名，target=对码 FlowStatus 的后继态契约串 */
sealed class FlowAction(val label: String, val target: String) {
    /** 分派检测任务 → task_assignment */
    data class Assign(val assignee: String) : FlowAction("assign", "task_assignment")

    /** 录入检测数据 → data_entry */
    data class EnterData(val recordCount: Int) : FlowAction("enter_data", "data_entry")

    /** 复核 → review */
    data class Review(val reviewer: String, val verdict: String) : FlowAction("review", "review")

    /** 审批 → approval */
    data class Approve(val approver: String) : FlowAction("approve", "approval")

    /** 签发 → issuance */
    data class Issue(val reportNo: String) : FlowAction("issue", "issuance")

    /** 归档 → archived */
    data class Archive(val archiveNo: String) : FlowAction("archive", "archived")

    /** 完成闭环 → completed */
    data class Complete(val finishedAt: String) : FlowAction("complete", "completed")

    /** 复核不通过退回录入 → data_entry（仅 verdict=reject 时合法） */
    data class Return(val reason: String, val revisedCount: Int) : FlowAction("return", "data_entry")
}

class FlowEngine(start: FlowState) {
    private var state: FlowState = start

    fun current(): FlowState = state

    /** 动作处理：合法则跃迁并返回新状态，非法抛 [IllegalFlowTransition] */
    fun dispatch(action: FlowAction): FlowState {
        state = when (val s = state) { // 外层穷举状态，无 else
            is FlowState.Received -> when (action) {
                is FlowAction.Assign -> FlowState.TaskAssigned(action.assignee)
                else -> throw IllegalFlowTransition(contractStatus(s), action.label, action.target)
            }
            is FlowState.TaskAssigned -> when (action) {
                is FlowAction.EnterData -> FlowState.DataEntered(action.recordCount)
                else -> throw IllegalFlowTransition(contractStatus(s), action.label, action.target)
            }
            is FlowState.DataEntered -> when (action) {
                is FlowAction.Review -> FlowState.Reviewed(action.reviewer, action.verdict)
                else -> throw IllegalFlowTransition(contractStatus(s), action.label, action.target)
            }
            is FlowState.Reviewed -> when (action) {
                is FlowAction.Approve -> FlowState.Approved(action.approver)
                is FlowAction.Return ->
                    if (s.verdict == "reject") FlowState.DataEntered(action.revisedCount)
                    else throw IllegalFlowTransition(contractStatus(s), action.label, action.target)
                else -> throw IllegalFlowTransition(contractStatus(s), action.label, action.target)
            }
            is FlowState.Approved -> when (action) {
                is FlowAction.Issue -> FlowState.Issued(action.reportNo)
                else -> throw IllegalFlowTransition(contractStatus(s), action.label, action.target)
            }
            is FlowState.Issued -> when (action) {
                is FlowAction.Archive -> FlowState.Archived(action.archiveNo)
                else -> throw IllegalFlowTransition(contractStatus(s), action.label, action.target)
            }
            is FlowState.Archived -> when (action) {
                is FlowAction.Complete -> FlowState.Completed(action.finishedAt)
                else -> throw IllegalFlowTransition(contractStatus(s), action.label, action.target)
            }
            is FlowState.Completed ->
                throw IllegalFlowTransition(contractStatus(s), action.label, action.target)
        }
        return state
    }
}
