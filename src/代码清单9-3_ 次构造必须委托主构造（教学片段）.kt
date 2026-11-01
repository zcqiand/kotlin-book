// 次构造：constructor 开头，必须委托主构造
class Greeted(val name: String) {
    constructor() : this("陌生人")   // 无参次构造，给 name 一个保底值
}