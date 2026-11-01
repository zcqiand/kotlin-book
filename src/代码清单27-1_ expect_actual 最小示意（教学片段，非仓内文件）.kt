// 共享层：expect 只声明「这里有一道平台缺口」，不带实现
expect fun platformName(): String

// Android 目标：actual 补上这份目标的真实实现
actual fun platformName(): String = "Android"

// iOS 目标：同一个名字，另一份实现
actual fun platformName(): String = "iOS"