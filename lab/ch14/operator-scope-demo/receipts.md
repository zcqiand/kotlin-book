# ch14 operator-scope-demo Gradle 实测回执

- 日期：2026-10-31
- 环境：Kotlin 2.4.21 / JDK 21 / 系统 Gradle 9.8.1 直跑（无 wrapper）/ Windows 11 / PowerShell
- 前置：每次运行前设 `$env:JAVA_TOOL_OPTIONS='-Dstdout.encoding=UTF-8'`
- 说明：`gradle run --args="..." --console=plain -q` 合用正常，-q 抑制了任务播报，程序 stdout 完整逐字。JVM 启动会打印 `Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8`（gradle run 时出现两行：daemon 一行 + 应用 JVM 一行），下文各回执中从略，中文输出确认无乱码。

## ① 正向编译（gradle compileKotlin）

- exit code：0

```
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin

BUILD SUCCESSFUL in 12s
1 actionable task: 1 executed
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

## ② batch 运行回执（gradle run --args="batch" --console=plain -q）

- exit code：0
- 程序 stdout（逐字）：

```
合并后 3 条：[T-1, T-2, N-1]
第 2 条=T-2
```

## ③ infix 运行回执（gradle run --args="infix" --console=plain -q）

- exit code：0
- 程序 stdout（逐字）：

```
中缀合并后 2 条：[T-3, N-2]
```

## ④ scopes 运行回执（gradle run --args="scopes" --console=plain -q）

- exit code：0
- 程序 stdout（逐字）：

```
摘要=系统维护｜今晚 22:00 升级，期间暂停收发
草稿就绪：系统维护
also 返回的还是原对象=true
let 收到：今晚 22:00 升级，期间暂停收发
run 算得总字数=22
```

## ⑤ drill 运行回执（gradle run --args="drill" --console=plain -q）

- exit code：0
- 程序 stdout（逐字）：

```
准备完成：欢迎加入
带签名=你好，U-1｜签名：研发团队
准备完成：欢迎加入
不带签名=你好，U-2
```

## ⑥ 反例一失败回执（negatives/WrongOpName.kt 临时并入编译）

操作：`Copy-Item negatives\WrongOpName.kt src\main\kotlin\demo\WrongOpName.kt` 后 `gradle compileKotlin`。

- exit code：1
- 编译器报错行（逐字）：

```
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch14/operator-scope-demo/src/main/kotlin/demo/WrongOpName.kt:4:1 'operator' modifier is not applicable to function: illegal function name.
```

- 诊断摘要行（逐字）：

```
   > Kotlin compiler: INAPPLICABLE_OPERATOR_MODIFIER
       Kotlin compiler error
         'operator' modifier is not applicable to function: illegal function name.
         Location: D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch14/operator-scope-demo/src/main/kotlin/demo/WrongOpName.kt:4
```

- 移出后复绿：`Move-Item` 移回 negatives/，`Get-ChildItem src\main\kotlin\demo` 确认仅剩 `Main.kt`（见⑦同批输出），随后按反例二流程继续；终态复绿见⑧。

## ⑦ 反例二失败回执（negatives/TwoParamInfix.kt 临时并入编译）

操作：移出 WrongOpName.kt 后，`Copy-Item negatives\TwoParamInfix.kt src\main\kotlin\demo\TwoParamInfix.kt` 后 `gradle compileKotlin`。移出确认输出：

```
Name
----
Main.kt
```

- exit code：1
- 编译器报错行（逐字）：

```
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch14/operator-scope-demo/src/main/kotlin/demo/TwoParamInfix.kt:4:1 'infix' modifier is inapplicable to this function.
```

- 诊断摘要行（逐字）：

```
   > Kotlin compiler: INAPPLICABLE_INFIX_MODIFIER
       Kotlin compiler error
         'infix' modifier is inapplicable to this function.
         Location: D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch14/operator-scope-demo/src/main/kotlin/demo/TwoParamInfix.kt:4
```

## ⑧ 终态复绿确认（gradle clean compileKotlin）

操作：`Move-Item` 将 TwoParamInfix.kt 移回 negatives/，`Get-ChildItem src\main\kotlin\demo` 确认仅剩 `Main.kt`，然后 `gradle clean compileKotlin`。

- exit code：0

```
> Task :clean
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin

BUILD SUCCESSFUL in 10s
2 actionable tasks: 2 executed
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

---

## 汇总

| 步骤 | 结果 | exit code |
|------|------|-----------|
| ① compileKotlin | BUILD SUCCESSFUL | 0 |
| ② batch | 输出正常 | 0 |
| ③ infix | 输出正常 | 0 |
| ④ scopes | 输出正常 | 0 |
| ⑤ drill | 输出正常 | 0 |
| ⑥ 反例一 WrongOpName.kt | 编译失败 INAPPLICABLE_OPERATOR_MODIFIER @ 4:1 | 1 |
| ⑦ 反例二 TwoParamInfix.kt | 编译失败 INAPPLICABLE_INFIX_MODIFIER @ 4:1 | 1 |
| ⑧ clean compileKotlin | BUILD SUCCESSFUL（demo/ 仅 Main.kt） | 0 |

零代码改动（无任何拼写/语法级修正）。
