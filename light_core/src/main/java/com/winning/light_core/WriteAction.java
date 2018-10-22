package com.winning.light_core;

public class WriteAction {
    byte[] log; //日志
    boolean isMainThread;
    long threadId;
    String threadName = "";
    long localTime;
    int type;

    boolean isValid() {
        boolean valid = false;
        if (null != log && log.length > 0) {
            valid = true;
        }
        return valid;
    }
}
