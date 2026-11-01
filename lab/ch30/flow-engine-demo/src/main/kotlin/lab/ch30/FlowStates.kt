/**
 * ch30 lab｜密封类状态机建模检测任务流转。
 *
 * 对码来源：案例仓 lab-management-system-kotlin
 * `src/main/kotlin/io/xr/lab/shared/models/FlowStatus.kt` L21-43——
 * enum FlowStatus 八值：receiving, task_assignment, data_entry, review,
 * approval, issuance, archived, completed。
 *
 * 教学差异：案例仓用 enum 建模同一契约，八值彼此等价、不携带数据；本文件改用
 * 密封类——每个状态可携带自己的异质数据载体（Received(receiptId, tenantId)、
 * Issued(reportNo)……），这是密封类相对枚举的核心增量：状态即数据，
 * 跃迁时顺带换载体，消费端靠 when 穷举解构。
 */
package lab.ch30

sealed class FlowState {
    /** 收样登记：受理回执号 + 租户 */
    data class Received(val receiptId: String, val tenantId: String) : FlowState()

    /** 任务分派：指派检测员 */
    data class TaskAssigned(val assignee: String) : FlowState()

    /** 数据录入：已录记录数 */
    data class DataEntered(val recordCount: Int) : FlowState()

    /** 复核：复核人 + 结论（pass / reject） */
    data class Reviewed(val reviewer: String, val verdict: String) : FlowState()

    /** 审批：审批人 */
    data class Approved(val approver: String) : FlowState()

    /** 签发：报告编号 */
    data class Issued(val reportNo: String) : FlowState()

    /** 归档：档案号 */
    data class Archived(val archiveNo: String) : FlowState()

    /** 完成闭环：闭环时间戳 */
    data class Completed(val finishedAt: String) : FlowState()
}

/**
 * 密封类穷举：返回对码案例仓 FlowStatus 的契约状态串。
 * when 不带 else——缺任一分支编译器即报错（密封类穷举检查生效）。
 */
fun contractStatus(state: FlowState): String = when (state) {
    is FlowState.Received -> "receiving"
    is FlowState.TaskAssigned -> "task_assignment"
    is FlowState.DataEntered -> "data_entry"
    is FlowState.Reviewed -> "review"
    is FlowState.Approved -> "approval"
    is FlowState.Issued -> "issuance"
    is FlowState.Archived -> "archived"
    is FlowState.Completed -> "completed"
}

/**
 * 密封类穷举展示函数：解构各状态自己的数据载体成读数串。
 * when 不带 else——新增状态时此处编译失败，逼着补读数格式。
 */
fun describe(state: FlowState): String = when (state) {
    is FlowState.Received -> "收样回执=${state.receiptId}/租户=${state.tenantId}"
    is FlowState.TaskAssigned -> "检测员=${state.assignee}"
    is FlowState.DataEntered -> "已录记录=${state.recordCount}条"
    is FlowState.Reviewed -> "复核=${state.reviewer}/结论=${state.verdict}"
    is FlowState.Approved -> "审批人=${state.approver}"
    is FlowState.Issued -> "报告编号=${state.reportNo}"
    is FlowState.Archived -> "档案号=${state.archiveNo}"
    is FlowState.Completed -> "闭环时间=${state.finishedAt}"
}
