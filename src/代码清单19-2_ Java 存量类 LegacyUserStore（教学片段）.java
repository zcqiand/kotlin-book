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

    // …… getLevel()/setLevel() 同构，略

    /**
     * 可空性没有任何标注——Kotlin 侧看到的返回类型是平台类型 String!，
     * 这是本章平台类型风险的源头：编译器不拦 null，运行期才爆。
     */
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