/** 典型 Java 遗留类：私有字段 + getter/setter + 构造器重载（迁移前） */
public class LegacyInvoice {
    private String owner;
    private int amountCents;
    private String memo;

    public LegacyInvoice(String owner, int amountCents) {
        // 构造器重载委托：等价于 Kotlin 主构造的默认参数
        this(owner, amountCents, "");
    }

    public LegacyInvoice(String owner, int amountCents, String memo) {
        this.owner = owner;
        this.amountCents = amountCents;
        this.memo = memo;
    }

    public String getOwner() {
        return owner;
    }

    public int getAmountCents() {
        return amountCents;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }
}