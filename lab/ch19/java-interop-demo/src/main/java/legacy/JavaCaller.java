package legacy;

import demo.DeviceInfo;
import demo.Invoice;
import demo.InvoiceQuery;
import demo.Session;
import demo.TokenStore;
import demo.UserRepo;

/**
 * Java 调 Kotlin 的调用点实录：属性映射成 get/set 对、companion 成员两态、
 * @JvmOverloads 重载链、@JvmField 字段直访、迁移后 Invoice 复调，全在这里实测。
 */
public class JavaCaller {

    /** 调 Kotlin 类 UserRepo：var/val 属性在 Java 眼里就是 get/set 方法对 */
    public static String callKotlinRepo() {
        UserRepo repo = new UserRepo();
        repo.setRegion("cn-south");        // Kotlin 的 var region → setRegion()
        String region = repo.getRegion();  // → getRegion()
        int count = repo.getUserCount();   // val userCount → 只有 getter
        return repo.greet("Java调用方") + " | region=" + region + " | count=" + count;
    }

    /** companion 成员的两种 Java 调法：不加 @JvmStatic 走 Companion 门面，加了在类上直调 */
    public static String callCompanionMembers() {
        String issuer = TokenStore.Companion.defaultIssuer(); // 未加注解：成员长在 Companion 上
        Session session = Session.create();                   // @JvmStatic：类上直调
        return "TokenStore.Companion.defaultIssuer()=" + issuer
                + "，Session.create()→" + session.describe();
    }

    /** @JvmOverloads 生成的重载链：1 参 / 2 参 / 3 参各 new 一次，默认值由重载链兜住 */
    public static String callInvoiceQueryOverloads() {
        String one = new InvoiceQuery("老陈").describe();
        String two = new InvoiceQuery("老陈", 2).describe();
        String three = new InvoiceQuery("老陈", 2, 50).describe();
        return one + " | " + two + " | " + three;
    }

    /** @JvmField：Java 侧直接摸字段，没有 getXxx() 这一步 */
    public static String readDeviceField() {
        DeviceInfo device = new DeviceInfo();
        return "device.serial 直接摸字段 = " + device.serial;
    }

    /**
     * 迁移后的 Invoice 复调：调用形态与当年调 LegacyInvoice 完全同形——
     * 两档构造实参、getter、setMemo，一处没改，证明迁移对 Java 调用方透明。
     */
    public static String callInvoice() {
        Invoice twoArgs = new Invoice("老陈", 12900);
        Invoice threeArgs = new Invoice("老陈", 12900, "十一月书费");
        threeArgs.setMemo("十一月书费（已支付）");
        return twoArgs.getOwner() + "/" + twoArgs.getAmountCents() + "/" + twoArgs.getMemo()
                + " | " + threeArgs.getOwner() + "/" + threeArgs.getAmountCents() + "/" + threeArgs.getMemo();
    }
}
