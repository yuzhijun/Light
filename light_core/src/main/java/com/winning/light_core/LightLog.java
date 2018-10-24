package com.winning.light_core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.BufferOverflowException;
import java.nio.MappedByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.FileChannel;
import java.util.concurrent.ConcurrentHashMap;

public class LightLog {
    private static final long KB = 1024; //1 for test
    private static final String FILE_NAME = "light_cache";
    private static final long DEFAULT_CACHE_SIZE = 10 * KB;
    private static LightLog sLightLog;
    private String mCachePath;
    private String mPath;
    private ConcurrentHashMap<Integer, MappedByteBuffer> mmapHashMap = new ConcurrentHashMap<>();
    private long mMaxLogFile;

    public static LightLog newInstance() {
        if (sLightLog == null) {
            synchronized (LightLog.class) {
                sLightLog = new LightLog();
            }
        }
        return sLightLog;
    }

    public void init(String cachePath, String path, long maxLogFile){
        mCachePath = cachePath;
        mPath = path;
        mMaxLogFile = maxLogFile;
    }

    public void flush(String date, int type){
        if (null == date || -1 == type){
            return;
        }
        String cachePath = mCachePath + File.separator +  FILE_NAME + File.separator + type + ".cache";
        String logPath = mPath + File.separator + date + "." + type + ".log";
        File cacheFile = new File(cachePath);
        if (!cacheFile.exists()){
            return;
        }

        RandomAccessFile rafi = null;
        RandomAccessFile rafo = null;
        FileChannel fci = null;
        FileChannel fco = null;

       try{
           File logFile = new File(logPath);
           if (!logFile.exists()){
               logFile.getParentFile().mkdirs();
               logFile.createNewFile();
           }

            rafi = new RandomAccessFile(cacheFile, "rw");
            rafo = new RandomAccessFile(logFile, "rw");

            fci = rafi.getChannel();
            fco = rafo.getChannel();

           long cacheSize = fci.size();
           long logSize = fco.size();

           MappedByteBuffer mbbi = fci.map(FileChannel.MapMode.READ_WRITE, 0, cacheSize);
           MappedByteBuffer mbbo = fco.map(FileChannel.MapMode.READ_WRITE, logSize, cacheSize);
           for (int i = 0; i < cacheSize; i++) {
               mbbo.put(mbbi.get(i));
           }
           //解除内存映射
           unmap(mbbi);
           unmap(mbbo);
           //清空缓存文件
           FileWriter fileWriter = new FileWriter(cacheFile);
           fileWriter.write("");
           fileWriter.flush();
           fileWriter.close();
       }catch (IOException e){
            e.printStackTrace();
       }finally {
           try {
               if (null != fci) {
                   fci.close();
                   fci = null;
               }

               if (null != fco) {
                   fco.close();
                   fco = null;
               }

               if (null != rafi) {
                   rafi.close();
                   rafi = null;
               }

               if (null != rafo) {
                   rafo.close();
                   rafo = null;
               }
           } catch (Exception e2) {
               e2.printStackTrace();
           }
       }

    }

    public void write(byte[] log, int type){
        if (null == log || -1 == type){
            return;
        }

        try {
            MappedByteBuffer mbbi = getMappedByteBufferByType(type);
            if (mbbi != null) {
                mbbi.put(log);
            }
        } catch (BufferOverflowException e){
            removeMappedByteBufferByType(type);
            //缓存区满了则flush到日志文件
            String currentDate = CommUtil.getDateStr(System.currentTimeMillis());
            flush(currentDate, type);

            MappedByteBuffer mbbi = getMappedByteBufferByType(type);
            if (mbbi != null) {
                mbbi.put(log);
            }
            e.printStackTrace();
        } catch (ReadOnlyBufferException e){
            e.printStackTrace();
        }
    }

    private String getCachePath(int type){
        return mCachePath + File.separator +  FILE_NAME + File.separator + type + ".cache";
    }

    private MappedByteBuffer getMappedByteBufferByType(int type){
        if (type < -1) {
            return null;
        }

        MappedByteBuffer mmapByteBuffer = mmapHashMap.get(type);
        if (null != mmapByteBuffer){
            return mmapByteBuffer;
        }

        RandomAccessFile rafi;
        FileChannel fci;

        try {
            File cacheFile = new File(getCachePath(type));
            if (!cacheFile.exists()){
                cacheFile.getParentFile().mkdirs();
                cacheFile.createNewFile();
            }

            rafi = new RandomAccessFile(cacheFile, "rw");
            fci = rafi.getChannel();

            MappedByteBuffer mbbi = fci.map(FileChannel.MapMode.READ_WRITE, 0, DEFAULT_CACHE_SIZE);
            if (null != mbbi){
                mmapHashMap.put(type, mbbi);
            }
            return mbbi;
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

    private void removeMappedByteBufferByType(int type){
        MappedByteBuffer mappedByteBuffer;
        if (null != mmapHashMap){
            mappedByteBuffer = mmapHashMap.get(type);
            if (null != mappedByteBuffer){
                unmap(mappedByteBuffer);
                mmapHashMap.remove(type);
            }
        }
    }

    /**
     * 解除内存与文件的映射
     * */
    private void unmap(MappedByteBuffer mbbi) {
        if (mbbi == null) {
            return;
        }
        try {
            Class<?> clazz = Class.forName("sun.nio.ch.FileChannelImpl");
            Method m = clazz.getDeclaredMethod("unmap", MappedByteBuffer.class);
            m.setAccessible(true);
            m.invoke(null, mbbi);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
