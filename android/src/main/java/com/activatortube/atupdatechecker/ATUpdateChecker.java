package com.activatortube.atupdatechecker;

import android.content.Context;

import java.io.File;

public class ATUpdateChecker {

    private static ATUpdateCheckerVersionHandler versionHandler;

    public static void setVersionHandler(ATUpdateCheckerVersionHandler versionHandler) {
        ATUpdateChecker.versionHandler = versionHandler;
    }

    public static ATUpdateCheckerVersionHandler getVersionHandler() {
        return ATUpdateChecker.versionHandler;
    }

    /**
     * 获取bundle包所在的路径
     * @param context 上下文对象
     * @return 如果是使用assets里面的bundle，则返回null，其他返回bundle所在路径
     */
    public static String bundlePath(Context context) {
        long version = ATUpdateCheckerModule.getVersionCode(context);
        long versionCode = ATUpdateCheckerModule.getAppVersionCode(context);

        if (versionCode >= version) { //直接显示本地的版本
            return null;
        } else {
            String dir = ATUpdateCheckerModule.getBundlePathByVersionCode((int) version, context);
            return new File(dir, "index.android.bundle").getAbsolutePath();
        }
    }

}
