# -p 一次性建出整条目录链, 父目录不存在时自动补齐, 重复执行也不报错
mkdir -p hello-gradle/src/main/kotlin/hello

# 后续所有 Gradle 命令都以工程根为工作目录, 先切进去
cd hello-gradle