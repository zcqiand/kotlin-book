package legacy;

/**
 * 模拟「不动存量」的 Java 旧代码：普通 getter/setter、可返回 null 的查找方法、静态工具方法各一。
 * Kotlin 侧直调本类时，getter/setter 会呈现为属性语法，findEmail/findNickname 呈现为平台类型。
 */
public class LegacyUserStore {
    private String displayName;
    private int level;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String value) {
        // Java 侧惯例：写入前可以任意加工，Kotlin 调用方对此无感
        this.displayName = (value == null) ? "" : value.trim();
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * 可空性没有任何标注——Kotlin 侧看到的返回类型是平台类型 String!，
     * 这是本章 p09 混淆点的源头：编译器不拦 null，运行期才爆。
     */
    public String findEmail(String userId) {
        if ("u1001".equals(userId)) {
            return "chen@example.com";
        }
        return null;
    }

    /** 同为平台类型返回，供 p09 三种处置对照演练使用 */
    public String findNickname(String userId) {
        if ("u1001".equals(userId)) {
            return "老陈";
        }
        return null;
    }

    /** 静态工具方法：Kotlin 侧直接类名调用，无需实例 */
    public static String versionTag() {
        return "legacy-store-1.0";
    }
}
