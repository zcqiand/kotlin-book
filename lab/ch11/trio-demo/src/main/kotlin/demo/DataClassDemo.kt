package demo

// data class：编译器按主构造参数自动生成 equals/hashCode/toString/copy 与解构，
// 「属性完全相同即相等」这个值语义不用手写一行样板
data class Task(val id: Int, val title: String, val done: Boolean)

fun runDataClassDemo() {
    val task = Task(1, "写周报", false)
    val same = Task(1, "写周报", false)
    // 两个实例属性完全相同：自动生成的 equals 按值比较，hashCode 也随之一致
    println("equals=${task == same}，hashCode 相同=${task.hashCode() == same.hashCode()}")
    // toString 自动拼出全部属性，打印即可读
    println("toString=$task")
    // copy 只换一个字段，生成新实例；原对象原地不动，可变共享状态无从产生
    val finished = task.copy(done = true)
    println("copy 后新实例=$finished，原对象未动=${!task.done}")
    // 解构按主构造参数顺序取回，data class 自动生成 component1/component2/component3
    val (id, title, done) = finished
    println("解构取回：id=$id，title=$title，done=$done")
}
