package com.winning.light_core;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WriteAction {
    byte[] log; //日志
    boolean isMainThread;
    long threadId;
    String threadName = "";
    long localTime;
    int type;
    AtomicBoolean isIdle = new AtomicBoolean(true);

    private static final int length = 64;
    private static AtomicInteger sIndex = new AtomicInteger(0);
    private static final int RESET_NUM = 1000;
    private static volatile WriteAction[] table = new WriteAction[length];

    static {
        for (int i = 0; i < length; i++) {
            table[i] = new WriteAction();
        }
    }

    boolean isValid() {
        boolean valid = false;
        if (null != log && log.length > 0) {
            valid = true;
        }
        return valid;
    }

    public static WriteAction obtain() {
        return obtain(0);
    }

    private static WriteAction obtain(int retryTime) {
        int index = sIndex.getAndIncrement();
        if (index > RESET_NUM) {
            sIndex.compareAndSet(index, 0);
            if (index > RESET_NUM * 2) {
                sIndex.set(0);
            }
        }

        int num = index & (length - 1);

        WriteAction target = table[num];

        if (target.isIdle.compareAndSet(true, false)) {
            target.log = null;
            target.isMainThread = false;
            target.threadId = 0;
            target.threadName = "";
            target.localTime = CommUtil.getCurrentTime();
            target.type = -1;
            return target;
        } else {
            if (retryTime < 5) {
                return obtain(retryTime++);
            } else {
                return new WriteAction();
            }
        }
    }
}
