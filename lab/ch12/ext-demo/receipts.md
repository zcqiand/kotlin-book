# ch12 ext-demo 实测回执

> 版本口径：Kotlin 2.4.21 / JDK 21（jvmToolchain）/ Gradle 9.8.1 系统直跑（不用 wrapper）。
> 以下全部回执由实测臂回填，coder 臂不产出任何运行输出。
> 体例：Gradle 任务播报行从略；工程路径缩写为 `lab/ch12/ext-demo`；每条回执保留命令行、关键输出行与 EXIT 值，逐字记录（`Picked up JAVA_TOOL_OPTIONS` 行与配置缓存提示行省略）。

## 章节对码表

| 清单 | 验证方式 | 回执 |
|------|----------|------|
| 清单12-1（扩展函数/扩展属性） | `gradle run --args="string"` 直跑 | 见回执一 |
| 清单12-3（静态解析/成员胜出） | `gradle run --args="resolve"` 直跑 | 见回执二 |
| 清单12-5（反例：私有成员红线 + open 扩展红线） | 反例临时并入 src 后 `gradle compileKotlin` 必须失败 | 见回执三 |
| 反例移除后复绿 | 删除反例文件后 `gradle compileKotlin` 恢复 exit 0 | 见回执四 |

## 回执一：清单12-1 扩展函数/扩展属性（string）

```
$ cd lab/ch12/ext-demo
$ export JAVA_TOOL_OPTIONS=-Dstdout.encoding=UTF-8
$ gradle run --args="string" ; echo "EXIT=$?"

w: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch12/ext-demo/src/main/kotlin/demo/ExtMain.kt:39:13 This extension is shadowed by a member: 'fun tag(): String' defined in 'demo.Channel'.

脱敏=138****5678，短号原样=95588，短信尾号=5678

BUILD SUCCESSFUL in 23s
EXIT=0
```

- 输出与预期逐字一致。
- 附带：compileKotlin 期产生一条 Kotlin 告警（Channel 同名扩展被成员遮蔽，w: 行全文如上），照实记录，正文需交代编译器附提醒。

## 回执二：清单12-3 静态解析/成员胜出（resolve）

```
$ gradle run --args="resolve" ; echo "EXIT=$?"

声明 OrderState、实际 Cancelled：OrderState 扩展：全部状态通用
声明 Cancelled：Cancelled 扩展：仅已取消可见
成员与扩展同名：成员函数

BUILD SUCCESSFUL in 6s
EXIT=0
```

- 三行分派与预期逐字一致（compileKotlin UP-TO-DATE，无新增告警）。

## 回执三：清单12-5 反例编译失败（私有成员红线 + open 扩展红线）

```
$ cp negatives/BrokenExt.kt.txt src/main/kotlin/demo/BrokenExt.kt
$ gradle compileKotlin ; echo "EXIT=$?"

> Task :compileKotlin FAILED
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch12/ext-demo/src/main/kotlin/demo/BrokenExt.kt:5:43 Cannot access 'val code: String': it is private in 'demo.SecretBox'.
e: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch12/ext-demo/src/main/kotlin/demo/BrokenExt.kt:13:1 Modifier 'open' is not applicable to 'top level function'.

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':compileKotlin' (registered by plugin 'org.jetbrains.kotlin.jvm').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: INVISIBLE_REFERENCE
       Kotlin compiler error
         Cannot access 'val code: String': it is private in 'demo.SecretBox'.
         Location: D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch12/ext-demo/src/main/kotlin/demo/BrokenExt.kt:5

BUILD FAILED in 7s
EXIT=1
```

- 两条 e: 报错逐字在案，位置分别为 BrokenExt.kt:5:43（私有成员访问）与 BrokenExt.kt:13:1（open 修饰顶层扩展）。
- 侧记：Gradle 的 What went wrong 段对第一条报错做了重复展开（INVISIBLE_REFERENCE 块），系 Gradle 9.8.1 错误报告格式，非额外报错。

## 回执四：反例移除后复绿

```
$ rm src/main/kotlin/demo/BrokenExt.kt
$ gradle compileKotlin ; echo "EXIT=$?"

> Task :compileKotlin UP-TO-DATE

BUILD SUCCESSFUL in 7s
EXIT=0
```

- EXIT=0；Gradle 因源集回到上次成功编译状态报 UP-TO-DATE（未重编译）。
- 为排除 UP-TO-DATE 的目击性疑义，补跑一次全量编译作旁证：

```
$ gradle clean compileKotlin ; echo "EXIT=$?"

w: file:///D:/qiand-life/1-projects/xr-knowledge-suite/code/kotlin-book/lab/ch12/ext-demo/src/main/kotlin/demo/ExtMain.kt:39:13 This extension is shadowed by a member: 'fun tag(): String' defined in 'demo.Channel'.

BUILD SUCCESSFUL in 7s
EXIT=0
```

- clean 全量重编译 EXIT=0，复绿坐实；仅余回执一所述 shadow 告警，属预期内。
