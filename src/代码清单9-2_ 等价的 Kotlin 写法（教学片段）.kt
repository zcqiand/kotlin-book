// Kotlin：参数表声明即构造，val 直接升格为属性
class Progress(val percent: Int) {
    init {
        require(percent in 0..100) { "percent 必须在 0..100：$percent" }
    }

    val isDone: Boolean
        get() = percent == 100
}