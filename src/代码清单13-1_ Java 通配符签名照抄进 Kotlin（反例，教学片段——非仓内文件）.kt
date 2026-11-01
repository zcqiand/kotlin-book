package demo

// 从 Java 搬来的转发逻辑，通配符原样照抄
fun forward(src: MutableList<? extends Message>, dst: MutableList<? super TextMessage>) {
    for (item in src) dst.add(item)
}