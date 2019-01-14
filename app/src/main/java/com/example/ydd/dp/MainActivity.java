package com.example.ydd.dp;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.MainThread;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.gprinter.aidl.GpService;

import java.lang.ref.WeakReference;


public class MainActivity extends AppCompatActivity {

    Monitor m1, m2;

    TextView t1;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions(this);

        t1 = findViewById(R.id.msg);


        m1 = new Monitor.Builder(getApplicationContext())
                .setIndex(1)
                .setIp("192.168.2.248")
                .setLifeListener(new Monitor.LifeListener() {
                    @Override
                    public void in(int index, Object msg) {

                        Log.e("DOAING", "添加 " + msg + " 到 " + index + " 打印机序列");

                    }

                    @Override
                    public void print(int index, GpService gpService,Object take) {

                        Log.e("DOAING", index + "打印机执行打印：" + take);

                        Utils.sendReceiptWithResponse(index,gpService, (String) take);


                    }

                    @Override
                    public void out(int index, Object msg) {

                        Log.e("DOAING", index + "打印机打印完成：" + msg);

                    }
                })
                .setOpenStateListener(new MyOpenStateListen(this))
                .build();

    }

    void setMsg(String msg) {

        t1.setText(msg);
    }

    public void openWifi1(View view) {

        m1.openPort();

    }

    public void closeWifi2(View view) {

        MonitorSelector.getInstance().closeIndexMonitor(1);
    }


    public void getPrinterStatusClicked2(View view) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 3;

                m1.addMsgToWorkQueue(count + "");

            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 2;

                m1.addMsgToWorkQueue(count + "");

            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 1;

                m1.addMsgToWorkQueue(count + "");

            }
        }).start();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.e("DOAING", "干掉了？？");
    }

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                    1);
        }

        int permission1 = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.READ_PHONE_STATE);

        if (permission1 != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                    2);
        }
    }


    /**
     * 防止内存泄漏
     */
    public static class MyOpenStateListen implements Monitor.OpenStateListener {


        WeakReference<MainActivity> sr;

        MyOpenStateListen(MainActivity activity) {

            sr = new WeakReference<>(activity);

        }

        //启动是状态
        @Override
        public void openState(int index, int s, String msg) {

            sr.get().setMsg(index + msg);

        }

        //运行时状态
        @Override
        public void currentState(final int index, int s, final String msg) {
            // Log.e("DOAING", index + msg);

            if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
                sr.get().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sr.get().setMsg(index + msg);
                    }
                });
            } else {

                sr.get().setMsg(index + msg);
            }


        }
    }

}
