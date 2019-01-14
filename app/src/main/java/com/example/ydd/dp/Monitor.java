package com.example.ydd.dp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.gprinter.aidl.GpService;
import com.gprinter.command.EscCommand;
import com.gprinter.command.GpCom;
import com.gprinter.command.GpUtils;
import com.gprinter.command.LabelCommand;
import com.gprinter.io.GpDevice;
import com.gprinter.io.PortParameters;
import com.gprinter.service.GpPrintService;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author dong
 * <p>
 * {每个打印机都有一个监听器，来处理自己的打印序列，和打印机的状态维护，通知用户等等}
 */
class Monitor {

    private BroadcastReceiver openStatusBroadcastReceiver;
    private BroadcastReceiver realStatusBroadcastReceiver;
    private boolean isRun;
    private static final int CONNECTING = 0;
    static final int CONNECTED = 1;
    private static final int DISCONNECT = 2;
    private Context context;
    private String ip;
    private int index;
    private OpenStateListener openStateListener;
    private LifeListener lifeListener;
    private LinkedBlockingQueue<Object> workQueue = new LinkedBlockingQueue<>();
    private static final int MAIN_QUERY_PRINTER_STATUS = 0xfe;
    private GpService gpService;
    private final Object object = new Object();
    private int MAXIMUM_RECONNECTION_NUMBER = 5;
    private int connectNumber;

    private volatile long currentTimeA = 0;
    private volatile long MAXIMUM_TIMEOUT = 5000;

    private Object cache;

    private Timer timer;
    private TimerTask timerTask;

    private Monitor(Builder builder) {
        this.index = builder.index;
        this.context = builder.context;
        this.ip = builder.ip;
        this.lifeListener = builder.lifeListener;
        this.openStateListener = builder.openStateListener;
    }

    /**
     * 发起打印机连接/重启（必须要先调用关闭方法）
     */
    void openPort() {

        if (isRun) return;

        Log.e("DOAING", workQueue.toString());

        connectNumber = 0;
        isRun = true;
        //注册打开打印机状态广播
        registerOpenPortBroadcast();
        registerRealStatusBroadcast();

        MyApplication.getExecutor().execute(new Runnable() {
            @Override
            public void run() {

                gpService = ((MyApplication) context).getmGpService();

                try {
                    gpService.openPort(index, PortParameters.ETHERNET, ip, 9100);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 关闭打印机连接
     */
    void closePort() {

        if (gpService == null) return;
        currentTimeA = 0;
        if (isRun) {
            try {
                gpService.closePort(index);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        isRun = false;
        if (realStatusBroadcastReceiver != null) {
            context.unregisterReceiver(realStatusBroadcastReceiver);
            realStatusBroadcastReceiver = null;
        }


    }


    private void processData() {
     /*   timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {

                //如果超过5秒还没有打印完成说明打印机出问题了
                if (currentTimeA != 0
                        && (System.currentTimeMillis() - currentTimeA) > MAXIMUM_TIMEOUT) {

                    openStateListener.currentState(index, DISCONNECT, "打印机没有响应");
                    closePort();

                }
            }
        };
        timer.schedule(timerTask, 0, MAXIMUM_TIMEOUT);*/

        MyApplication.getExecutor().execute(new Runnable() {
            @Override
            public void run() {

                while (isRun) {

                    //监测到一组新的订单
                    if (workQueue.size() > 0) {
                        //查询一下打印机状态

                        queryCurrentPrintConnectStatus();

                        try {
                            synchronized (object) {
                                object.wait();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                    }
                }
            }
        });
    }

    /**
     * 添加数据到打印机的工作队列
     *
     * @param object
     */
    void addMsgToWorkQueue(Object object) {

        if (lifeListener != null) {
            lifeListener.in(index, object);
        }

        try {
            workQueue.put(object);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取打印机的索引
     */
    public int getIndex() {
        return index;
    }

    /**
     * 查询打印机的状态
     */
    private void queryCurrentPrintConnectStatus() {

        try {

            gpService.queryPrinterStatus(index, 500, MAIN_QUERY_PRINTER_STATUS);

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static class Builder {
        private final Context context;
        private OpenStateListener openStateListener;

        Builder(Context context) {
            this.context = context.getApplicationContext();
        }

        //默认数据
        private String ip;
        private int index = 0;
        private LifeListener lifeListener;

        Builder setIp(String ip) {
            this.ip = ip;
            return this;
        }

        public Builder setIndex(int index) {
            this.index = index;
            return this;
        }

        Builder setLifeListener(LifeListener lifeListener) {
            this.lifeListener = lifeListener;
            return this;
        }

        /**
         * 获取打印机打开时的状态
         *
         * @param openStateListener 接口
         */
        public Builder setOpenStateListener(OpenStateListener openStateListener) {
            this.openStateListener = openStateListener;
            return this;
        }

        Monitor build() {
            return new Monitor(this);
        }
    }

    /**
     * 注册打开打印机状态的广播
     */
    private void registerOpenPortBroadcast() {

        openStatusBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (GpCom.ACTION_CONNECT_STATUS.equals(intent.getAction())) {
                    int id = intent.getIntExtra(GpPrintService.PRINTER_ID, 0);
                    if (id != index) return;
                    int type = intent.getIntExtra(GpPrintService.CONNECT_STATUS, 0);
                    if (type == GpDevice.STATE_CONNECTING) {

                        openStateListener.openState(index, CONNECTING, "正在连接");

                    } else if (type == GpDevice.STATE_NONE) {

                        openStateListener.openState(index, DISCONNECT, "未连接");
                        //解绑打开的状态广播
                        context.unregisterReceiver(openStatusBroadcastReceiver);

                        closePort();

                    } else if (type == GpDevice.STATE_VALID_PRINTER) {

                        MonitorSelector.getInstance().addMonitor(Monitor.this);

                        openStateListener.openState(index, CONNECTED, "已连接");
                        //初始化监听
                        processData();

                        //解绑打开的状态广播
                        context.unregisterReceiver(openStatusBroadcastReceiver);

                    } else if (type == GpDevice.STATE_INVALID_PRINTER) {

                        openStateListener.openState(index, DISCONNECT, "打印机不可以使用");
                        //解绑打开的状态广播
                        context.unregisterReceiver(openStatusBroadcastReceiver);
                        closePort();

                    }
                }
            }
        };

        context.registerReceiver(openStatusBroadcastReceiver, new IntentFilter(GpCom.ACTION_CONNECT_STATUS));
    }

    /**
     * 注册打印机实时状态监听
     */
    private void registerRealStatusBroadcast() {

        realStatusBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public synchronized void onReceive(Context context, Intent intent) {

                String action = intent.getAction();
                int id = intent.getIntExtra(GpPrintService.PRINTER_ID, -1);
                if (id != index) return;
                // GpCom.ACTION_DEVICE_REAL_STATUS 为广播的IntentFilter
                if (GpCom.ACTION_DEVICE_REAL_STATUS.equals(action)) {
                    // 业务逻辑的请求码，对应哪里查询做什么操作
                    int requestCode = intent.getIntExtra(GpCom.EXTRA_PRINTER_REQUEST_CODE, -1);
                    // 判断请求码，是则进行业务操作
                    if (requestCode == MAIN_QUERY_PRINTER_STATUS) {
                        int status = intent.getIntExtra(GpCom.EXTRA_PRINTER_REAL_STATUS, 16);
                        if (status == GpCom.STATE_NO_ERR) {
                            openStateListener.currentState(index, CONNECTED, "开始打印");
                            printMethod();

                        } else {

                            if ((byte) (status & GpCom.STATE_TIMES_OUT) > 0) {

                                openStateListener.currentState(index, DISCONNECT, "连接超时,检查网络重新连接");

                                closePort();
                            }
                            if ((byte) (status & GpCom.STATE_PAPER_ERR) > 0 || (byte) (status & GpCom.STATE_COVER_OPEN) > 0) {

                                openStateListener.currentState(index, DISCONNECT, "设备缺纸/开盖");
                                printMethod();

                            }
                            if ((byte) (status & GpCom.STATE_ERR_OCCURS) > 0) {

                                openStateListener.currentState(index, DISCONNECT, "打印机出错,请检查打印机并尝试重新手动连接");
                                closePort();
                            }
                            if ((byte) (status & GpCom.STATE_OFFLINE) > 0) {

                                openStateListener.currentState(index, DISCONNECT, "设备离线请重新连接");
                                closePort();
                            }
                        }

                    }
                } else if (GpCom.ACTION_RECEIPT_RESPONSE.equals(action)) {

                    printMethod();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(GpCom.ACTION_DEVICE_REAL_STATUS);
        intentFilter.addAction(GpCom.ACTION_RECEIPT_RESPONSE);
        context.registerReceiver(realStatusBroadcastReceiver, intentFilter);
    }

    private void printMethod() {
        if (workQueue.size() > 0) {


            try {
                lifeListener.print(index, gpService, workQueue.take());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } else {
            synchronized (object) {
                object.notify();
                cache = null;
            }
        }
    }


    interface OpenStateListener {
        void openState(int index, int s, String msg);

        void currentState(int index, int s, String msg);
    }

    interface LifeListener {

        void in(int index, Object msg);

        void print(int index, GpService gpService, Object take);

        void out(int index, Object msg);
    }

    void sendReceiptWithResponse(String msg) {
        EscCommand esc = new EscCommand();
        esc.addPrintAndFeedLines((byte) 3);
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);// 设置打印居中
        /* 打印文字 */
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);// 设置打印左对齐
        esc.addText(msg + "\r\n"); // 打印结束
        // 加入查询打印机状态，打印完成后，此时会接收到GpCom.ACTION_DEVICE_STATUS广播
        esc.addQueryPrinterStatus();
        Vector<Byte> datas = esc.getCommand(); // 发送数据
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        String sss = Base64.encodeToString(bytes, Base64.DEFAULT);
        int rs;
        try {
            rs = gpService.sendEscCommand(index, sss);
            GpCom.ERROR_CODE r = GpCom.ERROR_CODE.values()[rs];
            if (r != GpCom.ERROR_CODE.SUCCESS) {

                Log.e("DOAING", "错误信息：" + GpCom.getErrorText(r));
            }
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}



