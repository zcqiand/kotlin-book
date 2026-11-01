# ch34 identity-auth-rbac-lab 实测回执

- 日期：2026-11-01
- 工程：`output/xr-know-020/code/kotlin-book/lab/ch34/identity-auth-rbac-lab/`
- 运行方式：系统 Gradle 9.8.1 直跑（不用 wrapper，沿 kotlin-book 仓 README lab 约定），`JAVA_TOOL_OPTIONS=-Dstdout.encoding=UTF-8`，`--console=plain`。数据库：**本机 PG 127.0.0.1:54321（postgres/postgres，ch29/ch33 同口径）**，验证库 `kotlin_book_ch34_verify`（由工程 `ensureVerifyDatabase()` 自建，ch33 LabPg 镜像）；共享 PG 100.79.128.25 全程零触碰，案例仓 `saas-identity-platform-kotlin`（tag v0.2.1-20261025）只读零修改。端口零占用：不拉 HTTP，测试服务层直调（ch33 先例）。
- 对码：案例仓五绑定文件对码——`impl/JwtIssuer.kt` 逐行复刻（HS256 签发/验签 + `require(bytes.size >= 32)` 硬约束，claims iss/aud/sub/tenant_id/jti + iat/nbf/exp）；`impl/TokenPair.kt` 双令牌落库语义复刻（access 真 JWT + refresh 随机串落库绑定 user/tenant，TTL 改第 24 章教学口径 300s/7天）；`impl/OauthApiServiceImpl.kt` 复刻 rotateRefreshToken（撤旧签新、旧 rt 再用 INVALID_GRANT）与 exchangeAuthorizationCode（一次性 code、CODE_TTL_MINUTES=5、换发即删行防重放）；`impl/MeApiServiceImpl.kt` 复刻 member→role→menu 四级 inList 矩阵装配（教学化简为 RbacService.effectiveMenus/effectivePerms）；`impl/Security.kt` 复刻 requireBearer 收敛 401 + 教学补 requirePermission 403 通道。jjwt 三件 0.12.6（api=implementation、impl/jackson=runtimeOnly）版本号只在 libs.versions.toml。
- 体例：沿 ch33 receipts（`_work/` 收 rec-*.log，首行=exit code，正文逐字原始输出；本 receipts.md 落工程根）。

## 第 0 节：文件清单

```
identity-auth-rbac-lab/
├── .gitignore                        （build/ .gradle/ .kotlin/ _work/，沿 ch30-ch33 先例）
├── build.gradle.kts                  （Kotlin 2.4.21 + exposed-bom 1.5.1 + jjwt 0.12.6 + junit-bom 6.1.3）
├── settings.gradle.kts
├── gradle/libs.versions.toml         （ch33 基线 + jjwt 三件，版本唯一事实源）
├── src/main/kotlin/saas/identity/
│   ├── entity/
│   │   ├── SysRoleTable.kt           （sys_role：id/tenant_id/role_code/role_name/created_at）
│   │   ├── SysMenuTable.kt           （sys_menu：id/menu_name/perms（「域:动作」串）/created_at）
│   │   ├── SysRoleMenuTable.kt       （sys_role_menu：role_id→menu_id 多对多）
│   │   ├── TenantMemberRoleTable.kt  （成员→角色绑定，教学合并 userId+tenantId）
│   │   ├── RefreshTokenTable.kt      （refresh 随机串落库，绑定 user/tenant，revoked 撤旧）
│   │   └── AuthCodeTable.kt          （authorize 一次性 code，绑定 user/tenant/client）
│   ├── shared/                       （本 lab 无模型文件，目录留空未用）
│   ├── impl/
│   │   ├── JwtIssuer.kt              （HS256 签发/验签，≥32B require，TTL 参数注入）
│   │   ├── TokenIssuer.kt            （issueTokenPair + rotate 撤旧签新；TTL 300s/7天常量）
│   │   ├── AuthService.kt            （login / refresh / decodeAccess 收敛 401）
│   │   ├── SsoService.kt             （issueAuthorizationCode / exchangeCode 一次性）
│   │   ├── RbacService.kt            （seedRole/seedMenu/grant/assignRole + 四级 inList 矩阵装配）
│   │   └── SecurityLab.kt            （requireBearer 401 通道 + requirePermission 403 通道）
│   └── infra/
│       ├── LabPg.kt                  （54321 连接 + ensureVerifyDatabase + resetSchema 六表）
│       └── AuthException.kt          （status=401 认证侧 / 403 授权侧）
├── src/test/kotlin/io/xr/harness/
│   ├── AuthFlowTest.kt               （实测件 1：双令牌链路，5 用例）
│   ├── SsoCodeTest.kt                （实测件 2：SSO 一次性 code，2 用例）
│   ├── RbacMatrixTest.kt             （实测件 3：RBAC 矩阵，2 用例）
│   └── AuthzBoundaryTest.kt          （实测件 4：401/403 边界，3 用例）
├── receipts.md                       （本文件）
└── _work/rec-*.log ×9
```

---

## 实测件 1：双令牌认证链路（AuthFlowTest，5 用例）

| 用例 | 实测读数 |
|------|----------|
| ttlConstantsFollowChapter24Convention | ACCESS_TTL_SECONDS=**300**（5 分钟）、REFRESH_TTL_SECONDS=**604800**（7 天） |
| loginIssuesVerifiedAccessTokenAndBoundRefresh | login 后 decode access：**sub=userId / tenant_id=tenantId / jti·iat·exp 非 null**；exp−iat=**300 秒**；refresh 行落库绑定 user/tenant，revoked=**false**，前缀 `rt_` |
| expiredAccessRejectedThenRefreshExchangeSucceeds | 1 秒 TTL 变体 + sleep 1500ms → decode 抛 AuthException **status=401**，message=`invalid bearer token: ExpiredJwtException`；refresh 换新成功，新 access 可验签，新 access/refresh 均非旧串 |
| refreshRotateOneTimeUse | 第一次 refresh 成功（绑定不变）；**旧 rt 再用 → status=400**，message=`INVALID_GRANT: revoked refresh_token` |
| hs256SigningKeyMustBeAtLeast32Bytes | 31 字节密钥 → IllegalArgumentException 含 `>=32`；恰好 **32 字节**合法（边界值实测） |

## 实测件 2：SSO 单点登录一次性 code（SsoCodeTest，2 用例）

三次调用各一读数（authorizeCodeIsOneTime）：

1. **authorize 签发**：code=`code_<uuid>`，CODE_TTL_MINUTES=**5**（对码案例仓常量）。
2. **code 换双令牌**：成功——access 验签 sub/tenant_id 与 code 绑定一致，双令牌非同串。
3. **同 code 二次换发**：**status=400**，message=`INVALID_GRANT: code 不存在或已被使用`（换发即删行防重放）。

补充（unknownCodeAndClientMismatchRejected）：未知 code → 400 INVALID_GRANT；client 不匹配 → 同一 400 INVALID_GRANT 通道；正主 code 未被探测消费，仍可换发成功（探测面不误伤）。

## 实测件 3：RBAC 角色-权限-菜单矩阵（RbacMatrixTest，2 用例）

种子：3 菜单（`member:read` 会员档案 / `member:write` 会员编辑 / `report:issue` 报表签发）+ 两角色（tenant_admin 挂 3 菜单、member 挂 1 菜单）。

- tenant_admin：生效菜单数 **3**，perms 集合 = **{member:read, member:write, report:issue}**。
- member：生效菜单数 **1**，perms 集合 = **{member:read}**。
- 两角色矩阵不同（断言 assertNotEquals 通过），member perms 是 admin 子集。
- 无角色成员：生效菜单数 **0**（四级 inList 首级断流）。
- 租户隔离：同一 adminUser 在他租户矩阵 **0**、本租户 **3**（RBAC 也吃租户边界）。

## 实测件 4：认证 vs 授权边界 401/403（AuthzBoundaryTest，3 用例）

401 三种各一（authenticationSideRejectsThreeWaysWith401）：

| # | 输入 | 读数 |
|---|------|------|
| 1 | 无令牌（null） | status=**401**，message=`Bearer token required` |
| 2 | 坏签名（异密钥同 issuer 签发） | status=**401**，message=`invalid bearer token: SignatureException` |
| 3 | 过期令牌（1s TTL + 1500ms） | status=**401**，message=`invalid bearer token: ExpiredJwtException` |

403 一发（authorizationSideRejectsWith403ThenControlPasses）：

- member 角色令牌 requirePermission(`member:write`) → status=**403**，message=`permission denied: member:write`。
- 对照组边界内放行：requirePermission(`member:read`) → 返回 claims，sub 与身份一致。

通道顺序（authenticationPrecedesAuthorization）：无令牌 + 缺权限 → 先吃 **401**（认证先于授权，权限判定只对已认证身份生效）。

## 实测件 5：回归全绿 + 构建门

命令：`gradle test --console=plain`（exit 0）— `_work/rec-04-test-3rd.log`

```
BUILD SUCCESSFUL in 17s
4 actionable tasks: 2 executed, 2 up-to-date
```

全绿读数（`build/test-results/test/TEST-*.xml` testsuite 行逐字）：

```
<testsuite name="io.xr.harness.AuthFlowTest" tests="5" skipped="0" failures="0" errors="0" timestamp="2026-11-01T05:01:59.323Z" hostname="WIN-20240323MFF" time="3.956">
<testsuite name="io.xr.harness.AuthzBoundaryTest" tests="3" skipped="0" failures="0" errors="0" timestamp="2026-11-01T05:02:03.280Z" hostname="WIN-20240323MFF" time="2.308">
<testsuite name="io.xr.harness.RbacMatrixTest" tests="2" skipped="0" failures="0" errors="0" timestamp="2026-11-01T05:02:05.589Z" hostname="WIN-20240323MFF" time="1.434">
<testsuite name="io.xr.harness.SsoCodeTest" tests="2" skipped="0" failures="0" errors="0" timestamp="2026-11-01T05:02:07.023Z" hostname="WIN-20240323MFF" time="0.651">
```

**tests=12, failures=0, errors=0**。

构建门：`gradle build --console=plain`（exit 0）— `_work/rec-05-build.log`，产物 `build/libs/identity-auth-rbac-lab-0.1.0.jar`。clean 后一条龙：`gradle clean` + `gradle build test --console=plain`（均 exit 0）— `_work/rec-08-clean-build-test.log`，**clone 即跑路径成立**（BUILD SUCCESSFUL in 18s，4 tasks executed，12/12 复绿）。

## 实测件 6：环境读数

命令：`gradle --version`（exit 0）— `_work/rec-06-gradle-version.log`

```
Gradle 9.8.1
Kotlin:        2.4.10
Launcher JVM:  21.0.6 (Oracle Corporation 21.0.6+8-LTS-188)
```

与 ch31/ch32/ch33 复验清单读数一致（JVM 21.0.6）。

---

## 案例仓只读对码（正文用，全程只读零修改）

案例仓：`D:\qiand-life\1-projects\xr-code-suite\output\saas-identity-platform-kotlin`（tag **v0.2.1-20261025** 冻结）。五个文件路径与关键常量：

| 文件（src/main/kotlin/saas/identity/impl/ 下） | 关键内容 | 本 lab 对应 |
|------|----------|------------|
| `JwtIssuer.kt` | HS256 签发/验签；`HS256_MIN_KEY_BYTES=32`（require 硬约束）；claims iss/aud/sub/tenant_id/jti + iat/nbf/exp | 逐行复刻 |
| `TokenPair.kt` | `ACCESS_TTL_SECONDS=3600` / `REFRESH_TTL_SECONDS=30×24×3600`；access 行库列存占位 `"n/a"`（真签在 JWT）；rt 值 `rt_<uuid>` | 语义复刻，TTL 改教学口径 300/7天（第 24 章正文口径），lab 不建 access 行 |
| `OauthApiServiceImpl.kt` | `rotateRefreshToken` 撤旧签新（旧 rt revoked=true，再用 → `INVALID_GRANT: revoked refresh_token`）；`exchangeAuthorizationCode` 一次性消费（换发前 deleteCode 防重放）；`CODE_TTL_MINUTES=5` | 语义复刻（SsoService + TokenIssuer.rotate） |
| `MeApiServiceImpl.kt` | member→role→menu **四级 inList 矩阵装配**（memberIds→roleIds→menuIds→SysMenuTable，任一级空集短路）；查询恒 created_at ASC | 语义复刻（RbacService.effectiveMenus，教学化简 userId+tenantId 起步） |
| `Security.kt` | `requireBearer` 收敛 401（无头/非 Bearer/空串/一切验签失败 → InvalidCredentialsError，终审 I1 口径） | 语义复刻（SecurityLab.requireBearer）+ 教学补 requirePermission 403 |

## 附：命令与 exit code 清单

| # | 命令 | 日志 | exit |
|---|------|------|------|
| 1 | `gradle compileKotlin --console=plain`（首跑红） | `_work/rec-01-compile.log` | 1 |
| 2 | `gradle compileKotlin`（补 Uuid import 后） | `_work/rec-01b-compile-2nd.log` | 1 |
| 3 | `gradle compileKotlin`（补 flow import 后） | `_work/rec-01c-compile-3rd.log` | 0 |
| 4 | `gradle test --console=plain`（测试编译红） | `_work/rec-03-test-2nd.log` | 1 |
| 5 | `gradle test --console=plain`（修复后全绿） | `_work/rec-04-test-3rd.log` | 0 |
| 6 | `gradle build --console=plain` | `_work/rec-05-build.log` | 0 |
| 7 | `gradle --version` | `_work/rec-06-gradle-version.log` | 0 |
| 8 | `gradle clean` + `gradle build test --console=plain` | `_work/rec-07-clean.log` / `_work/rec-08-clean-build-test.log` | 0 / 0 |

（rec-02-test.log 为 rec-04 同轮的首次测试尝试留痕：AuthFlowTest 缺 flow firstOrNull import 红痕，保留逐字。）

## 附：修复清单（最小机械修复，逐笔）

1. **AuthService 漏 `kotlin.uuid.Uuid` import**：首跑 compileKotlin 红（rec-01，UNRESOLVED_REFERENCE）→ 补 import。
2. **r2dbc Query 是 Flow（ch33 同款坑复现）**：`.toList()/.firstOrNull()` Unresolved → RbacService/SsoService/TokenIssuer/AuthFlowTest 四处补 `kotlinx.coroutines.flow.{toList,firstOrNull}` import（ch33 修复清单第 2 条同款，未先吸取）。
3. **测试跨包误 import**：`import saas.identity.impl.AuthFlowTest`（AuthFlowTest 实际在 `io.xr.harness`）→ SsoCodeTest/AuthzBoundaryTest 同包删 import。

## 附：与案例仓口径差异（全部显式列出）

| 项 | 案例仓 | 本 lab | 性质 |
|----|--------|--------|------|
| HTTP/服务器面 | Ktor 3.6 + RoutingCall | 零 Ktor，服务层直调 | 教学裁剪（ch33 同款） |
| TTL 口径 | access 3600 / refresh 30 天 | access **300**（5 分钟）/ refresh **7 天**（第 24 章教学口径常量）；过期实测用 ttlSeconds=1 短变体 | 教学口径（派单指定） |
| 表面 | OauthAccessTokenTable/OauthRefreshTokenTable/OauthCodeTable + sys 全家桶 | RefreshTokenTable/AuthCodeTable + sys_role/sys_menu/sys_role_menu/tenant_member_role 六表 | 教学化简（不建 access 行，真签在 JWT 可验） |
| tenant_member_role 绑定 | memberId（成员行主键，前置 tenant_member 表） | userId+tenantId 二元绑定 | 教学合并 |
| requirePermission | 案例仓由路由层按菜单 perms 分发 | SecurityLab.requirePermission 直接查 effectivePerms | 教学补齐（第 30 章最小面升级口径） |
| 异常面 | InvalidCredentialsError(401)/BadRequestError HTTP 异常族 | AuthException(status=401/403/400) 单类 | 教学收敛（语义等价） |
| login 口令面 | 完整凭证校验链 | login 直接给定 user/tenant（不验口令哈希） | 教学裁剪（34 章正文焦点=令牌链路非口令存储） |
| wrapper | gradlew 9.8.1 | 无 wrapper（沿 kotlin-book lab 约定） | lab 约定 |

版本口径对码结果：Kotlin jvm 2.4.21 / exposed-bom 1.5.1 / r2dbc-postgresql 1.1.3.RELEASE / junit-bom 6.1.3 / **jjwt 0.12.6（案例仓同版本族）** / JDK 21（Launcher JVM 21.0.6），零偏差。

## 附：回执总检

- 逐字纪律：rec-01~rec-08 均为单轮整块输出重定向（`> _work/rec-*.log 2>&1` 前缀 exit code 首行），无跨轮拼合；首跑红 rec-01/rec-01b/rec-03 均为当时逐字留痕。
- 环境注记：系统 Gradle 9.8.1 直跑；exposed/r2dbc 栈走本地缓存，jjwt 0.12.6 三件本轮实拉（mavenCentral）；代理四变量未触碰；中文注释/输出无 U+FFFD（python 全文件扫描 0 命中）。
- 边界纪律：PG 仅本机 127.0.0.1:54321，验证库 kotlin_book_ch34_verify 工程自建；共享 PG 100.79.128.25 全程未出现；案例仓只读零写入（全程只 cat/find/grep）；不拉 HTTP（端口零占用）；不做任何 git 操作；新建文件仅限本工程目录与 `lab/ch34/`。
- 数据可重入：每轮测试 setUp 先 drop+create 六表全量重来，`gradle test` 可无限重跑（rec-08 clean 后复验已证）。
