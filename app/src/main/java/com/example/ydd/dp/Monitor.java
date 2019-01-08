package com.example.ydd.dp;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.util.Log;

import com.gprinter.aidl.GpService;
import com.gprinter.command.GpCom;
import com.gprinter.io.GpDevice;
import com.gprinter.io.PortParameters;
import com.gprinter.service.GpPrintService;

import java.util.concurrent.LinkedBlockingQueue;

class Monitor {

    private static boolean isRun = false;
    private Context context;
    private String ip;
    private int index;
    private int portNumber;
    private OpenStateListener openStateListener;
    private LinkedBlockingQueue<Object> linkedBlockingQueue;

    private Monitor(Builder builder) {

        this.openStateListener = builder.openStateListener;
        this.index = builder.index;
        this.context = builder.context;
        this.ip = builder.ip;
        this.portNumber = builder.portNumber;

        //注册打开打印机状态广播
        registerOpenPortBroadcast();

        //发起打印机注册请求
        connectPort();
    }

    /**
     * 发起连接请求
     */
    private void connectPort() {

        GpService gpService = ((MyApplication) context).getmGpService();
        try {
            gpService.openPort(index, PortParameters.ETHERNET, ip, portNumber);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static class Builder {

        //必填数据
        private OpenStateListener openStateListener;
        private final Context context;

        Builder(@NonNull Context context, OpenStateListener openStateListener) {
            this.context = context.getApplicationContext();
            this.openStateListener = openStateListener;
        }

        //默认数据
        private String ip = "192.168.2.248";
        private int index = 0;
        private int portNumber;

        Builder setPortNumber(int portNumber) {
            this.portNumber = portNumber;
            return this;
        }

        Builder setIp(@NonNull String ip) {
            this.ip = ip;
            return this;
        }

        public Builder setIndex(int index) {
            this.index = index;
            return this;
        }

        void build() {
            new Monitor(this);
        }

    }



    /**
     * 注册打开打印机状态的广播
     */
    private void registerOpenPortBroadcast() {

        IntentFilter filter = new IntentFilter();
        filter.addAction(GpCom.ACTION_CONNECT_STATUS);
        context.registerReceiver(PrinterStatusBroadcastReceiver, filter);
    }


    /**
     * 广播接收打印机是否启动成功
     */
    private BroadcastReceiver PrinterStatusBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GpCom.ACTION_CONNECT_STATUS.equals(intent.getAction())) {
                int id = intent.getIntExtra(GpPrintService.PRINTER_ID, 0);
                if (id != index) return;
                int type = intent.getIntExtra(GpPrintService.CONNECT_STATUS, 0);
                if (type == GpDevice.STATE_CONNECTING) {

                    openStateListener.state(index, "正在连接");

                } else if (type == GpDevice.STATE_NONE) {

                    openStateListener.state(index, "断开");
                    //解绑打开的状态广播
                    context.unregisterReceiver(PrinterStatusBroadcastReceiver);

                } else if (type == GpDevice.STATE_VALID_PRINTER) {

                    openStateListener.state(index, "打印机可以使用");

                    linkedBlockingQueue = new LinkedBlockingQueue<>();

                    processData();

                    //解绑打开的状态广播
                    context.unregisterReceiver(PrinterStatusBroadcastReceiver);

                } else if (type == GpDevice.STATE_INVALID_PRINTER) {

                    openStateListener.state(index, "打印机不可以使用");
                    //解绑打开的状态广播
                    context.unregisterReceiver(PrinterStatusBroadcastReceiver);

                }


            }
        }
    };

    /**
     * 启动数据处理线程
     */
    private void processData() {

        if (isRun) return;

        MyApplication.getExecutor().submit(new Runnable() {
            @Override
            public void run() {
                isRun = true;
                Log.e("DOAING", index + "数据处理启动了");
                while (isRun) {

                    if (linkedBlockingQueue.size() > 0) {

                        try {
                            Log.e("DOANG", index + "出队" + linkedBlockingQueue.take());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

    }
    /**
     * 关闭当前的打印机监听与数据处理，不销毁当前队列数据
     */
    void close() {
        isRun = false;
    }

    interface OpenStateListener {

        void state(int index, String msg);
    }
}
