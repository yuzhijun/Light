package com.winning.light_core;

import android.os.Looper;
import android.text.TextUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LightManager {
    private static LightManager sLoganManager;
    private ConcurrentLinkedQueue<LightModel> mCacheLogQueue = new ConcurrentLinkedQueue<>();
    private String mCachePath; // 缓存文件路径
    private String mPath; //文件路径
    private long mMaxQueue; //最大队列数
    private long mMinSDCard;
    private long mMaxLogFile;//最大文件大小
    private long mSaveTime; //存储时间
    private LightThread mLightThread;
    private SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd");

    static LightManager instance(LightConfig config) {
        if (sLoganManager == null) {
            synchronized (LightManager.class) {
                if (sLoganManager == null) {
                    sLoganManager = new LightManager(config);
                }
            }
        }
        return sLoganManager;
    }

    private LightManager(LightConfig config) {
        if (!config.isValid()) {
            throw new NullPointerException("config's param is invalid");
        }

        mPath = config.mPathPath;
        mCachePath = config.mCachePath;
        mMaxQueue = config.mMaxQueue;
        mMinSDCard = config.mMinSDCard;
        mMaxLogFile = config.mMaxFile;
        mSaveTime = config.mDay;

        init();
    }

    private void init(){
        if (null == mLightThread){
            mLightThread = new LightThread(mCacheLogQueue, mCachePath, mPath, mSaveTime, mMaxLogFile, mMinSDCard);
            mLightThread.start();
        }
    }

    void write(String log, String flag) {
        if (TextUtils.isEmpty(log)) {
            return;
        }
        LightModel model = new LightModel();
        model.action = LightModel.Action.WRITE;
        WriteAction action = new WriteAction();
        String threadName = Thread.currentThread().getName();
        long threadLog = Thread.currentThread().getId();
        boolean isMain = false;
        if (Looper.getMainLooper() == Looper.myLooper()) {
            isMain = true;
        }
        action.log = log;
        action.localTime = System.currentTimeMillis();
        action.flag = flag;
        action.isMainThread = isMain;
        action.threadId = threadLog;
        action.threadName = threadName;
        model.writeAction = action;
        if (mCacheLogQueue.size() < mMaxQueue) {
            mCacheLogQueue.add(model);
            if (mLightThread != null) {
                mLightThread.notifyRun();
            }
        }
    }

    void send(String dates[],String type ,SendLogRunnable runnable) {
        if (TextUtils.isEmpty(mPath) || dates == null || dates.length == 0) {
            return;
        }
        for (String date : dates) {
            if (TextUtils.isEmpty(date)) {
                continue;
            }
            long time = CommUtil.getDateTime(date);
            if (time > 0) {
                LightModel model = new LightModel();
                SendAction action = new SendAction();
                model.action = LightModel.Action.SEND;
                action.date = String.valueOf(time);
                action.sendLogRunnable = runnable;
                action.type = type;
                model.sendAction = action;
                mCacheLogQueue.add(model);
                if (mLightThread != null) {
                    mLightThread.notifyRun();
                }
            }
        }
    }

    File getDir() {
        return new File(mPath);
    }

    void flush(String[] dates, String type) {
        if (TextUtils.isEmpty(mPath)|| dates == null || dates.length == 0) {
            return;
        }

        for (String date : dates) {
            if (TextUtils.isEmpty(date)) {
                continue;
            }

            long time = CommUtil.getDateTime(date);
            if (time > 0) {
                LightModel model = new LightModel();
                FlushAction action = new FlushAction();
                model.action = LightModel.Action.FLUSH;
                action.date = date;
                action.type = type;
                model.flushAction = action;
                mCacheLogQueue.add(model);
                if (mLightThread != null) {
                    mLightThread.notifyRun();
                }
            }
        }
    }
}
