# ch13 generics-delegate-demo 实测回执

> 版本口径：Kotlin 2.4.21 / JDK 21（jvmToolchain）/ Gradle 9.8.1 系统直跑（不用 wrapper）。
> 以下全部回执由实测臂回填，coder 臂不产出任何运行输出。
> 体例：工程路径缩写为 `lab/ch13/generics-delegate-demo`；每条回执保留命令行、完整输出与 EXIT 值，逐字记录（`Picked up JAVA_TOOL_OPTIONS` 行与配置缓存提示行省略）。
> 本工程实测零告警（ch12 的 shadow w: 一类编译器附提醒在本章工程不存在）。

## 章节对码表

| 清单 | 验证方式 | 回执 |
|------|----------|------|
| 清单13-1（反例：Java 通配符照抄编不过） | 反例临时并入 src 后 `gradle compileKotlin` 必须失败 | 见回执五（复绿见回执六） |
| 清单13-3（泛型函数与上界约束） | `gradle run --args="digest"` 直跑 | 见回执一 |
| 清单13-4（in/out 型变自设计） | `gradle run --args="variance"` 直跑 | 见回执二 |
| 清单13-6（reified 实化） | `gradle run --args="reified"` 直跑 | 见回执三 |
| 清单13-8（反例：普通泛型 is T 被擦除拦下） | 反例临时并入 src 后 `gradle compileKotlin` 必须失败 | 见回执七（复绿见回执八） |
| 清单13-10（by lazy 消除样板） | `gradle run --args="lazy"` 直跑 | 见回执四 |
| 入口分发（Main.kt） | 四个参数各跑一次，即回执一至四 | — |

## 回执一：清单13-3 泛型函数与上界约束（digest）

```
$ cd lab/ch13/generics-delegate-demo
$ export JAVA_TOOL_OPTIONS=-Dstdout.encoding=UTF-8
$ gradle clean compileKotlin ; echo "EXIT=$?"

> Task :clean UP-TO-DATE
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin

BUILD SUCCESSFUL in 14s
2 actionable tasks: 1 executed, 1 up-to-date
EXIT=0
```

```
$ gradle run --args="digest" ; echo "EXIT=$?"

> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin UP-TO-DATE
> Task :compileJava NO-SOURCE
> Task :processResources NO-SOURCE
> Task :classes UP-TO-DATE

> Task :run
文本消息摘要=[T-1, T-2]
系统通知摘要=[N-1, N-2]

BUILD SUCCESSFUL in 7s
2 actionable tasks: 1 executed, 1 up-to-date
EXIT=0
```

- 两行输出与预期逐字一致；编译零告警。

## 回执二：清单13-4 in/out 型变自设计（variance）

```
$ gradle run --args="variance" ; echo "EXIT=$?"

> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin UP-TO-DATE
> Task :compileJava NO-SOURCE
> Task :processResources NO-SOURCE
> Task :classes UP-TO-DATE

> Task :run
生产者协变赋值成功：N-1
消费者逆变赋值成功：收到 1 条文本消息

BUILD SUCCESSFUL in 7s
2 actionable tasks: 1 executed, 1 up-to-date
EXIT=0
```

- 两行输出与预期逐字一致；`in T` 型变类持有 `mutableListOf<T>` 私有字段编译通过（私有成员不进公共 API，型变检查不拦）。

## 回执三：清单13-6 reified 实化（reified）

```
$ gradle run --args="reified" ; echo "EXIT=$?"

> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin UP-TO-DATE
> Task :compileJava NO-SOURCE
> Task :processResources NO-SOURCE
> Task :classes UP-TO-DATE

> Task :run
正在按 TextMessage 筛选
文本消息=2 条
正在按 SystemNotice 筛选
系统通知=1 条

BUILD SUCCESSFUL in 8s
2 actionable tasks: 1 executed, 1 up-to-date
EXIT=0
```

- 四行输出与预期逐字一致；`is T`、`T::class.simpleName` 在 inline + reified 下编译通过。

## 回执四：清单13-10 by lazy 消除样板（lazy）

```
$ gradle run --args="lazy" ; echo "EXIT=$?"

> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin UP-TO-DATE
> Task :compileJava NO-SOURCE
> Task :processResources NO-SOURCE
> Task :classes UP-TO-DATE

> Task :run
创建完成，两者都还没算过 preview
手写样板版：预览：report.pdf 摘要
（现算一次：读取 report.pdf 的内容生成预览）
委托版第一次：预览：report.pdf 摘要
委托版第二次：预览：report.pdf 摘要

BUILD SUCCESSFUL in 7s
2 actionable tasks: 1 executed, 1 up-to-date
EXIT=0
```

- 五行输出与预期逐字一致；「（现算一次…）」全输出只出现一次（手写版 preview 先算不打印提示，lazy 版首次读取才触发一次，第二次命中缓存）。

## 回执五：清单13-1 反例编译失败（Java 通配符照抄）

```
$ cp negatives/BrokenForward.kt.txt src/main/kotlin/demo/BrokenForward.kt
$ gradle compileKotlin ; echo "EXIT=$?"

> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED

> Task :compileKotlin FAILED

[Incubating] Problems report is available at: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/build/reports/problems/problems-report.html
1 actionable task: 1 executed

e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:5:1 Function 'forward' must have a body.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:5:30 Syntax error: Type expected.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:5:30 Syntax error: Incomplete code.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:5:31 Syntax error: Expecting a '>'.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:5:32 An explicit type is required on a value parameter.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:5:39 Syntax error: Expecting comma or ')'.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:5:40 An explicit type is required on a value parameter.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:5:47 Syntax error: Expecting comma or ')'.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:5:47 Syntax error: Expecting ')'.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:5:48 Syntax error: Expecting a top level declaration.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:5:50 Syntax error: Expecting a top level declaration.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:5:53 Syntax error: Expecting a top level declaration.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:5:55 Syntax error: Expecting a top level declaration.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:5:66 Syntax error: Expecting a top level declaration.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:5:67 Syntax error: Expecting a top level declaration.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:5:69 Syntax error: Expecting a top level declaration.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:5:75 Syntax error: Expecting a top level declaration.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:5:86 Syntax error: Expecting a top level declaration.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:5:87 Syntax error: Expecting a top level declaration.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:5:89 Syntax error: Expecting a top level declaration.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:5:89 Function declaration must have a name.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:6:18 Method 'iterator()' is ambiguous for this expression. Applicable candidates:
fun <T> Enumeration<T>.iterator(): Iterator<T>
fun <T> Iterator<T>.iterator(): Iterator<T>
fun <K, V> Map<out K, V>.iterator(): Iterator<Map.Entry<K, V>>
fun <K, V> MutableMap<K, V>.iterator(): Iterator<MutableMap.MutableEntry<K, V>>
fun CharSequence.iterator(): CharIterator
fun BufferedInputStream.iterator(): ByteIterator
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:6:18 Unresolved reference 'src'.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:6:23 Unresolved reference 'dst'.

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':compileKotlin' (registered by plugin 'org.jetbrains.kotlin.jvm').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: NON_MEMBER_FUNCTION_NO_BODY
       Kotlin compiler error
         Function 'forward' must have a body.
         Location: D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/BrokenForward.kt:5

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights into a Build Scan (powered by Develocity).
> Get more help at https://help.gradle.org.

BUILD FAILED in 7s
EXIT=1
```

- EXIT=1；首因是第 5 行第 1 列 `Function 'forward' must have a body.`——K2 从 `MutableList<?` 的 `?` 起整个签名解析崩塌，后面 20 余条 e: 全是同一根因的连带雪崩（输出经 `e:` 在 stderr、任务播报在 stdout 分流，回执按原顺序重排拼回）。
- 正文引用建议只取三条代表行：`5:1 Function 'forward' must have a body.`、`5:30 Syntax error: Type expected.`、`5:31 Syntax error: Expecting a '>'.`，不必全录雪崩。

## 回执六：反例一移除后复绿

```
$ rm src/main/kotlin/demo/BrokenForward.kt
$ gradle clean compileKotlin ; echo "EXIT=$?"

> Task :clean
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin

BUILD SUCCESSFUL in 8s
2 actionable tasks: 2 executed
EXIT=0
```

- EXIT=0；clean 全量重编译复绿坐实（非 UP-TO-DATE，2 executed）。

## 回执七：清单13-8 反例编译失败（普通泛型 is T 被擦除拦下）

```
$ cp negatives/PlainOfType.kt.txt src/main/kotlin/demo/PlainOfType.kt
$ gradle compileKotlin ; echo "EXIT=$?"

> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED

> Task :compileKotlin FAILED

[Incubating] Problems report is available at: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/build/reports/problems/problems-report.html
1 actionable task: 1 executed

e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/PlainOfType.kt:6:20 Cannot check for instance of erased type 'T (of fun <T : Message> List<Message>.ofTypePlain)'.

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':compileKotlin' (registered by plugin 'org.jetbrains.kotlin.jvm').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: CANNOT_CHECK_FOR_ERASED
       Kotlin compiler error
         Cannot check for instance of erased type 'T (of fun <T : Message> List<Message>.ofTypePlain)'.
         Location: D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch13/generics-delegate-demo/src/main/kotlin/demo/PlainOfType.kt:6

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights into a Build Scan (powered by Develocity).
> Get more help at https://help.gradle.org.

BUILD FAILED in 8s
EXIT=1
```

- EXIT=1；唯一一条 e: 在 PlainOfType.kt:6:20，报错原文为 `Cannot check for instance of erased type 'T (of fun <T : Message> List<Message>.ofTypePlain)'.`——K2 措辞比旧版多带类型参数归属（括号内全限定函数签名），正文引用时照录。
- `it as T` 那半句没报错：未检查转换只是告警级，真正的硬拦截落在 `it is T`。

## 回执八：反例二移除后复绿

```
$ rm src/main/kotlin/demo/PlainOfType.kt
$ gradle clean compileKotlin ; echo "EXIT=$?"

> Task :clean
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin

BUILD SUCCESSFUL in 10s
2 actionable tasks: 2 executed
EXIT=0
```

- EXIT=0；clean 全量重编译复绿坐实。至此工程全绿收尾。
