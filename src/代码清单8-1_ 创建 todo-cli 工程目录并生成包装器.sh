# -p 一次建出整条目录链，重复执行不报错；包名 todo 与第 2 章的 hello 同一惯例
mkdir -p todo-cli/src/main/kotlin/todo

# 后续所有命令都以工程根为工作目录
cd todo-cli

# 包装器生成沿用第 2 章清单 2-6 的办法，把 Gradle 9.8.1 钉进工程
gradle wrapper --gradle-version 9.8.1