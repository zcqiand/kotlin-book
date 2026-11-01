# ch30 flow-engine-demo 实测回执

- 日期：2026-11-01
- 工程：`output/xr-know-020/code/kotlin-book/lab/ch30/flow-engine-demo/`
- 运行方式：系统 Gradle 9.8.1 直跑（不用 wrapper，沿 kotlin-book 仓 README lab 约定），`JAVA_TOOL_OPTIONS=-Dstdout.encoding=UTF-8`，`--console=plain`。数据库：**本工程零数据库**（纯内存状态机 + 内存 RBAC），本机 PG 与共享 PG 零触碰。
- 对码：案例仓 `lab-management-system-kotlin` **只读**零修改——状态八值对码 `src/main/kotlin/io/xr/lab/shared/models/FlowStatus.kt` L21-43；权限串家族对码 `src/main/kotlin/io/xr/lab/impl/AuthApiServiceImpl.kt` L32-45（DEMO_PERMISSIONS，五域前缀 contract/sample/inspection/report/audit + `*`）。
- 体例：沿 ch29 repo-migrate-demo receipts.md。原始日志逐字存 `_work/rec-*.log`（每条命令一个文件，含 exit code 首行）。

---

## 实测件 1：编译 compileKotlin

命令：`gradle compileKotlin --console=plain`（exit 0）

```
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin

BUILD SUCCESSFUL in 13s
1 actionable task: 1 executed
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

要点：一把过零修复——密封类 when 穷举（外层状态无 else、describe 无 else）编译期全部生效。

## 实测件 2：测试 gradle test（全绿）

命令：`gradle test --console=plain`（exit 0）

```
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin UP-TO-DATE
> Task :compileJava NO-SOURCE
> Task :processResources NO-SOURCE
> Task :classes UP-TO-DATE
> Task :jar
> Task :processTestResources NO-SOURCE
> Task :compileTestKotlin
> Task :compileTestJava NO-SOURCE
> Task :testClasses UP-TO-DATE

> Task :test
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8

BUILD SUCCESSFUL in 11s
4 actionable tasks: 3 executed, 1 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

全绿读数（`build/test-results/test/TEST-lab.ch30.FlowEngineTest.xml`，testsuite 行逐字）：

```
<testsuite name="lab.ch30.FlowEngineTest" tests="5" skipped="0" failures="0" errors="0" timestamp="2026-11-01T01:53:56.364Z" hostname="WIN-20240323MFF" time="0.081">
```

5 用例：主链全通 / reject 退回支线 / pass 时 Return 被拒 / 非法跃迁抛异常 / 越权抛异常。

## 实测件 3：场景回执 gradle run

命令：`gradle run --console=plain`（exit 0）

```
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin UP-TO-DATE
> Task :compileJava NO-SOURCE
> Task :processResources NO-SOURCE
> Task :classes UP-TO-DATE

> Task :run
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
== 回执 A｜完整流转（receiving → task_assignment → data_entry → review → approval → issuance → archived → completed） ==
[站1] 状态=receiving 模块落点=sample 读数=收样回执=RCPT-2026-1101-001/租户=T-01
[站2] 状态=task_assignment 模块落点=inspection 读数=检测员=李检测
[站3] 状态=data_entry 模块落点=inspection 读数=已录记录=12条
[站4] 状态=review 模块落点=inspection 读数=复核=王复核/结论=pass
[站5] 状态=approval 模块落点=report 读数=审批人=赵主任
[站6] 状态=issuance 模块落点=report 读数=报告编号=RPT-2026-0031
[站7] 状态=archived 模块落点=audit 读数=档案号=ARC-2026-0007
[站8] 状态=completed 模块落点=audit 读数=闭环时间=2026-11-01T17:30

== 回执 B｜非法跃迁拦截 ==
[拦截] 非法跃迁被拒：receiving 不直达 issuance
       （异常原文：非法跃迁：receiving 不允许执行 issue（receiving 不可直达 issuance））

== 回执 C｜越权拦截 ==
[拦截] 越权请求被拒：sampler 无 report:issue
       （异常原文：越权：sampler 无 report:issue）

BUILD SUCCESSFUL in 7s
2 actionable tasks: 1 executed, 1 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

要点：
- 回执 A 八站齐（站1-站8），模块落点按权限串业务域对齐：收样→sample、检测→inspection、签发→report、归档→audit。
- 回执 B：receiving 直跳 issuance 被 `IllegalFlowTransition` 拦下，主行与两条拦截行均按派单口径逐字命中。
- 回执 C：sampler（仅 sample:write）请求 report:issue 被 `ForbiddenException` 拦下。
- 退回支线（review→data_entry）在测试实测件 2 的 `reviewRejectReturnsToDataEntry` 用例实测（reject 放行、pass 拒绝两向都盖到），run 场景不打该支线。

---

## 附：命令与 exit code 清单

| # | 命令 | 日志 | exit |
|---|------|------|------|
| 1 | `gradle compileKotlin --console=plain` | `_work/rec-01-compile.log` | 0 |
| 2 | `gradle test --console=plain` | `_work/rec-02-test.log` | 0 |
| 3 | `gradle run --console=plain` | `_work/rec-03-run.log` | 0 |

## 附：回执总检

- 逐字纪律：实测件 1-3 均为单轮整块输出（`gradle … > _work/rec-*.log 2>&1` 原样拷贝），无跨轮拼合；本轮零失败零修复，无失败日志。
- 环境注记：系统 Gradle 9.8.1 直跑；`JAVA_TOOL_OPTIONS=-Dstdout.encoding=UTF-8` 下中文输出无 U+FFFD。
- 边界纪律：案例仓只读零写入；本工程零数据库，零 PG 触碰；新建文件仅限本工程目录，仓根 README 未动（留编排臂登记）。
