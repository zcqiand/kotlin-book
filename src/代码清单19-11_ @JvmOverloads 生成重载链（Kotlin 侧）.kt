// @JvmOverloads：按默认参数生成完整重载链，Java 侧才用得起 1/2/3 参构造
class InvoiceQuery @JvmOverloads constructor(
    val owner: String,
    val page: Int = 1,
    val pageSize: Int = 20,
) {
    fun describe(): String = "InvoiceQuery(owner=$owner, page=$page, pageSize=$pageSize)"
}