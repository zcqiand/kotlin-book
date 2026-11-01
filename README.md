# Kotlin 从入门到项目实践 - 代码清单

## 关于本书

本书是一本基于 Kotlin 2.4.x（K2 编译器）+ JDK 21 的系统性 Kotlin 教程，全书四卷 35 章，从语言心智模型讲起，覆盖协程与结构化并发、Flow、Gradle 多模块工程化与 Ktor 服务端开发，最终落到两个企业级项目实战。面向零基础或仅有少量编程经验（如一点 Java 基础）的初学者，以及想从 Java 迁移到 Kotlin 的后端开发者。每个技术选型都附带适用场景与选择理由，每章交付一个可操作的成果。

## 本书特点

- 基于当前稳定版 Kotlin 2.4.x（K2 编译器）与 JDK 21，示例代码全部从真实案例仓转写、可运行，无版本错位。
- 决策框架驱动：空安全调用方式、作用域函数五选一、Flow 三种流选型等关键选型均给出决策框架，不只教怎么写，还教什么时候写、为什么这样写。
- 以第 4 章空安全与第 16-17 章协程/结构化并发两道分水岭组织全书重点，帮助读者从「会写语法」进入「工程思维做事」。
- 零基础与 Java 迁移双入口：从环境搭建讲起，不假设读者写过 Kotlin；有 Java 背景的读者可走第 19 章 Java 互操作专路与第 26 章 Spring Boot 4 桥接。

## 案例仓库

| 仓库名                            | 说明                                                             |
| :-------------------------------- | :--------------------------------------------------------------- |
| lab-management-system-kotlin      | 实验室管理系统（Ktor + Exposed + PostgreSQL），对应第 28~31 章     |
| saas-identity-platform-kotlin     | SaaS 多租户身份平台（Ktor + JWT + 租户隔离），对应第 32~35 章      |

> 两个案例仓均为 clone 后按各自 README 启动即可跑通的完整工程，包含完整测试与质量门控（ktfmt / detekt / 编译 / 测试），无第三方 API Key 依赖。

## 代码清单说明

本书所有代码清单均收录于本目录，对应书稿中「代码清单 N-M」标题块。

### 运行环境

```bash
- JDK：21
- IDE：IntelliJ IDEA（社区版即可）
- 构建工具：Gradle 9.8.1（Kotlin DSL）
- 语言与核心依赖：Kotlin 2.4.21、Ktor 3.6.0（ktor-bom 收口）、Exposed 1.5.1（exposed-bom 收口）、kotlinx-coroutines 1.11.0、kotlinx-serialization 1.9.0、JUnit 6.1.3
- 数据库：PostgreSQL（数据访问层经 R2DBC 驱动连接）
```

### 目录结构

```
src/
├── 代码清单1-* … 代码清单34-*   # 第 1-34 章，共 144 个清单文件（命名「代码清单N-M_ 描述.扩展名」）
└── extracted_code_manifest.json   # 全部清单索引（title/lang/chapter_file/line/extracted_file/source）
```
