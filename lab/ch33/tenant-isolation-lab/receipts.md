# ch33 tenant-isolation-lab 实测回执

- 日期：2026-11-01
- 工程：`output/xr-know-020/code/kotlin-book/lab/ch33/tenant-isolation-lab/`
- 运行方式：系统 Gradle 9.8.1 直跑（不用 wrapper，沿 kotlin-book 仓 README lab 约定），`JAVA_TOOL_OPTIONS=-Dstdout.encoding=UTF-8`，`--console=plain`。数据库：**本机 PG 127.0.0.1:54321（postgres/postgres，ch29 repo-migrate-demo 同口径，embedded 实例复用零新建容器）**，验证库 `kotlin_book_ch33_verify`（由工程 `ensureVerifyDatabase()` 自建，ch29 Db.kt 同款裸 R2DBC SPI 绕事务建库）；共享 PG 100.79.128.25 全程零触碰，案例仓 `saas-identity-platform-kotlin`（tag v0.2.1-20261025）只读零修改。
- 对码：案例仓七绑定文件逐件对码——`entity/TenantTable.kt`、`entity/TenantMemberTable.kt` 列定义逐列复刻；`shared/models/SwitchTenantResponse.kt` 字段与 validate() 语义复刻（去 @Serializable 基建）；`impl/AdminTenantsApiServiceImpl.kt` 复刻 list 读路径口径（DEFAULT_PAGE=0/SIZE=20、读路径排序 created_at ASC）并按同一写法补齐 create/disable/delete 三个生命周期 op；`impl/MeApiServiceImpl.kt` 复刻 meSwitchTenant 语义（租户存在 + 成员资格门槛=非 disabled + refresh 纯构造不落库）；两个测试类沿案例仓 `io/xr/harness/` 路径与类名（AdminTenantsListTest/MeApiTest），但调用面从 Ktor HTTP 裁剪为服务层直调（ch29/ch32 同款教学裁剪）。版本坐标：Kotlin jvm 2.4.21 / exposed-bom 1.5.1（BOM 收口禁裸写版本）/ r2dbc-postgresql 1.1.3.RELEASE / junit-bom 6.1.3 / jvmToolchain(21)。
- 体例：沿 ch29/ch32 receipts（`_work/` 收 rec-*.log，首行=exit code，正文逐字原始输出；本工程 receipts.md 按派单口径落工程根）。

## 第 0 节：文件清单

```
tenant-isolation-lab/
├── .gitignore                        （build/ .gradle/ .kotlin/ _work/，沿 ch30-ch32 先例）
├── build.gradle.kts                  （Kotlin 2.4.21 + exposed-bom 1.5.1 + r2dbc-postgresql 1.1.3 + junit-bom 6.1.3）
├── settings.gradle.kts
├── gradle/libs.versions.toml         （沿 ch29 版本目录同款）
├── src/main/kotlin/saas/identity/
│   ├── entity/
│   │   ├── TenantTable.kt            （uuid/varchar(64)/varchar(128)/short/timestampWithTimeZone，PK=id）
│   │   └── TenantMemberTable.kt      （+tenantId/userId/memberName?/isOwner/status，PK=id）
│   ├── shared/models/
│   │   └── SwitchTenantResponse.kt   （数据类 + validate() UUID 合法性门）
│   ├── impl/
│   │   ├── AdminTenantsApiServiceImpl.kt（create/disable/delete/list/count/tenantStatus 全生命周期）
│   │   └── MeApiServiceImpl.kt          （switchTenant + listMembersInContext + readMemberInContext
│   │                                     + renameMemberInContext + insertMember）
│   └── infra/
│       ├── LabPg.kt                  （54321 连接 + ensureVerifyDatabase + resetSchema）
│       └── IsolationException.kt     （TenantSwitchException：NOT_FOUND/BAD_REQUEST）
├── src/test/kotlin/io/xr/harness/
│   ├── AdminTenantsListTest.kt       （实测件 1：生命周期全流程，1 用例 13 断言）
│   └── MeApiTest.kt                  （实测件 2/3/4：换租户隔离/跨租户不可见/门槛/契约，5 用例）
├── receipts.md                       （本文件）
└── _work/rec-*.log ×6
```

---

## 实测件 1：租户生命周期全流程（AdminTenantsListTest）

命令：`gradle test --tests "*AdminTenantsListTest"` 面内含于全量 test 跑，逐步读数（每步 assertEquals 实测值）：

| 步 | 操作 | 读数 |
|----|------|------|
| 1 | 创建租户 A（lab-alpha） | 租户总数 0→**1**，status=**1**（active） |
| 2 | 创建租户 B（lab-beta） | 租户总数 1→**2** |
| 3 | 成员入库（A×2、B×1） | A 上下文成员数 **2**、B 上下文成员数 **1** |
| 4 | 停用 A | 受影响行数 **1**，A status 读回 **0**，B 仍 **1** |
| 5 | 删除 A | 成员清除 **2** 行 + 租户清除 **1** 行，总数回落 **1**（只剩 B），A 主键读 status=**-1**（不存在），A 上下文成员数 **0** |

## 实测件 2：行级隔离换租户（MeApiTest.switchTenantContextScopesQueryResults）

同一查询入口 `listMembersInContext(ctx.tenantId)`，上下文由 switchTenant 换发对回显：

- 种子：A 2 名成员（userX+甲组二号）、B 2 名成员（乙组一号 userC + userX 跨租户成员行）。
- `switchTenant(userX, A)` → 换发对 tenantId 回显 A；A 上下文查得 **N=2 行**，全部 tenantId=A（无越界行）。
- 同一入口 `switchTenant(userX, B)` → 回显 B；B 上下文查得 **M=2 行**，全部 tenantId=B（无越界行）。
- 合计读数 2+2=4 与种子一致。

## 实测件 3：跨租户不可见（MeApiTest.crossTenantPrimaryKeyAccessIsInvisible）

A 上下文按租户 B 的成员主键 memberBOnly（库中真实存在）读/改：

- 越界读 `readMemberInContext(A, memberBOnly)` → **null（0 行命中）**，主键存在性不泄露。
- 对照组：换到 B 上下文同一主键 → 命中（id 相等），排除「主键不存在」解释。
- 越界改 `renameMemberInContext(A, memberBOnly, "越界改名")` → **受影响 0 行**，B 侧数据原样（memberName 仍「乙组一号」）。
- 边界内改（B 上下文）→ 受影响 **1 行**（隔离谓词双向验证）。

补充：非成员换租户 → TenantSwitchException(NOT_FOUND) 且 message 含 "not an active member"（不泄露租户存在性，案例仓 404 语义镜像）；未知租户 → NOT_FOUND 含 "tenant"；成员资格门槛=非 disabled——invited(2)/suspended(3) 可切、disabled(0) 拒（S5 口径）。

## 实测件 4：隔离回归测试全绿

命令：`gradle test --console=plain`（exit 0）— `_work/rec-03-test-2nd.log`

```
BUILD SUCCESSFUL in 13s
4 actionable tasks: 2 executed, 2 up-to-date
```

全绿读数（`build/test-results/test/TEST-*.xml` testsuite 行逐字，clean 后终验口径）：

```
<testsuite name="io.xr.harness.AdminTenantsListTest" tests="1" skipped="0" failures="0" errors="0" timestamp="2026-11-01T04:13:16.785Z" hostname="WIN-20240323MFF" time="1.706">
<testsuite name="io.xr.harness.MeApiTest" tests="5" skipped="0" failures="0" errors="0" timestamp="2026-11-01T04:13:18.493Z" hostname="WIN-20240323MFF" time="2.264">
```

**tests=6, failures=0, errors=0**。6 用例：生命周期全流程 / 换租户上下文隔离读数 / 跨租户主键不可见读改 / 换租户门槛（非成员+未知租户）/ 成员 status 门槛（invited/suspended 可切 disabled 不可切）/ 换发对契约 validate()。

## 实测件 5：构建门 + clean 后一条龙复验

- `gradle build --console=plain`（exit 0）— `_work/rec-04-build.log`，产物 `build/libs/tenant-isolation-lab-0.1.0.jar`。
- `gradle clean` + `gradle build test --console=plain`（均 exit 0）— `_work/rec-05-clean-build-test.log`，**clone 即跑路径成立**（BUILD SUCCESSFUL in 13s，4 tasks executed，6/6 复绿）。

## 实测件 6：环境读数

命令：`gradle --version`（exit 0）— `_work/rec-06-gradle-version.log`

```
Gradle 9.8.1
Kotlin:        2.4.10
Launcher JVM:  21.0.6 (Oracle Corporation 21.0.6+8-LTS-188)
```

与 ch31/ch32 复验清单读数一致（JVM 21.0.6）。

---

## 附：命令与 exit code 清单

| # | 命令 | 日志 | exit |
|---|------|------|------|
| 1 | `gradle compileKotlin --console=plain` | `_work/rec-01-compile.log` | 0 |
| 2 | `gradle test --console=plain`（首跑红） | `_work/rec-02-test.log` | 1 |
| 3 | `gradle test --console=plain`（修复后） | `_work/rec-03-test-2nd.log` | 0 |
| 4 | `gradle build --console=plain` | `_work/rec-04-build.log` | 0 |
| 5 | `gradle clean` + `gradle build test --console=plain` | `_work/rec-05-clean-build-test.log` | 0 |
| 6 | `gradle --version` | `_work/rec-06-gradle-version.log` | 0 |

## 附：修复清单（最小机械修复，逐笔）

1. **补 exposed-kotlin-datetime 依赖**：首跑 compileKotlin 红（`timestampWithTimeZone` Unresolved）→ libs.versions.toml + build.gradle.kts 增 `exposed-kotlin-datetime`（BOM 收口，ch32 同款）。
2. **Flow 面补 toList()/flow 扩展 import**：r2dbc Query 是 Flow，`.map{}` 后需 `.toList()` 才是 List；补 `kotlinx.coroutines.flow.{map,toList,firstOrNull,any}` import。
3. **status 常量改 Short**：`const val TENANT_STATUS_ACTIVE: Short = 1`（short 列 set 需 Short）。
4. **resetSchema 改 suspend**：suspendTransaction 须在协程内。
5. **LabPg 顶层函数 import**：`LabPg` 是文件名不是类型，测试改按 `connectVerify/ensureVerifyDatabase/resetSchema` 顶层名直引。
6. **测试种子漏行修复（首跑红根因，rec-02 逐字留痕）**：MeApiTest 文档口径「userX 同属 A、B」但 setUp 漏插 userX 在 B 的成员行 → 第二次 switchTenant（换到 B）按成员资格门槛**正确**拒绝（门槛本身行为正确，种子不满足前置）→ 补 `insertMember(tenantB, userX, "甲组兼乙组")`，B 上下文读数同步改为 M=2。修复的是测试夹具，不是被测逻辑。
7. **@BeforeTest 禁返回值**：JUnit 6 discovery 硬门（`must not return a value`）→ setUp 改语句体 `{ runBlocking { ... } }`（表达式体把末行 insertMember 的 Uuid 带成了方法返回类型）。

## 附：与案例仓口径差异（全部显式列出）

| 项 | 案例仓 | 本 lab | 性质 |
|----|--------|--------|------|
| HTTP/服务器面 | Ktor 3.6 + testApplication + RoutingCall | 零 Ktor，测试服务层直调 | 教学裁剪 |
| JWT | jjwt 签发/解码（JwtIssuer） | token 为构造串（`lab-at-…`），不背 JWT 依赖 | 教学裁剪 |
| 数据库接入 | exposed-r2dbc 连 saas_dev 业务库 | 同 exposed-r2dbc 栈，连章级验证库 ch33_verify | lab 口径（ch29 同款） |
| impl 方法面 | 只覆盖 list/switch 等已实现 op | +create/disable/delete 生命周期 op（fastapi tenants_impl 语义） | 教学补齐（33 章正文需要） |
| 隔离查询入口 | 无独立入口 | listMembersInContext/readMemberInContext/renameMemberInContext（恒拼 tenant_id 谓词） | 教学补齐（行级隔离演示件） |
| 异常面 | NotFoundError/BadRequestError HTTP 异常族 | TenantSwitchException(NOT_FOUND/BAD_REQUEST) | 教学裁剪（语义等价） |
| 模型注解 | @Serializable + BadParameterException | 纯数据类 + require 风格 IllegalArgumentException | 教学裁剪（ch32 同款） |
| OpenAPI 生成注释 | 生成器头注释 | 无 | 教学裁剪 |
| wrapper | gradlew 9.8.1 | 无 wrapper（沿 kotlin-book lab 约定） | lab 约定 |

版本口径对码结果：Kotlin 2.4.21 / exposed-bom 1.5.1 / r2dbc-postgresql 1.1.3.RELEASE / junit-bom 6.1.3 / JDK 21（Launcher JVM 21.0.6）全部一致，零偏差。

## 附：回执总检

- 逐字纪律：rec-01~rec-06 均为单轮整块输出重定向（`> _work/rec-*.log 2>&1` 前缀 exit code 首行），无跨轮拼合；首跑红 rec-02 为当时逐字留痕（6 tests completed, 2 failed，种子漏行）。
- 环境注记：系统 Gradle 9.8.1 直跑；依赖全走本地缓存零联网拉包（ch29 已实证的 r2dbc 栈），代理四变量未触碰；中文注释/输出无 U+FFFD。
- 边界纪律：PG 仅本机 127.0.0.1:54321，验证库 kotlin_book_ch33_verify 工程自建；共享 PG 100.79.128.25 全程未出现；案例仓只读零写入（全程只 cat/find）；新建文件仅限本工程目录与 `lab/ch33/`；仓根 README 未动（留编排臂登记）。
- 数据可重入：每轮测试 setUp 先 drop+create 全量重来，`gradle test` 可无限重跑（rec-05 clean 后复验已证）。
