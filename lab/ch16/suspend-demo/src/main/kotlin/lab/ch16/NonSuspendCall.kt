package lab.ch16

import kotlinx.coroutines.delay

/**
 * P05 编译反例（已修复态）：修复方式 = 给函数标 suspend，为 delay 提供挂起语境。
 * 修复前的原始失败版本逐字留档在 compile-fail/NonSuspendCall.before-fix.kt，
 * 对应的 exit 1 逐字报错见 receipts.md。
 */
suspend fun notReallySuspending() {
    // 修复后此处合法：suspend 函数体内可以调 suspend 函数
    delay(1000)
}
