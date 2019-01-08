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

    }

    @Override
    protected void onRestart() {
        super.onRestart();

    }

    public void getPrinterStatusClicked1(View view) {


    }

    public void getPrinterStatusClicked2(View view) {


    }

    public void openWifi1(View view) {


        new Monitor.Builder(getApplicationContext(), new Monitor.OpenStateListener() {
            @Override
            public void state(int index, String msg) {

                Log.e("DOAING", index + msg);

            }
        }).setIndex(0).setIp("192.168.2.200").setPortNumber(9100).build();


    }

    public void openWifi2(View view) {


        new Monitor.Builder(getApplicationContext(), new Monitor.OpenStateListener() {
            @Override
            public void state(int index, String msg) {

                Log.e("DOAING", index + msg);

            }
        }).setIndex(1).setIp("192.168.2.248").setPortNumber(9100).build();
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
