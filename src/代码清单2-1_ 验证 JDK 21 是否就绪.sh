# 运行环境与编译器要分开验证: 只装了 JRE 的机器 java 有响应而 javac 没有,
# 而 Kotlin/JVM 开发需要完整 JDK 而非仅 JRE, javac 是否有响应正好是 JDK 是否装全的判别信号
java --version
javac --version

# 预期输出要点(两行都应出现 21 字样):
#   openjdk version "21.0.9" ...   ← 大版本必须是 21, 21.0.x 随补丁版浮动
#   javac 21.0.9                   ← 同上, 与 java 版本来自同一套 JDK