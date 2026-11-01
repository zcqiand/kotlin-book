/** companion 成员的两种 Java 调法：不加 @JvmStatic 走 Companion 门面，加了在类上直调 */
public static String callCompanionMembers() {
    String issuer = TokenStore.Companion.defaultIssuer(); // 未加注解：成员长在 Companion 上
    Session session = Session.create();                   // @JvmStatic：类上直调
    return "TokenStore.Companion.defaultIssuer()=" + issuer
            + "，Session.create()→" + session.describe();
}