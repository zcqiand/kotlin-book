# ch20 multi-module-demo 实跑回执（receipts）

- A15 仓身份先验：python `os.path.isdir('.git')` 实测输出 `git isdir: True`（cwd = output/xr-know-020/code/kotlin-book），仓身份成立后才动工
- 工程：`D:\qiand-life\1-projects\xr-knowledge-suite\output\xr-know-020\code\kotlin-book\lab\ch20\multi-module-demo`
- 实跑日期：2026-11-01
- 环境：Windows 11 Pro 10.0.22631 / JDK 21.0.6 / Gradle 9.8.1（wrapper，与 ch16 同源腾讯镜像收编）/ Kotlin JVM 插件 2.4.21（版本目录 alias）/ slf4j 2.0.16 / kotlinx-coroutines-core 1.11.0
- 重构基线申报：ch2 书稿工程 hello-gradle（chapter-002.md 清单 2-3/2-4/2-5）在 kotlin-book 仓盘上已无对应 lab 目录（os.walk 全仓核对，仅存 ch11-ch19），本工程按 ch2 骨架的段式约定（settings 登记 + 插件/工具链/依赖/入口四段 + App 三行输出）重构拆分，非直接复制盘上文件
- 命令形式：全程经 python subprocess.run 调 `gradlew.bat <task> --console=plain`（Bash 权限限 python），工作目录 = 工程根，输出逐字节存档 `_work/logs/`
- 退出码汇总：反例 A/B 失败轮 **exit 1** → 修复后复绿 **exit 0**；终态 `:app:run` 与 `clean :app:run` 均 **exit 0**
- 过程性修复申报（属搭建期脚手架错误，非教学实测矩阵，不入清单）：首次构建因子工程未声明仓库报 "Cannot resolve external dependency ... because no repositories are defined"（Gradle 9 多模块口径），改为 settings `dependencyResolutionManagement` 收敛声明后通过；同轮还清掉了入口误留的 runBlocking 直引

---

## 实测 1：正例基线（S0，implementation 态）——`gradlew.bat :app:run`

命令：`gradlew.bat :app:run --console=plain`，exit **0**。:core 以 implementation 引 slf4j-api，:app 只调公开 API，构建运行全绿，:core 日志经 slf4j-simple 落 stderr。

逐字输出：

```
> Task :app:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :core:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :core:processResources NO-SOURCE
> Task :app:processResources NO-SOURCE
> Task :core:compileKotlin
> Task :core:compileJava NO-SOURCE
> Task :core:classes UP-TO-DATE
> Task :core:jar
> Task :app:compileKotlin
> Task :app:compileJava NO-SOURCE
> Task :app:classes UP-TO-DATE

> Task :app:run
========== multi-module-demo: :app -> :core ==========
hello-multi: ch20 的多模块工程已就绪
Kotlin 运行库版本: 2.4.21
JVM 版本: 21.0.6

BUILD SUCCESSFUL in 19s
4 actionable tasks: 4 executed
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

stderr（逐字）：

```
[main] INFO hello.core.GreetingService - greet() 完成, 目标 = ch20
```

## 实测 2：反例 A（implementation 挡传递可见性）——`gradlew.bat :app:compileKotlin`

前置改动：:app 的 Main.kt 加 `import org.slf4j.LoggerFactory` 并实际调用（:app 未自行声明 slf4j 依赖），exit **1**。

逐字输出（stderr 主体 + 关键 stdout）：

```
> Task :app:compileKotlin FAILED
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/output/xr-know-020/code/kotlin-book/lab/ch20/multi-module-demo/app/src/main/kotlin/hello/Main.kt:6:12 Unresolved reference 'slf4j'.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/output/xr-know-020/code/kotlin-book/lab/ch20/multi-module-demo/app/src/main/kotlin/hello/Main.kt:10:21 Unresolved reference 'LoggerFactory'.

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:compileKotlin' (registered by plugin 'org.jetbrains.kotlin.jvm').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: UNRESOLVED_IMPORT
       Kotlin compiler error
         Unresolved reference 'slf4j'.
         Location: D:/qiand-life/1-projects/xr-knowledge-suite/output/xr-know-020/code/kotlin-book/lab/ch20/multi-module-demo/app/src/main/kotlin/hello/Main.kt:6

BUILD FAILED in 7s
```

要点：implementation 下报错形态是 **UNRESOLVED_IMPORT / Unresolved reference**（依赖根本不在 :app 编译 classpath），不是权限类报错。

## 实测 3：反例 A 复绿（:core 改 java-library + api）——`gradlew.bat :app:run`

前置改动：:core 的 build.gradle.kts 插件段加 `id("java-library")`，`implementation(libs.slf4j.api)` 改 `api(libs.slf4j.api)`，:app 代码不动，exit **0**。stdout 尾段与 stderr 逐字：

```
> Task :app:compileKotlin

> Task :app:run
========== multi-module-demo: :app -> :core ==========
hello-multi: ch20 的多模块工程已就绪
Kotlin 运行库版本: 2.4.21
JVM 版本: 21.0.6

BUILD SUCCESSFUL in 10s
4 actionable tasks: 3 executed, 1 up-to-date
```

stderr（逐字，:app 自己的日志这次也出来了）：

```
[main] INFO hello.MainKt - 多模块演示开始
[main] INFO hello.core.GreetingService - greet() 完成, 目标 = ch20
```

## 实测 4：反例 B（internal 拦人）——`gradlew.bat :app:compileKotlin`

前置改动：:app 的 Main.kt 增加直接调用 `service.buildBanner("Gradle", "9.8.1")`（该成员在 :core 标 internal），exit **1**。

逐字输出（stderr 主体 + 关键 stdout）：

```
> Task :app:compileKotlin FAILED
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/output/xr-know-020/code/kotlin-book/lab/ch20/multi-module-demo/app/src/main/kotlin/hello/Main.kt:16:21 Cannot access 'fun buildBanner(tool: String, version: String): String': it is internal in 'hello.core.GreetingService'.

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:compileKotlin' (registered by plugin 'org.jetbrains.kotlin.jvm').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: INVISIBLE_REFERENCE
       Kotlin compiler error
         Cannot access 'fun buildBanner(tool: String, version: String): String': it is internal in 'hello.core.GreetingService'.
         Location: D:/qiand-life/1-projects/xr-knowledge-suite/output/xr-know-020/code/kotlin-book/lab/ch20/multi-module-demo/app/src/main/kotlin/hello/Main.kt:16

BUILD FAILED in 7s
```

要点：internal 报错形态是 **INVISIBLE_REFERENCE / it is internal in**（可见性拦人，与反例 A 的 UNRESOLVED_IMPORT 形态可对照）；Kotlin 的 internal 按**编译单元**划界，:app 与 :core 是两个 Gradle 子工程即两个编译单元，跨模块 internal 不可见。

## 实测 5：反例 B 复绿（internal 改 public）——`gradlew.bat :app:run`

前置改动：:core 的 buildBanner 去掉 internal 修饰，:app 调用不动，exit **0**。关键输出逐字：

```
> Task :app:run
========== multi-module-demo: :app -> :core ==========
hello-multi: ch20 的多模块工程已就绪
[Gradle] 版本钉子 = 9.8.1 (由 :core 生成)
Kotlin 运行库版本: 2.4.21
JVM 版本: 21.0.6

BUILD SUCCESSFUL in 8s
4 actionable tasks: 4 executed
```

## 实测 6：终态清洗后 :app:run——exit **0**

终态清洗：Main.kt 横幅版本值改取 `KotlinVersion.CURRENT` 运行时读数（源码不留版本字面量）；:core 注释里的版本字面量同步擦除。关键输出逐字：

```
> Task :app:run
========== multi-module-demo: :app -> :core ==========
hello-multi: ch20 的多模块工程已就绪
[Kotlin] 版本钉子 = 2.4.21 (由 :core 生成)
JVM 版本: 21.0.6

BUILD SUCCESSFUL in 8s
4 actionable tasks: 2 executed, 2 up-to-date
```

## 实测 7：clean 全量构建（独立可复现）——`gradlew.bat clean :app:run`，exit **0**

逐字输出：

```
> Task :app:clean
> Task :core:clean
> Task :app:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :core:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :app:processResources NO-SOURCE
> Task :core:processResources NO-SOURCE
> Task :core:compileKotlin
> Task :core:compileJava NO-SOURCE
> Task :core:classes UP-TO-DATE
> Task :core:jar
> Task :app:compileKotlin
> Task :app:compileJava NO-SOURCE
> Task :app:classes UP-TO-DATE

> Task :app:run
========== multi-module-demo: :app -> :core ==========
hello-multi: ch20 的多模块工程已就绪
[Kotlin] 版本钉子 = 2.4.21 (由 :core 生成)
JVM 版本: 21.0.6

BUILD SUCCESSFUL in 8s
6 actionable tasks: 6 executed
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.1/userguide/configuration_cache_enabling.html
```

## 验收：全局搜索裸写版本号——构建脚本与源码 0 命中

搜索方法（python os.walk 全工程，排除 .git/build/.gradle/.kotlin/_work）：

- 搜索字面量：`2.4.21`、`1.11.0`、`2.0.16`、`9.8.1`（version-lock.json 四项锁定值）
- 档位划分：`gradle/libs.versions.toml`（版本目录，唯一登记处）与 `gradle/wrapper/gradle-wrapper.properties`（Gradle 发行包 URL，钉的是构建工具自身而非依赖坐标）为合法居留地；其余 build 脚本（*.gradle.kts）与全部源码（*.kt）计违例档

实测结果：**违例档 0 命中**；合法档 7 命中（toml 6：kotlin 2.4.21 / slf4j 2.0.16 / kotlinxCoroutines 1.11.0 各一处声明 + 文件头口径注释一处；wrapper properties 1：distributionUrl 的 9.8.1）。

---

## 实测断言登记（frag 清单引用底账）

| # | 断言 | 实测出处 |
|---|------|---------|
| 1 | S0 态 implementation 下 :app:run 全绿，:core 日志走 slf4j-simple 落 stderr | 实测 1 |
| 2 | implementation 挡传递：下游直引报 UNRESOLVED_IMPORT "Unresolved reference"，exit 1 | 实测 2（清单 20-5） |
| 3 | :core 改 java-library + api 后同代码复绿 exit 0，下游日志出现 | 实测 3（清单 20-6） |
| 4 | internal 跨模块拦截报 INVISIBLE_REFERENCE "it is internal in"，exit 1 | 实测 4（清单 20-8） |
| 5 | internal 改 public 复绿 exit 0 | 实测 5（清单 20-8 续） |
| 6 | clean 全量构建独立可复现 exit 0 | 实测 7（清单 20-9） |
| 7 | 构建脚本与源码裸写版本号 0 命中，仅 toml + wrapper properties 合法居留 | 验收节（清单 20-9） |
