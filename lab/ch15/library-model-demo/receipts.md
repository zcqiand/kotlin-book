# library-model-demo 实测回执（ch15）

环境：系统 Gradle 9.8.1 直跑（不用 wrapper），`JAVA_TOOL_OPTIONS=-Dstdout.encoding=UTF-8`。

> 工程修正备注（实测中发现并修复，非 runbook 预案）：原 `CopyTest.kt` 第 5 行
> `import org.junit.jupiter.api.assertFailsWith` 编译不过——JUnit Jupiter 6.1.3 的
> `AssertionsKt` 只有 `assertThrows` / `assertThrowsExactly` / `assertDoesNotThrow`，
> 没有 `assertFailsWith`（已实捕：`Unresolved reference 'assertFailsWith'`，
> CopyTest.kt:5:30 / 35:9，BUILD FAILED）。该符号属 `kotlin.test`。最小修复：
> import 改为 `import kotlin.test.assertFailsWith`（调用点逐字不变），并在
> `build.gradle.kts` 补 `testImplementation(kotlin("test"))`（不写版本号，跟随
> Kotlin 插件 2.4.21，与 version-lock 一致；junit 6.1.3 未动）。章节正文若引用
> 本测试，须按 `kotlin.test.assertFailsWith` 口径写。

## 红灯回执（overdueDays 未收口）

命令：`gradle test --rerun-tasks`（工程：lab/ch15/library-model-demo）

改动：`src/main/kotlin/library/LoanRules.kt` 第 18 行临时改为
`fun Borrowed.overdueDays(onDay: Int): Int = onDay - dueDay`（只去掉 coerceAtLeast 收口）。

```text
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
Starting a Gradle Daemon, 1 busy and 1 incompatible Daemons could not be reused, use --status for details
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :processResources NO-SOURCE
> Task :processTestResources NO-SOURCE
> Task :compileKotlin
> Task :compileJava NO-SOURCE
> Task :classes UP-TO-DATE
> Task :jar
> Task :compileTestKotlin
> Task :compileTestJava NO-SOURCE
> Task :testClasses UP-TO-DATE

> Task :test FAILED
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8

CopyTest > checkoutMovesStateToBorrowed() PASSED

CopyTest > bookEqualityFollowsFields() PASSED

CopyTest > reportLostMovesStateToLost() PASSED

CopyTest > giveBackReturnsToAvailable() PASSED

CopyTest > checkoutUnavailableCopyIsRejected() PASSED

LifecycleTest > fullLifecycleWalkthrough() PASSED

OverdueTest > dueDayIsLoanDayPlusLoanDays() PASSED

OverdueTest > overdueDaysIsZeroBeforeDueDay() FAILED
    org.opentest4j.AssertionFailedError at OverdueTest.kt:21

OverdueTest > isOverdueTracksOverdueDays() PASSED

OverdueTest > overdueDaysCountsDaysBeyondDueDay() PASSED

10 tests completed, 1 failed

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':test'.
> There were failing tests. See the report at: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch15/library-model-demo/build/reports/tests/test/index.html

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights from a Build Scan (powered by Develocity).

BUILD FAILED in 1m 53s
4 actionable tasks: 4 executed
```

失败详情（控制台只给单行堆栈，完整信息取自 build/test-results/test/TEST-library.OverdueTest.xml）：

```text
org.opentest4j.AssertionFailedError: expected: <0> but was: <-11>
    at ...
```

（借出第 1 天、onDay=20 时 20-31=-11，去收口后负数漏出，符合预期。）

## 全绿回执（复绿后）

命令：`gradle test --rerun-tasks`（工程：lab/ch15/library-model-demo）

复绿：第 18 行逐字还原为
`fun Borrowed.overdueDays(onDay: Int): Int = (onDay - dueDay).coerceAtLeast(0)`，
并经 Read 复核与原始文件逐字一致（第 17 行注释未动）。

```text
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :processResources NO-SOURCE
> Task :processTestResources NO-SOURCE
> Task :compileKotlin
> Task :compileJava NO-SOURCE
> Task :classes UP-TO-DATE
> Task :jar
> Task :compileTestKotlin
> Task :compileTestJava NO-SOURCE
> Task :testClasses UP-TO-DATE

> Task :test
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8

OverdueTest > dueDayIsLoanDayPlusLoanDays() PASSED

OverdueTest > overdueDaysIsZeroBeforeDueDay() PASSED

OverdueTest > isOverdueTracksOverdueDays() PASSED

OverdueTest > overdueDaysCountsDaysBeyondDueDay() PASSED

CopyTest > checkoutMovesStateToBorrowed() PASSED

CopyTest > bookEqualityFollowsFields() PASSED

CopyTest > reportLostMovesStateToLost() PASSED

CopyTest > giveBackReturnsToAvailable() PASSED

CopyTest > checkoutUnavailableCopyIsRejected() PASSED

LifecycleTest > fullLifecycleWalkthrough() PASSED

BUILD SUCCESSFUL in 11s
4 actionable tasks: 4 executed
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

10 个测试全过（CopyTest 5 + OverdueTest 4 + LifecycleTest 1），`overdueDaysIsZeroBeforeDueDay()` 由 FAILED 转 PASSED，收口逻辑验证闭环。
