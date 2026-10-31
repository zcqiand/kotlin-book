# java-interop-demo 实测回执（ch19）

环境：系统 Gradle 9.8.1 直跑（不用 wrapper），`JAVA_TOOL_OPTIONS=-Dstdout.encoding=UTF-8`，
Kotlin 2.4.21 + jvmToolchain(21)（与主仓 `output/xr-know-020/version-lock.json` 的
kotlin 2.4.21 / jdk 21 / gradle 9.8.1 同口径）。本章无第三方依赖（纯 Kotlin 标准库 + JDK）。

> 工程修正备注（实测中发现并修复，非 runbook 预案，共两处）：
>
> 1. 混编方向默认不通：Gradle 默认让 javac（compileJava）跑在 Kotlin 编译之前，
>    Java→Kotlin 方向直接「找不到符号」。build.gradle.kts 里显式
>    `compileJava` dependsOn `compileKotlin` 并把 Kotlin 产物挂进 javac classpath 后打通。
>
>    **【更正注 2026-11-01 · fact-checker 对照实验，原记录保留不删】** 上面这条的归因有误：
>    Kotlin/JVM 插件自 1.1.60（KT-15620）起默认就让 compileJava 依赖 compileKotlin、
>    并把 Kotlin 编译产物挂进 javac 的 classpath，Java→Kotlin 方向默认即通，无需手动挂接。
>    对照实测：本工程原样副本去除下方 wiring 后干净构建（留档
>    `lab/ch19/_work/interop-default-order-test/`），任务序仍为 compileKotlin→compileJava，
>    JavaCaller.java（import demo.* 五个 Kotlin 类）直接编译通过，四演示输出与本回执
>    逐字一致全绿，EXIT=0。下方显式 wiring 保留，定位为「插件默认行为的可读性冗余保险」。
>
> 2. 迁移类首跑炸出：Invoice 主构造带默认参数 `memo`，但默认参数**不生成重载链**，
>    Java 侧 `new Invoice("老陈", 12900)` 两参调用点编译失败
>    （`错误: 无法将类 Invoice中的构造器 Invoice应用到给定类型; 需要: String,int,String 找到: String,int`）。
>    最小修复：构造器加 `@JvmOverloads`，Java 调用方两档实参才都编译通过——
>    该失败本身就是清单 19-8 机制在迁移场景的反向教材，正文可引用。

## 全绿回执（四演示）

命令：`gradle run --rerun-tasks`（工程：lab/ch19/java-interop-demo）

```text
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :processResources NO-SOURCE
> Task :compileKotlin
> Task :compileJava
> Task :classes

> Task :run
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
========== 演示 2: twoWayDemo（双向互调：Kotlin 调 Java / Java 调 Kotlin） ==========
[two-way] Kotlin 写 Java 属性：displayName=老陈，level=9
[two-way] Java 静态方法直调：LegacyUserStore.versionTag() = legacy-store-1.0
[two-way] Java 调 Kotlin：你好，Java调用方（来自 Kotlin） | region=cn-south | count=2
---------- 演示 2 结束 ----------
========== 演示 3: p09DrillDemo（平台类型防御：String! 的坑位地图） ==========
[p09] (a) 裸奔下场：NullPointerException —— null 喂进非空位，运行期才暴露
[p09] (b) 防御收窄：查无此人，走兜底昵称「访客」
[p09] (c) 边界收口 findNicknameOrNull("u1001")：老陈
---------- 演示 3 结束 ----------
========== 演示 4: jvmAnnotationsDemo（@Jvm 注解：给 Java 调用方一份好接口） ==========
[jvm] companion 两态：TokenStore.Companion.defaultIssuer()=kotlin-book，Session.create()→Session 实例（maxRetry=3）
[jvm] @JvmOverloads 重载链：InvoiceQuery(owner=老陈, page=1, pageSize=20) | InvoiceQuery(owner=老陈, page=2, pageSize=20) | InvoiceQuery(owner=老陈, page=2, pageSize=50)
[jvm] @JvmField 字段直访：device.serial 直接摸字段 = SN-2026-0042
---------- 演示 4 结束 ----------
========== 演示 5: migrateDemo（增量迁移：LegacyInvoice → Invoice） ==========
[migrate] Kotlin 侧两档构造：Invoice(owner=老陈, amountCents=12900, memo='<空>')
[migrate]              　　Invoice(owner=老陈, amountCents=12900, memo='十一月书费')
[migrate] Java 侧复调（调用形态不变）：老陈/12900/ | 老陈/12900/十一月书费（已支付）
[migrate] require 校验接管：金额不能为负：-1
---------- 演示 5 结束 ----------

BUILD SUCCESSFUL in 7s
3 actionable tasks: 3 executed
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

EXIT=0。

说明：本章演示 1（interopLayoutDemo）不对应运行输出——混编结构本身由工程目录承载
（src/main/java/legacy/ 存量不动，src/main/kotlin/demo/ 新代码），正文清单 19-1 为目录树 text 块。

## 反例回执（@JvmField 标在 private 属性上 → 编译失败）

在 JvmShowcase.kt 临时追加反例类后跑 `gradle compileKotlin`：

```text
> Task :compileKotlin FAILED
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/output/xr-know-020/code/kotlin-book/lab/ch19/java-interop-demo/src/main/kotlin/demo/JvmShowcase.kt:45:5 JvmField has no effect on a private property.

FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':compileKotlin' (registered by plugin 'org.jetbrains.kotlin.jvm').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: INAPPLICABLE_JVM_FIELD
       Kotlin compiler error
         JvmField has no effect on a private property.
         Location: D:/qiand-life/1-projects/xr-knowledge-suite/output/xr-know-020/code/kotlin-book/lab/ch19/java-interop-demo/src/main/kotlin/demo/JvmShowcase.kt:45
```

EXIT=1。

反例代码（已移出，原文留档）：

```kotlin
// 反例（演示后即移出）：@JvmField 标在 private 属性上
class BrokenField {
    @JvmField
    private val cacheKey: String = "device-cache"
}
```

要点：报错口径是「no effect on a private property」而非「不允许 private」——
注解对 private 属性不适用（INAPPLICABLE_JVM_FIELD），编译器直接拒编。
private 属性本来就不暴露给 Java，`@JvmField`「以公共字段形态暴露」的前提不成立，
所以这不是警告而是错误。

## 复绿记录

移出反例类后 `gradle compileKotlin`：

```text
BUILD SUCCESSFUL in 6s
1 actionable task: 1 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

EXIT=0。工程终态编译通过。

## 迁移等价性口径（演示 5 补充）

- 「调用方不改一行」的精确含义：Java 调用点的**调用形态**（两档构造实参数量、
  getOwner/getAmountCents/getMemo、setMemo）与当年调 LegacyInvoice 完全同形，
  JavaCaller.callInvoice() 实测编译运行通过；类名与 import 需指向新类，这是迁移不可避免的一处替换。
- require 校验是迁移**增强项**：常规调用面行为不变，仅新增「负数金额在构造期拒绝」。
- 修正备注 2 的教训归清单 19-8：默认参数是 Kotlin 侧的便利，Java 侧的兼容靠 @JvmOverloads 兑现。

## 可复跑性口径

- 演示输出全为确定性 println，无时间戳打点，逐字稳定可复现。
- 反例类已从工程移出，工程终态四演示全绿；复现反例按上文留档代码临时追加即可。
