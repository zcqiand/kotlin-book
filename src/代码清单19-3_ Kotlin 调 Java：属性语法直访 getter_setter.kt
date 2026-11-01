// Kotlin 调 Java：getter/setter 在这里就是属性语法，赋值/取值两行编译后走 set/get 对
legacyStore.displayName = "  老陈  "   // 实际走 setDisplayName，Java 侧会 trim
legacyStore.level = 9                  // 实际走 setLevel(9)
println("[two-way] Kotlin 写 Java 属性：displayName=${legacyStore.displayName}，level=${legacyStore.level}")
// 静态工具方法：Kotlin 侧直接类名调用，跟调用 Kotlin object 没有手感差别
println("[two-way] Java 静态方法直调：LegacyUserStore.versionTag() = ${LegacyUserStore.versionTag()}")