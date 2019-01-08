package com.example.ydd.dp;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.gprinter.aidl.GpService;
import com.gprinter.command.GpCom;
import com.gprinter.service.GpPrintService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.gprinter.service.GpPrintService.PRINTER_ID;

public class MyApplication extends Application {

    private static final int MAIN_QUERY_PRINTER_STATUS = 0xfe;

    public GpService getmGpService() {
        return mGpService;
    }

    private GpService mGpService = null;
    private PrinterServiceConnection conn = null;
    static ExecutorService executor;


    @Override
    public void onCreate() {
        super.onCreate();

        AppException appException = AppException.getInstance();
        appException.init(getApplicationContext());
        connection();


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

    public static ExecutorService getExecutor() {
        return executor == null ? (executor = Executors.newCachedThreadPool()) : executor;
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
