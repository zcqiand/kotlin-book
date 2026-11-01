package demo

// 反例：与正发（Vehicle.kt）同构，只是去掉了所有 open——
// Kotlin 默认 final 哲学下，这份代码必须在编译期失败
class LockedVehicle(val name: String) {
    fun fuelType(): String = "汽油"
}

class LockedElectricVehicle(name: String) : LockedVehicle(name) {
    // 期望报错：fuelType 在父类是 final，不允许被覆写
    override fun fuelType(): String = "纯电"
}
