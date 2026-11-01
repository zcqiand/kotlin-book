/** 调 Kotlin 类 UserRepo：var/val 属性在 Java 眼里就是 get/set 方法对 */
public static String callKotlinRepo() {
    UserRepo repo = new UserRepo();
    repo.setRegion("cn-south");        // Kotlin 的 var region → setRegion()
    String region = repo.getRegion();  // → getRegion()
    int count = repo.getUserCount();   // val userCount → 只有 getter
    return repo.greet("Java调用方") + " | region=" + region + " | count=" + count;
}