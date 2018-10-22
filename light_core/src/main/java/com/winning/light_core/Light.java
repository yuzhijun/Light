package com.winning.light_core;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Light {
    private static LightManager sLightManager;
    static boolean sDebug = false;

    public static void init(LightConfig lightConfig) {
        sLightManager = LightManager.instance(lightConfig);
    }

    /**
     * @param t 要存储的性能实体
     * @param type 类型
     * */
    public static <T> void w(T t, int type){
        sLightManager.write(t, type);
    }

    /**
     * @param dates   日期数组，格式：“2018-07-27”
     * @param runnable 发送操作
     * @brief 发送日志
     */
    public static void s(String dates[], int type, SendLogRunnable runnable) {
        sLightManager.send(dates, type, runnable);
    }

    /**
     * @brief 立即写入日志文件
     */
    public static void f(String dates[], int type) {
        sLightManager.flush(dates, type);
    }

    /**
     * @brief 返回所有日志文件信息
     */
    public static Map<String, Long> getAllFilesInfo() {
        File dir = sLightManager.getDir();
        if (!dir.exists()) {
            return null;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }
        Map<String, Long> allFilesInfo = new HashMap<>();
        for (File file : files) {
            try {
                String[] longStrArray = file.getName().split("\\.");
                if (longStrArray.length > 0){
                    allFilesInfo.put(CommUtil.getDateStr(Long.parseLong(longStrArray[0])), file.length());
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return allFilesInfo;
    }

    /**
     * @brief Logan Debug开关
     */
    public static void setDebug(boolean debug) {
        Light.sDebug = debug;
    }
}
