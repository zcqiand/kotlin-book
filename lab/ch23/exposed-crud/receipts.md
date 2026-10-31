# ch23 exposed-crud 实测回执

- 日期：2026-11-01
- 工程：`output/xr-know-020/code/kotlin-book/lab/ch23/exposed-crud/`
- 版本口径（version-lock.json）：Kotlin 2.4.21 / Gradle 9.8.1 / JDK 21 / Exposed 1.5.1（exposed-bom 收口）/ r2dbc-postgresql 1.1.3.RELEASE / junit 6.1.3（junit-bom）/ kotlinx-coroutines 1.11.0 / slf4j 2.0.16
- 驱动路线：R2DBC + 挂起事务。依据 = 案例仓实码（`xr-code-suite/output/saas-identity-platform-kotlin`：`R2dbcDatabase.connect` + `suspendTransaction` + `org.jetbrains.exposed.v1.r2dbc.*` 全套；其 build.gradle.kts 注释「r2dbc=Phase 3 挂 Ktor 协程」；同族 lab-management-system-kotlin 同为 r2dbc-postgresql 接线）。Exposed API 用法全部从该仓实码转写，`autoIncrement`/`uniqueIndex`/`deleteAll`/`SchemaUtils.create` 形态另经 exposed-core/exposed-r2dbc 1.5.1 jar 字节码（javap）核实。
- PG：本机 127.0.0.1:54321（phase4 实测臂数据库纪律口径，postgres/postgres），验证库 `kotlin_book_ch23_verify`，零容器、零共享 PG 新建。
- 运行方式：系统 Gradle 9.8.1 直跑（phase4 Gradle 环境纪律），`JAVA_TOOL_OPTIONS=-Dstdout.encoding=UTF-8`。

---

## 实测件 0（前置）：验证库创建 init-db

命令：`gradle run --args="init-db" --console=plain`（exit 0）

```
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8

> Task :run
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
数据库 kotlin_book_ch23_verify 已创建

BUILD SUCCESSFUL in 9s
2 actionable tasks: 1 executed, 1 up-to-date
```

实现注记：CREATE DATABASE 不能进事务块（首次尝试 `suspendTransaction { SchemaUtils.createDatabase(...) }` 实跑被 PG 拒绝，回执：`[25001] CREATE DATABASE cannot run inside a transaction block`），故建库走裸 R2DBC SPI（`io.r2dbc.spi.ConnectionFactories` + reactor `Mono`，autocommit 通道）；建表/增删改查仍全走 Exposed。

---

## 实测件 1：表对象定义 + SchemaUtils.create 真实建表

命令：`gradle run --args="schema" --console=plain`（exit 0）

```
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin UP-TO-DATE
> Task :compileJava NO-SOURCE
> Task :processResources NO-SOURCE
> Task :classes UP-TO-DATE

> Task :run
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
建表 DDL: CREATE TABLE IF NOT EXISTS books (id SERIAL PRIMARY KEY, title VARCHAR(128) NOT NULL, author VARCHAR(64) NOT NULL, "year" INT NOT NULL) ALTER TABLE books ADD CONSTRAINT books_title_uq UNIQUE (title)
books 表已建立（已存在则幂等跳过）

BUILD SUCCESSFUL in 9s
2 actionable tasks: 1 executed, 1 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

要点：`integer("id").autoIncrement()` 落成 PG `SERIAL PRIMARY KEY`；`year` 是 PG 保留字，Exposed 自动转义为 `"year"`。

---

## 实测件 2：DSL 查询三连 insert → select → update → delete

命令：`gradle run --args="crud" --console=plain`（exit 0）

```
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :compileJava NO-SOURCE
> Task :processResources NO-SOURCE
> Task :classes UP-TO-DATE

> Task :run
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
== insert ==
写入 3 行，自增 id: 1, 2
== select (year >= 2023) ==
  1: Kotlin 实战 / 林牧 / 2024
  2: Ktor 服务端开发 / 沈砚 / 2025
== update (2020 → 2022) ==
更新 1 行
== delete (Ktor 服务端开发) ==
删除 1 行
剩余 2 行: Kotlin 实战, 数据访问模式

BUILD SUCCESSFUL in 8s
2 actionable tasks: 1 executed, 1 up-to-date
```

要点：`insert { } get BooksTable.id` 取 SERIAL 自增回填 id；`selectAll().where { }.toList()`（toList 来自 kotlinx.coroutines.flow——v1 的 Query 是 Flow）；`update({ 条件 }) { }` / `deleteWhere { }` 返回受影响行数。

## 实测件 3：transaction 块事务边界——回滚 vs 提交对照

命令：`gradle run --args="tx" --console=plain`（exit 0）

```
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :run
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
回滚分支：事务内写入后抛异常，表内行数 = 0（期望 0）
提交分支：无异常正常退出事务，表内行数 = 1（期望 1）

BUILD SUCCESSFUL in 8s
2 actionable tasks: 2 executed
```

要点：suspendTransaction 块内抛异常 → 整块回滚（写入未落库）；正常退出 → 落库。调试中的两次失败留痕：`deleteAll()` 与事务外 `select` 计数报 `IllegalStateException: No transaction in context.`（v1 纪律：一切 DSL/查询执行必须在 suspendTransaction 上下文内）。

## 实测件 4：反例——唯一约束违反（红态留档 → 移出复绿）

红态命令：`gradle run --args="reverse" --console=plain`（exit 0；违例被 runCatching 捕获展示，进程本身不炸）

```
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :run
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
第一次插入成功
第二次插入失败——异常类型: ExposedR2dbcException
异常消息: duplicate key value violates unique constraint "books_title_uq"

BUILD SUCCESSFUL in 8s
2 actionable tasks: 1 executed, 1 up-to-date
```

同题第二次 insert 触发 books_title_uq 唯一约束 → `org.jetbrains.exposed.v1.r2dbc.ExposedR2dbcException`。

### 4b. 移出反例代码后复绿

将 reverseDemo 的同题重复插入移出（改为插前查重），重跑：

```
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :run
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
同题已存在，跳过插入（反例代码已移出，恢复绿态）

BUILD SUCCESSFUL in 9s
2 actionable tasks: 2 executed
```

## 实测件 5：JUnit 集成测试——对真实 PostgreSQL 跑增删改查断言

命令：`gradle test --console=plain`（exit 0）

```
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :testClasses UP-TO-DATE

> Task :test
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8

BooksCrudTest > update 改得动 delete 删得掉() PASSED

BooksCrudTest > 唯一约束违反会回滚整块写入() PASSED

BooksCrudTest > insert 后能按条件查出() PASSED

BUILD SUCCESSFUL in 10s
4 actionable tasks: 3 executed, 1 up-to-date
```

JUnit XML 报告统计（build/test-results/test/）：`tests="3" failures="0" errors="0" skipped="0"`；HTML 报告 build/reports/tests/test/index.html。

测试内容：①insert 后按条件查出断言字段值；②update 行数=1 + delete 行数=1 + 查无残留；③同题两行事务整块回滚断言（assertThrows + 回滚后作者列表只剩首行）。

---

## 附：回执总检

- 逐字纪律：以上五件均为单轮整块输出（`gradle run/test > _work/rec*.log 2>&1` 原样拷贝），无跨轮拼合。
- 环境注记：Gradle wrapper distribution 未走现场下载，按 phase4 Gradle 环境纪律用系统 Gradle 9.8.1 直跑；`JAVA_TOOL_OPTIONS=-Dstdout.encoding=UTF-8` 下中文输出无 U+FFFD。
- 随书仓身份：`output/xr-know-020/code/kotlin-book/.git` 存在（test -d 通过）。

---

## 五、SQL 拼接演示回执（补测，§23.2）

- 日期：2026-11-01。演示源码 = `src/main/kotlin/lab/ch23/SqlConcatDemo.kt`（新增），入口分支 `sqlconcat` 挂在 Main.kt（先幂等播种「Kotlin 实战」，再跑三发）。
- 执行通道：事务内 `exec(sql) { row -> T }`——`R2dbcTransaction` 成员挂起函数（exposed-r2dbc 1.5.1 javap 核实签名：`exec(String, Iterable, Function1<Row, T>): Flow<T>`），Row 映射器用 r2dbc SPI 的 `row.get("列名", Class)`。返回值在 Kotlin 侧判空后 `.toList()` 收流。
- 只读红线：三发全是 SELECT，无任何 DELETE/UPDATE 载荷；发 3 异常捕获打印仅为可复跑。
- 中途失败留痕（红态已修）：首版把三发塞进同一事务、发 3 异常在事务内捕获——PG 判定事务中止后 commit 阶段抛 `PostgresqlRollbackException: [99999] The database returned ROLLBACK...`，且 Exposed 整块重试 3 次导致输出三重复、进程 exit 1。修正 = 每发独立事务、发 3 异常抛到块外捕获（失败即整块回滚，干净退出），重跑 exit 0。这条本身即 §23.2 的教学点：断链不只查询失败，还把整个事务带崩。

### 5.1 三发（播种 + 正常 / 注入 / 断链）

命令：`gradle run --args="sqlconcat" --console=plain`（exit 0）

```
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :processResources NO-SOURCE
> Task :compileKotlin
> Task :compileJava NO-SOURCE
> Task :classes UP-TO-DATE

> Task :run
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
播种：种子行「Kotlin 实战」已在，跳过
books 表当前 3 行
== 发 1｜正常输入：按书名精确匹配 ==
拼接 SQL: SELECT id, title, author FROM books WHERE title = 'Kotlin 实战'
返回 1 行:
  17: Kotlin 实战 / 林牧
== 发 2｜注入输入：恒真条件把整表拉出 ==
拼接 SQL: SELECT id, title, author FROM books WHERE title = 'x' OR '1'='1'
返回 3 行（整表被拉出）:
  12: 同题两行不可能 / 周临
  16: Exposed 入门 / 林牧
  17: Kotlin 实战 / 林牧
== 发 3｜断链：拼接引用不存在的列（列已改名、拼接代码没人改） ==
拼接 SQL: SELECT id, title, author FROM books WHERE name = 'Kotlin 实战'
异常类型: PostgresqlBadGrammarException
数据库错误消息: column "name" does not exist

BUILD SUCCESSFUL in 9s
2 actionable tasks: 2 executed
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

要点：注入载荷 `x' OR '1'='1` 拼进 WHERE 后形成 `title = 'x' OR '1'='1'`（恒真），3 行整表被拉出（表内全部行）；发 3 的 PG 原始错误 = `column "name" does not exist`（SQLSTATE 42703 类文案，r2dbc 驱动抛 `PostgresqlBadGrammarException`）。exit 说明：三发各自完成，进程 exit 0（发 3 异常被捕获，演示可复跑）。

### 5.2 顺带核查 1：事务外执行 DSL

命令：`gradle run --args="no-tx" --console=plain`（exit 0；异常被捕获打印，进程不炸）

```
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin UP-TO-DATE
> Task :compileJava NO-SOURCE
> Task :classes UP-TO-DATE

> Task :run
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
异常类型: java.lang.IllegalStateException
异常消息: No transaction in context.

BUILD SUCCESSFUL in 7s
2 actionable tasks: 1 executed, 1 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

此前仅在实测件 3 要点里文字提及，本节补齐逐字回执：`IllegalStateException: No transaction in context.`（`deleteAll()` 在无 suspendTransaction 上下文时触发）。

### 5.3 顺带核查 2：事务内 CREATE DATABASE

命令：`gradle run --args="create-db-tx" --console=plain`（exit 0；异常被捕获打印，进程不炸）

```
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin UP-TO-DATE
> Task :compileJava NO-SOURCE
> Task :classes UP-TO-DATE

> Task :run
Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8
异常类型: org.jetbrains.exposed.v1.r2dbc.ExposedR2dbcException
异常消息: CREATE DATABASE cannot run inside a transaction block
SQL: [CREATE DATABASE kotlin_book_ch23_verify]

BUILD SUCCESSFUL in 8s
2 actionable tasks: 1 executed, 1 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

此前仅在实测件 0 实现注记里文字引用，本节补齐逐字回执：`ExposedR2dbcException: CREATE DATABASE cannot run inside a transaction block`（PG 25001；本实跑经事务内 `exec` 通道复现，与实现注记的 `SchemaUtils.createDatabase` 首试路径殊途同归）。

### 5.4 回执总检（补测批）

- 逐字纪律：5.1-5.3 三件均为单轮整块输出（`gradle run > _work/rec-*.log 2>&1` 原样拷贝），无跨轮拼合。
- 注入发整表证据：发 2 返回 3 行 = 播种后表内全量行数（与「books 表当前 3 行」一致）。
- 幂等可复跑：sqlconcat 重复执行输出一致（种子行查重跳过）。
