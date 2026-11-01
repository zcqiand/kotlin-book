package hello

// 单表达式函数：函数体只有一行返回值时，等号直接接表达式，
// 省掉 return 和花括号——这是 Kotlin 推崇的简洁风格
fun greet(name: String): String = "你好，$name"

fun main() {
    println(greet("Kotlin"))
}