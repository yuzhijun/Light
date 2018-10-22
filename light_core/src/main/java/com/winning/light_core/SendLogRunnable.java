package com.winning.light_core;

import android.text.TextUtils;

import java.io.File;

public abstract class SendLogRunnable implements Runnable {
    public static final int SENDING = 10001;
    public static final int FINISH = 10002;

    private SendAction mSendAction;
    private OnSendLogCallBackListener mCallBackListener;

    /**
     * 真正发送上传日志文件的方法，留给外部实现
     *
     * @param logFile 日志文件
     */
    public abstract void sendLog(File logFile);

    public void setSendAction(SendAction action) {
        mSendAction = action;
    }

    @Override
    public void run() {
        if (mSendAction == null || TextUtils.isEmpty(mSendAction.date)) {
            if (mCallBackListener != null) {
                mCallBackListener.onCallBack(FINISH);
            }
            return;
        }

        if (TextUtils.isEmpty(mSendAction.uploadPath)) {
            if (mCallBackListener != null) {
                mCallBackListener.onCallBack(FINISH);
            }
            return;
        }
        File file = new File(mSendAction.uploadPath);
        sendLog(file);
        if (mSendAction.date.equals(String.valueOf(CommUtil.getCurrentTime()))) {
            file.delete();
        }

        if (mCallBackListener != null) {
            mCallBackListener.onCallBack(FINISH);
        }
    }

    public void setCallBackListener(OnSendLogCallBackListener callBackListener) {
        mCallBackListener = callBackListener;
    }

    interface OnSendLogCallBackListener {
        void onCallBack(int statusCode);
    }
}
