/** @JvmOverloads 生成的重载链：1 参 / 2 参 / 3 参各 new 一次，默认值由重载链兜住 */
public static String callInvoiceQueryOverloads() {
    String one = new InvoiceQuery("老陈").describe();
    String two = new InvoiceQuery("老陈", 2).describe();
    String three = new InvoiceQuery("老陈", 2, 50).describe();
    return one + " | " + two + " | " + three;
}