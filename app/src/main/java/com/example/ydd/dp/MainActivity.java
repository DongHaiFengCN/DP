package com.example.ydd.dp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    Monitor m1, m2;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions(this);

        m1 = new Monitor(getApplicationContext());
        m1.addStatusChangeListener(new Monitor.StatusChange() {
            @Override
            public void onChanged(int index, String code, String massage) {
                Log.e("DOAING", index + "--" + code + "--" + massage);


            }

        });
        m2 = new Monitor(getApplicationContext());
        m2.addStatusChangeListener(new Monitor.StatusChange() {
            @Override
            public void onChanged(int index, String code, String massage) {
                Log.e("DOAING", index + "--" + code + "--" + massage);
            }


        });
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        Log.e("DOAING", "重启了？？？");
    }

    public void getPrinterStatusClicked1(View view) {

        if (MonitorSelector.addMsgToQueue(0, "1-1")) {
            Log.e("DOAING", "添加成功～");
        } else {
            Log.e("DOAING", "添加失败～");
        }


    }

    public void getPrinterStatusClicked2(View view) {

        if (MonitorSelector.addMsgToQueue(1, "2-1")) {
            Log.e("DOAING", "添加成功～");
        } else {
            Log.e("DOAING", "添加失败～");
        }


    }

    public void openWifi1(View view) {


        if (m1.openPort(0, "192.168.2.200") == 0) {
            Log.e("DOAING", 0 + "成功～");
            MonitorSelector.addMonitor(m1);

        }


    }

    public void openWifi2(View view) {


        if (m2.openPort(1, "192.168.2.248") == 0) {
            Log.e("DOAING", 1 + "成功～");

            MonitorSelector.addMonitor(m2);
        }

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


}
