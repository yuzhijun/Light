package com.winning.light_core;

import android.os.StatFs;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LightThread extends Thread {
    private static final int MINUTE = 60 * 1000;
    private static final long LONG = 24 * 60 * 60 * 1000;
    private static final int CACHE_SIZE = 1024;
    private volatile boolean mIsRun = true;
    private boolean mIsWorking;
    private boolean mIsSDCard;
    private long mLastTime;
    private final Object sync = new Object();
    private final Object sendSync = new Object();

    private ConcurrentLinkedQueue<LightModel> mCacheLogQueue;
    private String mCachePath; // 缓存文件路径
    private String mPath; //文件路径
    private long mSaveTime; //存储时间
    private long mMaxLogFile;//最大文件大小
    private long mMinSDCard;
    private long mCurrentDay;
    private int mSendLogStatusCode;

    // 发送缓存队列
    private ConcurrentLinkedQueue<LightModel> mCacheSendQueue = new ConcurrentLinkedQueue<>();
    private ExecutorService mSingleThreadExecutor = Executors.newSingleThreadExecutor();

    public LightThread(
            ConcurrentLinkedQueue<LightModel> cacheLogQueue, String cachePath,
            String path, long saveTime, long maxLogFile, long minSDCard) {
        mCacheLogQueue = cacheLogQueue;
        mCachePath = cachePath;
        mPath = path;
        mSaveTime = saveTime;
        mMaxLogFile = maxLogFile;
        mMinSDCard = minSDCard;

        LightLog.newInstance().init(mCachePath, mPath, mMaxLogFile);
    }

    @Override
    public void run() {
        super.run();
        while (mIsRun) {
            synchronized (sync) {
                mIsWorking = true;
                try{
                    LightModel model = mCacheLogQueue.poll();
                    if (model == null) {
                        mIsWorking = false;
                        sync.wait();
                        mIsWorking = true;
                    } else {
                        doNetworkLog(model);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    mIsWorking = false;
                }
            }
        }
    }

    public void notifyRun() {
        if (!mIsWorking) {
            synchronized (sync) {
                sync.notify();
            }
        }
    }

    public void quit() {
        mIsRun = false;
        if (!mIsWorking) {
            synchronized (sync) {
                sync.notify();
            }
        }
    }

    private void doNetworkLog(LightModel model) {
        if (model == null || !model.isValid()) {
            return;
        }

        if (model.action == LightModel.Action.WRITE) {
            doWriteLog2File(model.writeAction);
        } else if (model.action == LightModel.Action.SEND) {
            if (model.sendAction.sendLogRunnable != null) {
                // 是否正在发送
                synchronized (sendSync) {
                    if (mSendLogStatusCode == SendLogRunnable.SENDING) {
                        mCacheSendQueue.add(model);
                    } else {
                        doSendLog2Net(model.sendAction);
                    }
                }
            }
        } else if (model.action == LightModel.Action.FLUSH) {
            doFlushLog2File(model.flushAction.date, model.flushAction.type);
        }
    }

    private void doWriteLog2File(WriteAction action) {
        if (!isDay()) {
            long tempCurrentDay = CommUtil.getCurrentTime();
            //save时间
            long deleteTime = tempCurrentDay - mSaveTime;
            deleteExpiredFile(deleteTime);
            mCurrentDay = tempCurrentDay;
        }

        long currentTime = System.currentTimeMillis(); //每隔1分钟判断一次
        if (currentTime - mLastTime > MINUTE) {
            mIsSDCard = isCanWriteSDCard();
        }
        mLastTime = System.currentTimeMillis();

        if (!mIsSDCard) { //如果大于50M 不让再次写入
            return;
        }

        LightLog.newInstance().write(action.log, action.type);
        //设置WriteAction为空闲
        action.isIdle.set(true);
    }

    private void doFlushLog2File(String date, int type) {
        if (null == date || -1 == type){
            return;
        }
        LightLog.newInstance().flush(date, type);
    }

    private void doSendLog2Net(SendAction action) {
        if (TextUtils.isEmpty(mPath) || action == null || !action.isValid()) {
            return;
        }
        boolean success = prepareLogFile(action);
        if (!success) {
            return;
        }
        action.sendLogRunnable.setSendAction(action);
        action.sendLogRunnable.setCallBackListener(
                new SendLogRunnable.OnSendLogCallBackListener() {
                    @Override
                    public void onCallBack(int statusCode) {
                        synchronized (sendSync) {
                            mSendLogStatusCode = statusCode;
                            if (statusCode == SendLogRunnable.FINISH) {
                                mCacheLogQueue.addAll(mCacheSendQueue);
                                mCacheSendQueue.clear();
                                notifyRun();
                            }
                        }
                    }
                });
        mSendLogStatusCode = SendLogRunnable.SENDING;
        mSingleThreadExecutor.execute(action.sendLogRunnable);
    }

    /**
     * 发送日志前的预处理操作
     */
    private boolean prepareLogFile(SendAction action) {
        if (isFile(action)) { //是否有日期文件
            String src = mPath + File.separator + action.date + "." + action.type + ".log";
            if (action.date.equals(CommUtil.getCurrentDate())) {
                doFlushLog2File(action.date, action.type);
                String des = mPath + File.separator + action.date + "." + action.type + ".copy";
                if (copyFile(src, des)) {
                    action.uploadPath = des;
                    return true;
                }
            } else {
                action.uploadPath = src;
                return true;
            }
        } else {
            action.uploadPath = "";
        }
        return false;
    }

    private boolean isCanWriteSDCard() {
        boolean item = false;
        try {
            StatFs stat = new StatFs(mPath);
            long blockSize = stat.getBlockSize();
            long availableBlocks = stat.getAvailableBlocks();
            long total = availableBlocks * blockSize;
            if (total > mMinSDCard) { //判断SDK卡
                item = true;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return item;
    }

    private void deleteExpiredFile(long deleteTime) {
        File dir = new File(mPath);
        if (dir.isDirectory()) {
            String[] files = dir.list();
            if (files != null) {
                for (String item : files) {
                    try {
                        if (TextUtils.isEmpty(item)) {
                            continue;
                        }
                        String[] longStrArray = item.split("\\.");
                        if (longStrArray.length > 0) {  //小于时间就删除
                            long longItem = CommUtil.getDateTime(longStrArray[0]);
                            if (longItem <= deleteTime) {
                                new File(mPath, item).delete(); //删除文件
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private boolean copyFile(String src, String des) {
        boolean back = false;
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(new File(src));
            outputStream = new FileOutputStream(new File(des));
            byte[] buffer = new byte[CACHE_SIZE];
            int i;
            while ((i = inputStream.read(buffer)) >= 0) {
                outputStream.write(buffer, 0, i);
                outputStream.flush();
            }
            back = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return back;
    }

    private boolean isDay() {
        long currentTime = System.currentTimeMillis();
        return mCurrentDay < currentTime && mCurrentDay + LONG > currentTime;
    }

    private boolean isFile(SendAction action) {
        boolean isExist = false;
        if (TextUtils.isEmpty(mPath)) {
            return false;
        }
        File file = new File(mPath + File.separator + action.date + "." + action.type + ".log");
        if (file.exists() && file.isFile()) {
            isExist = true;
        }
        return isExist;
    }
}
