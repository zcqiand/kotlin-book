# kotlin-book — xr-know-020《Kotlin 从入门到项目实践》随书代码仓

独立 git 仓（xr-knowledge-suite 主仓 .gitignore 零跟踪）。版本口径：Kotlin 2.4.x / JDK 21 / Gradle 9.8.1（见主仓 `output/xr-know-020/version-lock.json`）。

## lab 约定

- 章级验证工程一律放 `lab/chNN/<工程名>/`；一章可含多工程，各工程独立可运行、自带 `.gitignore` 排除构建产物（`build/`、`.gradle/`）。
- 工程内 `receipts.md` 保存实测回执（正例 exit 0 / 反例编译失败 exit 1 逐字），与章正文代码清单对码。
- 验证跑法：系统 Gradle 9.8.1 直跑（不用 wrapper），`JAVA_TOOL_OPTIONS=-Dstdout.encoding=UTF-8` 保中文回执不乱码。

## 已登记

| 章 | 工程 | 内容 |
|----|------|------|
| ch11 | `lab/ch11/trio-demo/` | data class / sealed class 状态机 / object 三件套演示 + when 漏分支编译失败回执 |
| ch12 | `lab/ch12/ext-demo/` | String 业务扩展 + 静态解析演示 + 局限反例（私有访问/open 修饰编译失败回执 + shadow 告警实捕） |
| ch13 | `lab/ch13/generics-delegate-demo/` | 泛型/型变/reified/委托四示例 + Java 泛型编不过现场（BrokenForward 雪崩）+ 擦除反例（PlainOfType erased type 回执） |
| ch14 | `lab/ch14/operator-scope-demo/` | 运算符重载/infix/五作用域函数对照 + 双反例（INAPPLICABLE_OPERATOR_MODIFIER / INAPPLICABLE_INFIX_MODIFIER 编译失败回执 + 复绿） |
| ch15 | `lab/ch15/library-model-demo/` | 卷二实战：图书管理领域建模（data class/sealed 状态机/扩展）+ JUnit 6.1.3 全绿 10 测试 + 先红后绿两轮回执（kotlin.test assertFailsWith） |
| ch16 | `lab/ch16/suspend-demo/` | 卷三分水岭：suspend 首例+线程释放时间线（见证协程 +216ms）+单线程十万协程数量级+P05 三步判定法演练+ILLEGAL_SUSPEND_FUNCTION_CALL 编译失败回执复绿（kotlinx-coroutines 1.11.0，Ktor bom 依赖树实测） |
| ch17 | `lab/ch17/structured-concurrency-demo/` | 卷三：结构化并发不泄漏五演示（泄漏对照/父子作用域取消联动/P06 launch-async 选型/取消传播链/异常边界）+runBlocking 单线程自锁桥翻现场修复回执（五演示全绿 EXIT=0） |
| ch18 | `lab/ch18/flow-demo/` | 卷三：Flow 响应式数据流六演示（冷热对照 P07/StateFlow 状态承载 P08 前半/SharedFlow 事件广播 P08 后半/操作符族 debounce 静默窗/三问定流）+裸 launch DEPRECATION_ERROR 升级实录改 CoroutineScope 扩展函数（两跑 EXIT=0，kotlinx-coroutines 1.11.0） |
| ch19 | `lab/ch19/java-interop-demo/` | 卷三：Java/Kotlin 混编互操作（双向互调 getter-setter 属性映射/平台类型 String! 三处置 P09/@JvmStatic-@JvmOverloads-@JvmField 逐注解/增量迁移全流程）+混编构建次序实测（Java 编译依赖 Kotlin 产物）+@JvmField+private INAPPLICABLE_JVM_FIELD 反例回执复绿（EXIT=0） |
| ch20 | `lab/ch20/multi-module-demo/` | 卷三：Gradle 多模块工程化（libs.versions.toml 收纳/api-implementation 可见性收敛/settings 拆分 :core+:app/验收搜索七落点）+反例 A UNRESOLVED_IMPORT 与反例 B INVISIBLE_REFERENCE 逐字回执复绿+12 条工程断言全实测出处（EXIT=0） |
| ch21 | `lab/ch21/quality-gate-demo/` | 卷三收官：JUnit 6 参数化测试+断言风格演练+ktfmt/detekt 双静态关+四级门控流水线（gradle check 串联）；六级逐字回执（测试绿/断言红复绿/ktfmt 红复绿/detekt 红复绿/check/build）；ktfmt 0.27.0=插件坐标（引擎另版）申报节 |
| ch22 | `lab/ch22/hello-ktor/` | 卷三服务端段开章：Ktor 3.6.0 第一个 HTTP 服务（EngineMain+application.conf 六端点/路由/ContentNegotiation+serialization）；本书 lab 首个 version catalog（gradle/libs.versions.toml version.ref+BOM platform 双收口）；热重载同 PID 回执+embeddedServer 8091 对照三发+反例 Task 'run' not found 复绿+curl 六发逐字回执 |
| ch23 | `lab/ch23/exposed-crud/` | 数据访问章：Exposed 1.5.1（exposed-bom 收口）+本机 PG 127.0.0.1:54321（BooksTable schema/CUD 全链+transaction 有无双向实证 rec3-tx vs rec-no-tx）；SQL 拼接注入反例三发（SqlConcatDemo 发1/2/3）→参数化复绿；junit-bom 6.1.3 BooksCrudTest tests=3 对账回执；本书 lab 第二个 version catalog（r2dbc-postgresql 1.1.3.RELEASE BOM 外钉 exact）；练习 stock 列迁移回执 |
