// (a) 裸奔：平台类型当非空用，编译器不拦，null 在成员访问处当场炸 NPE
try {
    val length = legacyStore.findNickname("u404").length  // findNickname 返回 String!，这行照常编译
    println("[p09] (a) 裸奔侥幸成功：length=$length")
} catch (e: NullPointerException) {
    println("[p09] (a) 裸奔下场：${e::class.simpleName} —— null 喂进非空位，运行期才暴露")
}
// (b) 防御性收窄：在赋值点显式标注 String?，之后 ?. 与 ?: 接管（第 4 章安全调用决策框架口径）
val nickname: String? = legacyStore.findNickname("u404")
println("[p09] (b) 防御收窄：${nickname?.let { "昵称=$it" } ?: "查无此人，走兜底昵称「访客」"}")
// (c) 边界收口：包装函数签名上显式声明可空性，平台类型被封死在这一个函数里
println("[p09] (c) 边界收口 findNicknameOrNull(\"u1001\")：${findNicknameOrNull("u1001")}")