package demo

// 示例集入口：--args 选演示，输出逐行对回执（沿 ch9 口径）
fun main(args: Array<String>) {
    when (args.getOrNull(0)) {
        "override" -> {
            // 同一个循环、同一句 describe()，答案随实际类型变——这就是多态
            val garage = listOf(Vehicle("油车"), ElectricVehicle("电车"))
            for (v in garage) println(v.describe())
        }
        "protect" -> {
            val account = SavingsAccount("小契")
            account.deposit(200)
            account.deposit(50)
            // 读走公开出口；写（audit/auditLog）只发生在子类内部
            for (line in account.auditHistory()) println(line)
        }
        "shape" -> {
            // 接口两条路径：默认实现白拿 vs 覆写定制
            val describables = listOf(Article("Kotlin open 哲学"), Video("open 哲学公开课"))
            for (d in describables) println(d.describe())
            // 抽象类：abstract 欠条由子类兑现，具体成员直接继承
            val image = ImageAsset(id = 7, width = 1920, height = 1080)
            println(image.label())
        }
        else -> println("用法：gradle run --args=<override|protect|shape>")
    }
}
