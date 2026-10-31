# ch21 quality-gate-demo 实跑回执（receipts）

- 仓身份先验：`git rev-parse --show-toplevel` = `D:/qiand-life/1-projects/xr-knowledge-suite/output/xr-know-020/code/kotlin-book`，`git log --oneline -1` = `cf59fc2 lab(ch20): Gradle 多模块工程 multi-module-demo...`，仓身份成立后动工
- 工程：`D:\qiand-life\1-projects\xr-knowledge-suite\output\xr-know-020\code\kotlin-book\lab\ch21\quality-gate-demo`
- 实跑日期：2026-11-01
- 环境：Windows 11 Pro 10.0.22631 / JDK 21.0.6 (Oracle LTS) / 系统 Gradle 9.8.1 直跑（不用 wrapper，与仓根 README 约定一致）/ Kotlin JVM 插件 2.4.21 / junit-bom 6.1.3 / com.ncorti.ktfmt.gradle 0.27.0（引擎 com.facebook:ktfmt 0.64）/ detekt 1.23.8
- 演示域：`OverdueFeeCalculator`（图书逾期滞纳金：滞纳金 = 天数 x 日费率，封顶值兜底；衔接 ch15 图书馆域，包名 `library` 同源）
- 命令形式：`gradle -p <工程根> <task> --console=plain`，`JAVA_TOOL_OPTIONS=-Dstdout.encoding=UTF-8` 保中文不乱码；原始日志存 `_work/logs/`
- 退出码汇总：测试反例 exit 1 -> 复绿 exit 0；ktfmt 反例 exit 1 -> 复绿 exit 0；detekt 反例 exit 1 -> 复绿 exit 0；check/build 终态 exit 0

---

## 实测 1：单元测试基线（正例，exit 0）

命令：`gradle -p . test --console=plain`，首次运行（含插件与依赖下载），exit **0**。

逐字输出（关键行）：

```
> Task :compileKotlin
> Task :compileTestKotlin

> Task :test
OverdueFeeCalculatorTest > 封顶值生效后金额不再增长() PASSED
OverdueFeeCalculatorTest > 负数逾期天数直接拒绝() PASSED
OverdueFeeCalculatorTest > 滞纳金按天数线性增长() PASSED
OverdueFeeCalculatorTest > 零天逾期不收费() PASSED
OverdueFeeCalculatorTest > 负费率在构造期即被拒绝() PASSED

BUILD SUCCESSFUL in 13s
4 actionable tasks: 4 executed
```

计数行（JUnit XML 报告 `build/test-results/test/TEST-library.OverdueFeeCalculatorTest.xml` 逐字）：

```
tests="5" skipped="0" failures="0" errors="0"
```

## 实测 2：参数化测试（正例，exit 0）

命令：`gradle -p . test --rerun-tasks --console=plain`，exit **0**。同一断言被 10 组 (天数, 费率, 期望值) 数据驱动，逐条入账：

```
OverdueFeeParameterizedTest > 滞纳金按公式或封顶值结算(int, double, double) > 逾期"0"天, 日费率"0.5" => 滞纳金应为"0.0"元 PASSED
OverdueFeeParameterizedTest > 滞纳金按公式或封顶值结算(int, double, double) > 逾期"1"天, 日费率"0.5" => 滞纳金应为"0.5"元 PASSED
OverdueFeeParameterizedTest > 滞纳金按公式或封顶值结算(int, double, double) > 逾期"3"天, 日费率"0.5" => 滞纳金应为"1.5"元 PASSED
OverdueFeeParameterizedTest > 滞纳金按公式或封顶值结算(int, double, double) > 逾期"10"天, 日费率"0.5" => 滞纳金应为"5.0"元 PASSED
OverdueFeeParameterizedTest > 滞纳金按公式或封顶值结算(int, double, double) > 逾期"39"天, 日费率"0.5" => 滞纳金应为"19.5"元 PASSED
OverdueFeeParameterizedTest > 滞纳金按公式或封顶值结算(int, double, double) > 逾期"40"天, 日费率"0.5" => 滞纳金应为"20.0"元 PASSED
OverdueFeeParameterizedTest > 滞纳金按公式或封顶值结算(int, double, double) > 逾期"100"天, 日费率"0.5" => 滞纳金应为"20.0"元 PASSED
OverdueFeeParameterizedTest > 滞纳金按公式或封顶值结算(int, double, double) > 逾期"0"天, 日费率"2.0" => 滞纳金应为"0.0"元 PASSED
OverdueFeeParameterizedTest > 滞纳金按公式或封顶值结算(int, double, double) > 逾期"5"天, 日费率"2.0" => 滞纳金应为"10.0"元 PASSED
OverdueFeeParameterizedTest > 滞纳金按公式或封顶值结算(int, double, double) > 逾期"20"天, 日费率"2.0" => 滞纳金应为"20.0"元 PASSED

BUILD SUCCESSFUL in 9s
```

计数行（XML `TEST-library.OverdueFeeParameterizedTest.xml` 逐字）：

```
tests="10" skipped="0" failures="0" errors="0"
```

合计 15 条（5 单元 + 10 参数化），与后续红灯回执的 `15 tests completed` 对得上。

## 实测 3：断言失败反例（exit 1 -> 修复 -> exit 0）

反例：把 `OverdueFeeCalculatorTest.滞纳金按天数线性增长` 的期望值故意写错（1.5 改 1.3）。
命令：`gradle -p . test --rerun-tasks --console=plain`，exit **1**。逐字关键行：

```
OverdueFeeCalculatorTest > 滞纳金按天数线性增长() FAILED
    org.opentest4j.AssertionFailedError at OverdueFeeCalculatorTest.kt:23

15 tests completed, 1 failed

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':test'.
> There were failing tests. See the report at: file:///.../build/reports/tests/test/index.html

BUILD FAILED in 9s
```

expected/actual 行（XML 报告 failure message 属性逐字，`&lt;` 为 XML 转义）：

```
org.opentest4j.AssertionFailedError: expected: &lt;1.3&gt; but was: &lt;1.5&gt;
```

即正文口径：`expected: <1.3> but was: <1.5>`。

### 对照反例：多条断言塞一条测试（临时件，回执后已删除）

`MultiAssertBadDemoTest`：同一测试方法内三条断言，第 1 条通过、第 2 条失败、第 3 条根本没执行。
exit **1**，逐字关键行：

```
OverdueFeeCalculatorTest > 滞纳金按天数线性增长() FAILED
    org.opentest4j.AssertionFailedError at OverdueFeeCalculatorTest.kt:23
MultiAssertBadDemoTest > 一条塞多条断言的坏味道() FAILED
    org.opentest4j.AssertionFailedError at MultiAssertBadDemoTest.kt:20
16 tests completed, 2 failed
BUILD FAILED in 8s
```

定位含糊实证：报告只指到 `MultiAssertBadDemoTest.kt:20`（第 2 条断言）；第 3 条（21 行）对错无从得知——一条断言一个意图的动机即此。回执后临时件已删除。

复绿：期望值改回 1.5，`gradle -p . test --rerun-tasks --console=plain`，exit **0**，15/15 PASSED，`BUILD SUCCESSFUL in 9s`。

## 实测 4：ktfmt 关卡（反例 exit 1 -> 格式化 -> exit 0）

任务实名（`gradle tasks --all` 逐字）：

```
ktfmtFormat     - Run Ktfmt formatter for all source sets for project 'quality-gate-demo'
ktfmtFormatMain/ktfmtFormatTest/ktfmtFormatScripts（source set 变体）
ktfmtCheck      - Run Ktfmt validation for all source sets for project 'quality-gate-demo'
ktfmtCheckMain/ktfmtCheckTest/ktfmtCheckScripts（source set 变体）
```

反例：`FeeFormatter.kt` 以紧凑单行放行（其余文件为 4 空格缩进，同样不合 ktfmt 正形）。
命令：`gradle -p . ktfmtCheck --console=plain`，exit **1**。逐字：

```
> Task :ktfmtCheckMain FAILED
[ktfmt] Invalid formatting for: D:\...\lab\ch21\quality-gate-demo\src\main\kotlin\library\FeeFormatter.kt
[ktfmt] Invalid formatting for: D:\...\lab\ch21\quality-gate-demo\src\main\kotlin\library\OverdueFeeCalculator.kt

* What went wrong:
Execution failed for task ':ktfmtCheckMain' (registered by plugin 'com.ncorti.ktfmt.gradle').
> [ktfmt] Found 2 files that are not properly formatted:
  src\main\kotlin\library\FeeFormatter.kt
  src\main\kotlin\library\OverdueFeeCalculator.kt

BUILD FAILED in 8s
```

申报：ktfmt 0.27.0 的控制台报告只到**文件级**，不带行号列号（与卡面预期的"行列报告"不符，实情如此；行级差异要看 build/reports 或本地 diff）。

修复：`gradle -p . ktfmtFormat --console=plain`（exit 0，3 个 source set 任务全执行，全部 Kotlin 文件就地重排为 ktfmt 正形，2 空格缩进）。
复检：`gradle -p . ktfmtCheck --console=plain`，exit **0**，`BUILD SUCCESSFUL in 9s`。

## 实测 5：detekt 关卡（反例 exit 1 -> 修复 -> exit 0）

基线先行：`gradle -p . detekt --console=plain`，在植入违例前先跑，exit **0**（`> Task :detekt` + `BUILD SUCCESSFUL in 8s`），证明存量代码零违例，红灯确系临时植入。

反例：`Discount.kt` 裸写 `10.0` 与 `0.9`（不给命名常量）。
命令：`gradle -p . detekt --console=plain`，exit **1**。逐字：

```
> Task :detekt FAILED
D:\...\lab\ch21\quality-gate-demo\src\main\kotlin\library\Discount.kt:8:16: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
D:\...\lab\ch21\quality-gate-demo\src\main\kotlin\library\Discount.kt:9:21: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]

* What went wrong:
Execution failed for task ':detekt' (registered by plugin 'io.gitlab.arturbosch.detekt').
> Analysis failed with 2 weighted issues.

BUILD FAILED in 7s
```

规则名原文：`[MagicNumber]`，报告格式 `文件:行:列: 消息 [规则名]`（detekt 给行列，ktfmt 不给，两关报告粒度如实对照）。

修复：升格具名常量 `DISCOUNT_THRESHOLD = 10.0`、`DISCOUNT_RATE = 0.9`（detekt 默认 ignoreConstantDeclaration 放过常量声明）。复跑 detekt，exit **0**，`> Task :detekt` + `BUILD SUCCESSFUL in 7s`。

## 实测 6：四级门控串联（check 一键全跑 + build）

### 意外收获的跨关实证（正记录）：detekt 修好的 Discount.kt 手写形态不合 ktfmt 正形

`gradle -p . check` 首跑（detekt 已绿）exit **1**，被 ktfmt 关卡拦下——两关独立、各管各的：

```
> Task :detekt UP-TO-DATE

> Task :ktfmtCheckMain FAILED
[ktfmt] Invalid formatting for: D:\...\src\main\kotlin\library\Discount.kt

BUILD FAILED in 9s
```

`ktfmtFormat` 修复（exit 0）后重跑 `gradle -p . check --console=plain`，exit **0**。逐字任务序列（check 实际聚合与执行顺序）：

```
> Task :detekt
> Task :ktfmtCheckMain
> Task :ktfmtCheckScripts UP-TO-DATE
> Task :ktfmtCheckTest UP-TO-DATE
> Task :compileKotlin
> Task :compileTestKotlin
> Task :test
> Task :check
BUILD SUCCESSFUL in 10s
8 actionable tasks: 6 executed, 2 up-to-date
```

任务图顺序实证（`gradle check --dry-run --console=plain` 逐字，exit 0）：

```
:detekt SKIPPED
:ktfmtCheckMain SKIPPED
:ktfmtCheckScripts SKIPPED
:ktfmtCheckTest SKIPPED
:compileKotlin SKIPPED
...
:test SKIPPED
:check SKIPPED
```

四关齐全实证：detekt（静态检查）+ ktfmtCheckMain/ktfmtCheckTest/ktfmtCheckScripts（格式关）+ test（单元 + 参数化测试）全部挂在 `:check` 之下，`gradle check` 一键全跑，无需手工逐个调用。

构建验证：`gradle -p . build --console=plain`，exit **0**，`BUILD SUCCESSFUL in 7s`（8 actionable tasks: 8 up-to-date，check 为 build 的既有依赖，四关随 build 一并兑现）。

## 申报与偏差

1. ktfmt 接线实情：**0.27.0 落在插件坐标上**（`com.ncorti.ktfmt.gradle:com.ncorti.ktfmt.gradle.gradle.plugin:0.27.0` -> `com.ncorti.ktfmt.gradle:plugin:0.27.0`），底层格式化引擎是它传递依赖的 `com.facebook:ktfmt:0.64`（gradle buildEnvironment + 本机 gradle 缓存 jar 双证）。任务实名 ktfmtCheck / ktfmtFormat（含 Main/Test/Scripts 三变体），插件自动把 ktfmtCheck 挂进 check。
2. ktfmtCheck 控制台报告只到文件级（无行列），与卡面"含文件与行列报告"预期不符；detekt 报告带行列（8:16）。已如实引文。
3. 案例仓无 gradle/libs.versions.toml（版本直写 build.gradle.kts），lab 照抄同款直写口径。
4. JUnit 6 接线沿用案例仓口径：junit-bom 收版本 + jupiter + platform-engine + platform-launcher 四行依赖，缺 launcher 在 Gradle 9 下测试跑不起来（案例仓注释口径，lab 未单独做删行反例）。
5. ktfmtFormat 会重排全部 Kotlin 文件（含 build.gradle.kts，Scripts source set），本回执引用的源码均为盘上 ktfmt 正形终态；另实测一次正记：手写代码过了 detekt 也会被 ktfmt 拦（两关独立），已在实测 6 引文。
