# ch29 repo-migrate-demo 实测回执

- 日期：2026-11-01
- 工程：`output/xr-know-020/code/kotlin-book/lab/ch29/repo-migrate-demo/`
- 运行方式：系统 Gradle 9.8.1 直跑（不用 wrapper，沿 kotlin-book 仓 README lab 约定），`JAVA_TOOL_OPTIONS=-Dstdout.encoding=UTF-8`，`--console=plain`。数据库：本机 PG 127.0.0.1:54321（postgres/postgres），验证库 `kotlin_book_ch29_verify`（由工程 init-db 子命令自建）。
- 体例：沿 ch23 exposed-crud receipts.md。原始日志逐字存 `_work/rec-*.log`（每条命令一个文件，含 exit code 首行）。

---

## 实测件 0（前置）：验证库创建 init-db

命令：`gradle run --args="init-db" --console=plain`（exit 0）

```
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :processResources UP-TO-DATE
> Task :compileKotlin
> Task :compileJava NO-SOURCE
> Task :classes UP-TO-DATE

> Task :run
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
数据库 kotlin_book_ch29_verify 已创建

BUILD SUCCESSFUL in 16s
3 actionable tasks: 2 executed, 1 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

实现注记：建库走裸 R2DBC SPI（`ConnectionFactories` + reactor `Mono`，对维护库 postgres 发 `CREATE DATABASE`），与 ch23 同款——CREATE DATABASE 不能进事务块。日志留痕：首两次 `compileKotlin` 失败（见「附：修复清单」）。

---

## 实测件 1：迁移首应用 migrate（第一遍）

命令：`gradle run --args="migrate" --console=plain`（exit 0）

```
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin UP-TO-DATE
> Task :compileJava NO-SOURCE
> Task :processResources UP-TO-DATE
> Task :classes UP-TO-DATE

> Task :run
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
迁移 V001__init.sql：应用成功（2 条语句）
迁移 V002__test_records_reviewer.sql：应用成功（1 条语句）

BUILD SUCCESSFUL in 10s
3 actionable tasks: 1 executed, 2 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

要点：V001/V002 两个迁移文件的脚本 SQL 原样执行，逐条按分号拆分应用，`schema_migration` 台账登记。DDL 执行通道为事务内 `exec(stmt) { true }?.toList()`（DDL 无结果集，收空流），实测可用。

## 实测件 2：迁移幂等跳过 migrate（第二遍）

命令：`gradle run --args="migrate" --console=plain`（exit 0；**第二遍**，验证幂等跳过回执）

```
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin UP-TO-DATE
> Task :compileJava NO-SOURCE
> Task :processResources UP-TO-DATE
> Task :classes UP-TO-DATE

> Task :run
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
迁移 V001__init.sql：已应用，跳过
迁移 V002__test_records_reviewer.sql：已应用，跳过

BUILD SUCCESSFUL in 9s
3 actionable tasks: 1 executed, 2 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

要点：两个版本均已登记 → 逐版本「已应用，跳过」，DDL 零重复执行。

## 实测件 3：种子灌入 seed（第一遍）

命令：`gradle run --args="seed" --console=plain`（exit 0）

```
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin UP-TO-DATE
> Task :compileJava NO-SOURCE
> Task :processResources UP-TO-DATE
> Task :classes UP-TO-DATE

> Task :run
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
种子灌入完成：3 行（upsert 幂等，可重跑）

BUILD SUCCESSFUL in 9s
3 actionable tasks: 1 executed, 2 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

## 实测件 4：种子 upsert 幂等 seed（第二遍）

命令：`gradle run --args="seed" --console=plain`（exit 0；第二遍，验证 upsert 幂等）

```
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin UP-TO-DATE
> Task :compileJava NO-SOURCE
> Task :processResources UP-TO-DATE
> Task :classes UP-TO-DATE

> Task :run
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
种子灌入完成：3 行（upsert 幂等，可重跑）

BUILD SUCCESSFUL in 9s
3 actionable tasks: 1 executed, 2 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

要点：与第一遍输出一致——update 命中（行数≠0）走更新分支不插新行，表内仍 3 行（`all` 一条龙的 [冒烟1] 计数 3 行可交叉印证）。

## 实测件 5：一条龙完整冒烟 all

命令：`gradle run --args="all" --console=plain`（exit 0；init-db → migrate → seed → smoke 一条龙）

```
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin UP-TO-DATE
> Task :compileJava NO-SOURCE
> Task :processResources UP-TO-DATE
> Task :classes UP-TO-DATE

> Task :run
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
数据库 kotlin_book_ch29_verify 已创建
迁移 V001__init.sql：已应用，跳过
迁移 V002__test_records_reviewer.sql：已应用，跳过
种子灌入完成：3 行（upsert 幂等，可重跑）
== 冒烟 1｜仓储读：findAll ==
[冒烟1] findAll = 3 行（期望 3）
== 冒烟 2｜仓储读：findByCode ==
[冒烟2] findByCode(STRC) = 混凝土结构（期望 混凝土结构）
== 冒烟 3｜仓储写：upsert 新增专业 ==
[冒烟3] upsert 后 findAll = 4 行（期望 4）
== 冒烟 4｜仓储写：setEnabled 停用再启用 ==
[冒烟4] STEL enabled: false → true（期望 false → true）
== 冒烟 5｜事务边界：中途失败整块回滚 ==
[冒烟5] 回滚后 findByCode(ROLLBACK_ROW) = null（期望 null）
== 清场 ==
[清场] 冒烟行已删，findAll = 3 行（期望 3）

BUILD SUCCESSFUL in 10s
3 actionable tasks: 1 executed, 2 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

要点：
- 五项冒烟全过：读（findAll 计数 / findByCode 字段值）、写（upsert 新增 3→4 行、setEnabled false→true）、事务边界（事务内中途抛错 → ROLLBACK_ROW 查无，整块回滚），清场删冒烟行回到 3 行基线。
- 一条龙内 migrate/seed 均走幂等跳过分支（对照实测件 2/4），证明管线整体可重复执行。
- 注记：`all` 里 init-db 打「已创建」属 `ensureVerifyDatabase` 的既有行为——库在实测件 0 已建，裸 R2DBC SPI 的 `execute()` 返回 Result 是惰性的，「already exists」错误在取结果前不冒出，实现按异常消息判 "exists" 的分支未触发。建库本身成功、流程零阻断，教学语义（建若缺）不因库已存在而改变。

---

## 附：命令与 exit code 清单

| # | 命令 | 日志 | exit |
|---|------|------|------|
| 0a | `gradle run --args="init-db" --console=plain`（首轮） | `_work/rec-00a-compile-fail-1.log` | 1 |
| 0b | `gradle run --args="init-db" --console=plain`（修复一后） | `_work/rec-00b-compile-fail-2.log` | 1 |
| 1 | `gradle run --args="init-db" --console=plain`（修复二后） | `_work/rec-01-init-db.log` | 0 |
| 2 | `gradle run --args="migrate" --console=plain`（第一遍） | `_work/rec-02-migrate-1st.log` | 0 |
| 3 | `gradle run --args="migrate" --console=plain`（第二遍） | `_work/rec-03-migrate-2nd.log` | 0 |
| 4 | `gradle run --args="seed" --console=plain`（第一遍） | `_work/rec-04-seed-1st.log` | 0 |
| 5 | `gradle run --args="seed" --console=plain`（第二遍） | `_work/rec-05-seed-2nd.log` | 0 |
| 6 | `gradle run --args="all" --console=plain` | `_work/rec-06-all.log` | 0 |

## 附：修复清单（最小机械修复，逐笔）

1. **SeedLoader.kt 删 `import kotlinx.serialization.json.content`**（编译错 `Unresolved reference 'content'`，line 5）。原因：kotlinx-serialization 1.9.0 中 `content` 已是 `JsonPrimitive` 的成员属性，不存在同名顶层扩展，该 import 恒为 unresolved；删除后成员属性直接解析，`jsonPrimitive.content` 调用点零改动，教学语义不变。
2. **SpecialtyRepository.kt 补 `import kotlinx.coroutines.flow.map` 与 `import kotlinx.coroutines.flow.firstOrNull`**（编译错 `Unresolved reference 'map' on receiver of type 'Query'` / `'firstOrNull' on receiver of type 'Query'`）。原因：Exposed v1 的 `Query` 是 `Flow<ResultRow>`，`map`/`firstOrNull` 是 kotlinx.coroutines.flow 扩展函数，缺 import 即不可见；纯补漏，调用点零改动。
3. **SpecialtyRepository.kt findAll 尾补 `.toList()`**（编译错 `Return type mismatch: expected 'List<Specialty>', actual 'Flow<Specialty>'`）。原因：`map` 仍是 Flow，需收流成 List；`toList`（kotlinx.coroutines.flow）原文件已 import，补一个链式调用即等价收流（与 ch23 实测件 2 的 `.toList()` 收流形态一致）。签名与教学语义不变。

无重构、无教学语义改动；DDL 执行通道 `exec(stmt) { true }` 实测可用，未动用等价原生执行通道替换权。

## 附：回执总检

- 逐字纪律：实测件 1-5 均为单轮整块输出（`gradle run > _work/rec-*.log 2>&1` 原样拷贝），无跨轮拼合；0a/0b 两份失败日志为当时的逐字留痕。
- 环境注记：系统 Gradle 9.8.1 直跑；`JAVA_TOOL_OPTIONS=-Dstdout.encoding=UTF-8` 下中文输出无 U+FFFD。
- 数据库纪律：仅本机 127.0.0.1:54321，验证库 kotlin_book_ch29_verify 由工程 init-db 自建，零共享库触碰。
