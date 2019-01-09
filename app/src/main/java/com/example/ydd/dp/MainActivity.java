package com.example.ydd.dp;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    Monitor m1, m2;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions(this);

    }


    public void openWifi1(View view) {

  /*      m1 = new Monitor.Builder(getApplicationContext())
                .setIndex(0)
                .setIp("192.168.2.200")
                .build();

        m1.setOpenStateListener(new MyOpenStateListen(m1) );*/

    }

    public void openWifi2(View view) {

        m2 = new Monitor.Builder(getApplicationContext())
                .setIndex(1)
                .setIp("192.168.2.248")
                .build();
        m2.setOpenStateListener(new MyOpenStateListen(m2));
    }

    public void getPrinterStatusClicked1(View view) {


      // MonitorSelector.getInstance().addMsgToQueue(0, "0");

    }

    int count;
    public void getPrinterStatusClicked2(View view) {

       MonitorSelector.getInstance().addMsgToQueue(1, (count++)+"");

    }

    public void getStatus(View view){

      //  MonitorSelector.getInstance().getIndexPrinterStatus(1);

    }

    public void get(View view){

        //Log.e("DOAING", MonitorSelector.getInstance().takeFailMsgOneByOne()+"");
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

        Monitor monitor;

        MyOpenStateListen(Monitor monitor) {

            this.monitor = monitor;
        }

        @Override
        public void state(int index, int s, String msg) {
            //启动成功
            if (s == Monitor.CONNECTED) {

                if (MonitorSelector.getInstance().addMonitor(monitor)) {

                    Log.e("DOAING", index + "添加成功");
                } else {
                    Log.e("DOAING", "当前id已被占用/打印机为空");
                }
            }else {
                Log.e("DOAING", index+msg);
            }
        }
    }

}
