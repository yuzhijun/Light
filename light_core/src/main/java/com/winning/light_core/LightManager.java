package com.winning.light_core;

import android.os.Looper;
import android.text.TextUtils;

import com.winning.light_core.lightprotocol.TLVManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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

    <T> void write(T log, int type) {
        if (null == log) {
            return;
        }

        try {
            LightModel model = new LightModel();
            model.action = LightModel.Action.WRITE;
            WriteAction action = new WriteAction();
            String threadName = Thread.currentThread().getName();
            long threadLog = Thread.currentThread().getId();
            boolean isMain = false;
            if (Looper.getMainLooper() == Looper.myLooper()) {
                isMain = true;
            }
            action.log = TLVManager.convertModel2ByteArray(log, type);
            action.localTime = System.currentTimeMillis();
            action.type = type;
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    byte[] get(String date, int type){
        if (TextUtils.isEmpty(date)){
            return null;
        }
        File dir = getDir();
        if (!dir.exists()) {
            return null;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (File file : files) {
            try {
                String[] longStrArray = file.getName().split("\\.");
                if (longStrArray.length > 1 && longStrArray[0].equalsIgnoreCase(date) && longStrArray[1].equalsIgnoreCase(String.valueOf(type))){
                    InputStream in = new FileInputStream(file);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024 * 4];
                    int n = 0;
                    while ((n = in.read(buffer)) != -1) {
                        out.write(buffer, 0, n);
                    }
                   return out.toByteArray();
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            } catch (IOException e2){
                e2.printStackTrace();
            }
        }

        return null;
    }

    void send(String dates[], int type ,SendLogRunnable runnable) {
        if (TextUtils.isEmpty(mPath) || dates == null || dates.length == 0) {
            return;
        }
        for (String date : dates) {
            if (TextUtils.isEmpty(date)) {
                continue;
            }

            LightModel model = new LightModel();
            SendAction action = new SendAction();
            model.action = LightModel.Action.SEND;
            action.date = date;
            action.sendLogRunnable = runnable;
            action.type = type;
            model.sendAction = action;
            mCacheLogQueue.add(model);
            if (mLightThread != null) {
                mLightThread.notifyRun();
            }
        }
    }

    File getDir() {
        return new File(mPath);
    }

    void flush(String[] dates, int type) {
        if (TextUtils.isEmpty(mPath)|| dates == null || dates.length == 0) {
            return;
        }

        for (String date : dates) {
            if (TextUtils.isEmpty(date)) {
                continue;
            }

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
