// (a) async 版：三个 async 同时发车，await 逐个取值汇总
val deferred = P06_SUBJECTS.map { subject -> async { fetchScore(subject) } }
val total = deferred.sumOf { it.await() }

// (a-对照) 三个取值放进同一个循环顺序 await：并发发生在「多个 async」之间，
// 同一个循环里照样排队，退化成三个一秒相加
for (subject in P06_SUBJECTS) {
    serialTotal += async { fetchScore(subject) }.await()
}

// (b) launch 版：结果只进日志，返回值没人要，joinAll 等收尾
val jobs = P06_SUBJECTS.map { subject ->
    launch { tl.mark("(b) launch 副作用任务「$subject」干完，无返回值可接") }
}
jobs.joinAll()