package com.activatortube.atupdatechecker;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ATUpdateCheckerModule extends ReactContextBaseJavaModule {

    public ATUpdateCheckerModule(@NonNull ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @NonNull
    @Override
    public String getName() {
        return "ATUpdateChecker";
    }

    @ReactMethod
    public void getVersionCode(Promise promise) {
        long version = this.getVersionCode(getCurrentActivity());
        promise.resolve((int) version);
    }

    @ReactMethod
    public void downloadBundle(String url, int version_code, Promise promise) {
        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                promise.reject("1", "download error");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = response.body().byteStream();
                //获取cache的目录，将下载zip暂时存在cache下
                String cacheDir = new File(getCurrentActivity().getCacheDir(), version_code + ".zip").getAbsolutePath();
                FileOutputStream fos = new FileOutputStream(new File(cacheDir));
                int len = 0;
                byte[] buffer = new byte[2048];
                while (-1 != (len = is.read(buffer))) {
                    fos.write(buffer, 0, len);
                }
                fos.flush();
                fos.close();
                is.close();

                String dest = getBundlePathByVersionCode(version_code, getCurrentActivity());
                unzip(cacheDir, dest);

                promise.resolve(dest);
            }
        });
    }

    /**
     * 获取apk的版本号
     * @param context 上下文
     * @return apk的版本
     */
    public static long getAppVersionCode(Context context) {

        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            //这样获取的versionCode是原始的code，对于分包发布到google play的情况，因为对版本号进行了处理，需要对版本号进行进一步处理才能获取到正确的版本号
            long versionCode;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                versionCode = packageInfo.getLongVersionCode();
            } else {
                versionCode = packageInfo.versionCode;
            }

            if (ATUpdateChecker.getVersionHandler() != null) {
                versionCode = ATUpdateChecker.getVersionHandler().progress(versionCode);
            }
            return versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return 0;
        }

    }

    /**
     * 获取版本号
     * @return 版本号
     */
    public static long getVersionCode(Context context) {

        long versionCode = getAppVersionCode(context);

        File versionsFile = context.getDir("ATVersions", Context.MODE_PRIVATE);
        File[] files = versionsFile.listFiles();

        for (File file : files) {
            if (file.isDirectory()) {
                String name = file.getName();
                Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
                boolean isInt = pattern.matcher(name).matches();
                if (isInt) {
                    long bundleVersion = Long.parseLong(name);
                    if (bundleVersion > versionCode) {
                        versionCode = bundleVersion;
                    }
                }
            }
        }

        return versionCode;

    }

    /**
     * 通过versionCode获取保存当前版本的路径
     * @param versionCode 版本号
     * @return 保存的路径
     */
    public static String getBundlePathByVersionCode(int versionCode, Context context) {
        File versionsFile = context.getDir("ATVersions", Context.MODE_PRIVATE);
        File file = new File(versionsFile, versionCode + "");
        return file.getAbsolutePath();
    }

    private static void unzip(String source, String dest) {
        try {
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(new File(source))));
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(dest, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory: " +
                            dir.getAbsolutePath());
                if (ze.isDirectory())
                    continue;
                FileOutputStream fout = new FileOutputStream(file);
                try {
                    while ((count = zis.read(buffer)) != -1)
                        fout.write(buffer, 0, count);
                } finally {
                    fout.close();
                }
            /* if time should be restored as well
            long time = ze.getTime();
            if (time > 0)
                file.setLastModified(time);
            */
            }
            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
