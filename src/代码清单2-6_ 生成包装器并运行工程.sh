# gradle wrapper 把 Gradle 9.8.1 钉进工程: 包装器文件入库后,
# 任何人克隆仓库都会拿到同一个 Gradle 版本, 机器上装没装 Gradle 都无所谓
gradle wrapper --gradle-version 9.8.1

# run 任务由 application 插件提供: 编译、定位 mainClass、运行一条龙。
# Windows 下把 ./gradlew 换成 gradlew.bat 即可, 其余参数完全不变
./gradlew run