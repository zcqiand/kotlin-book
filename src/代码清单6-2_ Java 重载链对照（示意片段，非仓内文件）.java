// Java 对照示意片段，非仓内文件：为了模拟「后两个参数可选」，
// 被迫立起三个重载，方法数随可选参数个数增长，默认值靠重载链接力补齐
public class PriceFormatter {

    // 最简重载：只给金额，符号与精度走约定默认值，委托给下一层补齐
    public static String formatPrice(double amount) {
        return formatPrice(amount, "￥");
    }

    // 中间重载：金额加符号，精度仍取默认，继续向下委托
    public static String formatPrice(double amount, String symbol) {
        return formatPrice(amount, symbol, 2);
    }

    // 完整重载：三个参数一个不缺，真正的格式化逻辑全类只有这一份
    public static String formatPrice(double amount, String symbol, int decimals) {
        return symbol + String.format("%." + decimals + "f", amount);
    }
}