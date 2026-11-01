package demo

// 抽象类对照：abstract 成员是「子类必须兑现的欠条」，
// 具体成员则像普通继承一样直接白拿——与接口的「可选实现」互补
abstract class MediaAsset(val id: Int) {
    // 欠条：没有默认体，每个具体子类都必须给实现，否则自己编译不过
    abstract fun sizeKb(): Int

    // 具体成员：子类直接继承，内部同样依赖 sizeKb() 的多态分派
    fun label(): String = "资产#$id，体积=${sizeKb()}KB"
}

class ImageAsset(id: Int, val width: Int, val height: Int) : MediaAsset(id) {
    // 兑现欠条：按未压缩 RGB 粗略估算体积
    override fun sizeKb(): Int = width * height * 3 / 1024
}
