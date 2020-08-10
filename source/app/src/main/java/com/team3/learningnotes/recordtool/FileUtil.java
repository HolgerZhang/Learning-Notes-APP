package com.team3.learningnotes.recordtool;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// 管理录音文件的类
public class FileUtil {

    private static String rootPath = null;
    //原始文件(不能播放)
    private final static String AUDIO_PCM_BASEPATH = "pcm/";
    //可播放的高质量音频文件
    private final static String AUDIO_WAV_BASEPATH = "wav/";

    private static void setRootPath(final Context context, final String noteFileName) {
        FileUtil.rootPath = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC).toString()
                + File.separator + noteFileName + File.separator;
    }

    public static String getPcmFileAbsolutePath(String fileName, final Context context, final String noteFileName) {
        if (TextUtils.isEmpty(fileName)) {
            throw new NullPointerException("fileName isEmpty");
        }
        if (!isSdcardExit()) {
            throw new IllegalStateException("sd card no found");
        }
        String mAudioRawPath = "";
        if (isSdcardExit()) {
            if (!fileName.endsWith(".pcm")) {
                fileName += ".pcm";
            }
            setRootPath(context, noteFileName);
            String fileBasePath = rootPath + AUDIO_PCM_BASEPATH;
            File file = new File(fileBasePath);
            // 创建目录
            if (!file.exists()) {
                file.mkdirs();
            }
            mAudioRawPath = fileBasePath + fileName;
        }

        return mAudioRawPath;
    }

    public static String getWavFileAbsolutePath(String fileName, final Context context, final String noteFileName) {
        if (fileName == null) {
            throw new NullPointerException("fileName can't be null");
        }
        if (!isSdcardExit()) {
            throw new IllegalStateException("sd card no found");
        }

        String mAudioWavPath = "";
        if (isSdcardExit()) {
            if (!fileName.endsWith(".wav")) {
                fileName += ".wav";
            }
            setRootPath(context, noteFileName);
            String fileBasePath = rootPath + AUDIO_WAV_BASEPATH;
            File file = new File(fileBasePath);
            //创建目录
            if (!file.exists()) {
                file.mkdirs();
            }
            mAudioWavPath = fileBasePath + fileName;
        }
        return mAudioWavPath;
    }

    // 判断是否有外部存储设备sdcard
    public static boolean isSdcardExit() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            return true;
        else
            return false;
    }

    // 获取全部pcm文件列表
    public static List<File> getPcmFiles(final Context context, final String noteFileName) {
        List<File> list = new ArrayList<>();
        setRootPath(context, noteFileName);
        String fileBasePath = rootPath + AUDIO_PCM_BASEPATH;


        File rootFile = new File(fileBasePath);
        if (rootFile.exists()) {
            File[] files = rootFile.listFiles();
            for (File file : files) {
                list.add(file);
            }
        }
        return list;

    }

    // 获取全部wav文件列表
    public static List<File> getWavFiles(final Context context, final String noteFileName) {
        List<File> list = new ArrayList<>();
        setRootPath(context, noteFileName);
        String fileBasePath = rootPath + AUDIO_WAV_BASEPATH;

        File rootFile = new File(fileBasePath);
        if (!rootFile.exists()) {
            File[] files = rootFile.listFiles();
            for (File file : files) {
                list.add(file);
            }
        }
        return list;
    }

    public static String[] getWavFileNameList(final Context context, final String noteFileName) {
        setRootPath(context, noteFileName);
        String fileBasePath = rootPath + AUDIO_WAV_BASEPATH;

        File createDir = new File(fileBasePath);
        if (!createDir.exists()) {
            createDir.mkdirs();
            return new String[0];
        }
        return createDir.list();
    }

    public static String getWavFileDir(final Context context, final String noteFileName) {
        setRootPath(context, noteFileName);
        return rootPath + AUDIO_WAV_BASEPATH;
    }
}

