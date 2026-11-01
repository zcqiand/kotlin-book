package demo

// 普通函数的泛型在运行期已被擦除，is T 直接被编译器拦下
fun <T : Message> List<Message>.ofTypePlain(): List<T> =
    filter { it is T }.map { it as T }