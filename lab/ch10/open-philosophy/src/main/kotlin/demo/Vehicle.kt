package demo

// open 是 Kotlin 的显式设计契约：类与成员默认 final，
// 继承是一种强耦合，作者必须主动表态（写 open）才允许被覆写
open class Vehicle(val name: String) {
    // open 成员：这里是留给子类的「多态点」，
    // describe() 不用改一行，就能随子类换答案
    open fun fuelType(): String = "汽油"

    // 普通成员内部调用 open 成员：多态在运行期按实际类型分派
    fun describe(): String = "$name：能源=${fuelType()}"
}

class ElectricVehicle(name: String) : Vehicle(name) {
    // 覆写点：把父类的默认答案换成子类的答案
    override fun fuelType(): String = "纯电"
}
