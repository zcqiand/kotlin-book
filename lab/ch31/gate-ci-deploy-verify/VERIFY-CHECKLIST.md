# ch31 gate-ci-deploy-verify — clone 即跑复验清单

从零复验路径：JDK 21 → 拉工程 → 四级门控 → 起 PG 16 → 起应用 → 冒烟。
每步给出的命令在本机 2026-11-01 已真实执行过一遍（读数见 `_work/receipts.md`）。

## 0. 前置准备

| 步骤 | 命令 | 期望 |
|------|------|------|
| 0.1 JDK 21 | `java -version` | `21.0.x`（本机 21.0.6） |
| 0.2 Gradle | `gradle --version` | `Gradle 9.8.1`（系统直跑，工程不带 wrapper，沿 kotlin-book lab 约定） |

## 1. 拉工程

```bash
git clone <工程所在仓> && cd kotlin-book/lab/ch31/gate-ci-deploy-verify
```

（本 lab 随书仓内开发，复验=检出版工程目录即可；文件清单见 `_work/receipts.md` 第 0 节。）

## 2. 四级门控一条龙

```bash
gradle ktfmtCheck detekt test build --console=plain
```

期望 exit 0、`BUILD SUCCESSFUL`。四级各自的单独跑法与首轮失败自愈实录：

| 级 | 命令 | 首轮 | 自愈后 |
|----|------|------|--------|
| ① 格式门 | `gradle ktfmtCheck --console=plain` | exit 1（2 文件非法格式） | `gradle ktfmtFormat` 后 exit 0 |
| ② 静态门 | `gradle detekt --console=plain` | exit 1（1 问题：DestructuringDeclarationWithTooManyEntries） | 改直取属性后 exit 0 |
| ③ 测试门 | `gradle test --console=plain` | exit 1（1/5 失败：用户名「李工」2 字符低于阈值） | 修测试数据后 exit 0 |
| ④ 构建门 | `gradle build --console=plain` | — | exit 0（jar 11KB + dist zip 2.9MB） |

## 3. 起 PG 16（容器化方案）

```bash
docker compose -f deploy/docker-compose.yaml up -d pg_ch31_verify
docker ps --filter name=pg_ch31_verify   # 期望 Up (healthy)，端口 127.0.0.1:15439->5432
```

命名纪律：容器 `pg_ch31_verify` / 库 `lab_ch31_verify` / 用户 `lab_ch31_verify`，一律带 `_ch31_verify` 隔离后缀；端口绑 `127.0.0.1:15439`。

**本机 Docker 缺位时的等效路线**（本次实测走的就是这条，读数见 receipts 实测件 9-12）：PostgreSQL 16.15 便携二进制（zonky windows-amd64 16.15.0）`initdb` + `pg_ctl -o "-p 15439 -h 127.0.0.1"` 起在本机高位口，库名/用户名同上；首次需建库建用户（无 psql 客户端时可用 JDBC 一条龙：连 postgres 库执行 `CREATE USER` / `CREATE DATABASE`）。

## 4. 起应用 + 冒烟

```bash
export DATABASE_URL="postgresql://lab_ch31_verify:ch31_verify@127.0.0.1:15439/lab_ch31_verify"
gradle run --args="db-check" --console=plain
```

期望三连回执（exit 0）：

```
[建表] login_audit 就绪
[插数] 1 行（username=ch31_verify）
[查询] login_audit 共 N 行     ← 重跑累计 +1（CREATE TABLE IF NOT EXISTS 幂等，INSERT 不去重）
```

教学注记：本最小工程的应用面是 CLI，冒烟=上述三连回执；HTTP 冒烟（`/health` + swagger 文档面三件 200）的完整形态见案例仓 `.github/workflows/ci.yml` 镜像装配冒烟步。

## 5. 环境回执

| 步骤 | 本机实测读数 |
|------|------|
| 0.1 | java 21.0.6 LTS |
| 0.2 | Gradle 9.8.1（JVM 21.0.6） |
| 2 | 四级一条龙 exit 0（rec-13，clean 后全任务实跑） |
| 3 | PostgreSQL 16.15 on 127.0.0.1:15439（rec-11）；db-check 三连 exit 0 ×2（rec-09/rec-12） |
| 4 | demo 四案例全「通过」（rec-10） |
