package com.example.ydd.dp;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.gprinter.aidl.GpService;
import com.gprinter.service.GpPrintService;

public class MyApplication extends Application {

    public static boolean isIsRun() {
        return isRun;
    }

    private static boolean isRun = true;

    private static Monitor[] monitorList = new Monitor[20];
    public GpService getmGpService() {
        return mGpService;
    }

    private GpService mGpService = null;
    private PrinterServiceConnection conn = null;
    public static Database database;

    @Override
    public void onCreate() {
        super.onCreate();

        AppException appException = AppException.getInstance();
        appException.init(getApplicationContext());

        //初始化数据库
        DatabaseConfiguration config = new DatabaseConfiguration(getApplicationContext());
        try {
            database = new Database("Local", config);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        connection();
        MonitorSelector.startService();
    }

    @Override
    public void onLowMemory() {
        // 低内存的时候执行
        Log.d("DOAING", "onLowMemory～～～～～～～");
        super.onLowMemory();
    }
    @Override
    public void onTrimMemory(int level) {
        // 程序在内存清理的时候执行
        Log.d("DOAING", "onTrimMemory～～～～～～～～～～");
        super.onTrimMemory(level);
    }

    private void connection() {
        conn = new PrinterServiceConnection();
        Intent intent = new Intent(this, GpPrintService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE); // bindService
    }

    class PrinterServiceConnection implements ServiceConnection {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("ServiceConnection", "onServiceDisconnected() called");
            mGpService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mGpService = GpService.Stub.asInterface(service);
        }
    }
}
