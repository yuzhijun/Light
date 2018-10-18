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
     * @param log  表示日志内容
     * @param type 表示日志类型 "crash" 或者 "device"
     * @brief Logan写入日志
     */
    public static void w(String log, String type) {
        sLightManager.write(log, type);
    }

    /**
     * @param dates   日期数组，格式：“2018-07-27”
     * @param runnable 发送操作
     * @brief 发送日志
     */
    public static void s(String dates[], String type, SendLogRunnable runnable) {
        sLightManager.send(dates, type, runnable);
    }

    /**
     * @brief 立即写入日志文件
     */
    public static void f(String dates[], String type) {
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
