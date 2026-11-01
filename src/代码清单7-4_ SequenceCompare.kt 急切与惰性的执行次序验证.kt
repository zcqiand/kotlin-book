package collections

fun main() {
    // 固定数据保证输出可复测：读者照着清单跑，每一行都该和书上一样
    val numbers = listOf(-2, 5, -7, 8, -1, 3, -4, 6)

    // ---------- 急切版：每一步都是一次完整遍历 ----------
    // filter 先吃掉整个列表产出新列表，map 再吃掉那个新列表——
    // println 标记会显示：前 8 行全是 filter，后 4 行全是 map，两个阶段泾渭分明
    println("== 急切版：先走完 filter，再走 map ==")
    val eagerResult = numbers
        .filter { println("filter 处理 $it"); it > 0 }
        .map { println("map 处理 $it"); it * 10 }
        .take(2)
    println("急切版结果: $eagerResult")

    // ---------- 惰性版：asSequence 让元素逐个流过整条链 ----------
    // 同一条链只多加了 asSequence()，println 标记却变成 filter/map 交替——
    // 因为每个元素被推着走完全程，而不是每一步等全体到齐
    println()
    println("== 惰性版：asSequence 后逐元素流过整条链 ==")
    val lazyResult = numbers.asSequence()
        .filter { println("filter 处理 $it"); it > 0 }
        .map { println("map 处理 $it"); it * 10 }
        .take(2)
        .toList()   // 序列是「承诺」，toList 才真正开始逐个要结果
    println("惰性版结果: $lazyResult")

    // ---------- 短路版：first 找到即停，后面的元素根本不进链 ----------
    println()
    println("== 短路版：first 命中即停 ==")
    val shortHit = numbers.asSequence()
        .filter { println("filter 处理 $it"); it > 0 }
        .map { println("map 处理 $it"); it * 10 }
        .first { it > 40 }
    println("短路版命中: $shortHit")

    // 两条路线风格不同，答案必须相同——这是验证执行次序改动没改坏结果的底线
    println()
    println("[验证] 急切版与惰性版最终结果相同: ${eagerResult == lazyResult}")
}