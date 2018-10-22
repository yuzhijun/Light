package com.winning.light_core;

public class SendAction {
    long fileSize; //文件大小
    String date; //日期
    String uploadPath;
    int type;
    SendLogRunnable sendLogRunnable;

    boolean isValid() {
        boolean valid = false;
        if (sendLogRunnable != null) {
            valid = true;
        } else if (fileSize > 0) {
            valid = true;
        }
        return valid;
    }
}
