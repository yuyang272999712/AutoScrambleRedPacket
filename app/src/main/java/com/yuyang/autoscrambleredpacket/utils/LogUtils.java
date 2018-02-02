package com.yuyang.autoscrambleredpacket.utils;

import android.util.Log;

/**
 * Created by yuyang on 2017/12/20.
 */

public class LogUtils {
    //TODO yuyang 打包前请关闭debug模式
    public static boolean isDebug = false;

    public static void e(String tag, String message){
        if (isDebug){
            Log.e(tag, message);
        }
    }
}
