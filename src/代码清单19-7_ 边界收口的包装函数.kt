// 边界收口的落点：可空性写进签名，调用方从签名就读得到真相，再也接触不到平台类型
fun findNicknameOrNull(userId: String): String? = legacyStore.findNickname(userId)