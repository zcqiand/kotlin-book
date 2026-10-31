package lab.ch16

import kotlinx.coroutines.delay

/**
 * P05 编译反例（待修复态）：普通函数直接调 delay。
 * 这个文件故意停留在编译不过的状态跑一轮 compileKotlin 拿逐字报错，
 * 报错留档 receipts.md 后，本文件才被修复为 suspend 版复绿。
 */
fun notReallySuspending() {
    // delay 是 suspend 函数，普通函数体内没有挂起语境，编译器在此拦截
    delay(1000)
}
