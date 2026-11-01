// (a) 冷流 flow{}：两个收集者先后 collect，上游跑两遍
val cold = flow {
    coldTl.mark("冷流上游：开始生产")
    emit(1)
    delay(COLD_EMIT_MS)
    emit(2)
    delay(COLD_EMIT_MS)
    emit(3)
}
coldTl.mark("收集者一 collect")
cold.collect { coldTl.mark("收集者一拿到：$it") }
coldTl.mark("收集者二 collect")
cold.collect { coldTl.mark("收集者二拿到：$it") }

// (b) 热流 MutableStateFlow：上游只生产（赋值）一遍，A 全程在收，B 迟到
val hot = MutableStateFlow(0)
val a = launch { hot.collect { hotTl.mark("订阅者 A 收到：$it") } }
delay(HOT_EMIT_MS)   // A 先就位（A 已收到初始值 0）
hot.value = 1
delay(HOT_EMIT_MS)
hot.value = 2
delay(HOT_EMIT_MS)   // 此刻第 2 个值已发出，B 才来订阅
val b = launch { hot.collect { hotTl.mark("订阅者 B 收到：$it") } }
delay(HOT_EMIT_MS)
hot.value = 3
delay(HOT_EMIT_MS)
a.cancel()
b.cancel()