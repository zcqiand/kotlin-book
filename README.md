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
