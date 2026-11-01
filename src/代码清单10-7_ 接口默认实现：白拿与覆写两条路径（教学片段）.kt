package demo

// 接口默认实现：方法体直接写在接口里，
// 「大多数实现都一样、少数才需要定制」的能力面就用它
interface Describable {
    val title: String

    // 默认实现：实现类不写一个字，也免费获得这份行为
    fun describe(): String = "【$title】（默认介绍模板）"
}

// 路径一：什么都不覆写，直接消费默认实现
class Article(override val title: String) : Describable

// 路径二：覆写默认实现，换成自己的介绍
class Video(override val title: String) : Describable {
    override fun describe(): String = "$title（视频专属介绍）"
}