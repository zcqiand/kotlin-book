/** @JvmField：Java 侧直接摸字段，没有 getXxx() 这一步 */
public static String readDeviceField() {
    DeviceInfo device = new DeviceInfo();
    return "device.serial 直接摸字段 = " + device.serial;
}