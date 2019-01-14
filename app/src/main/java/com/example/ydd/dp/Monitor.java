package com.example.ydd.dp;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.util.Log;
import com.gprinter.aidl.GpService;
import com.gprinter.command.GpCom;
import com.gprinter.io.GpDevice;
import com.gprinter.io.PortParameters;
import com.gprinter.service.GpPrintService;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author dong
 *
 *  {每个打印机都有一个监听器，来处理自己的打印序列，和打印机的状态维护，通知用户等等}
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
    private int MAXIMUM_RECONNECTION_NUMBER = 3;
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

        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }

        openStateListener.openState(index,DISCONNECT,"手动关闭");
    }


    private void processData() {
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {

                //如果超过5秒还没有打印完成说明打印机出问题了
                if (currentTimeA != 0
                        && (System.currentTimeMillis() - currentTimeA) > MAXIMUM_TIMEOUT) {

                    openStateListener.currentState(index, DISCONNECT, "石沉大海:");
                    closePort();

                }
            }
        };
        timer.schedule(timerTask, 0, MAXIMUM_TIMEOUT);

        MyApplication.getExecutor().execute(new Runnable() {
            @Override
            public void run() {

                while (isRun) {

                    if (workQueue.size() > 0) {

                        //查询打印机的状态，当前线程堵塞
                        queryCurrentPrintConnectStatus();
                        currentTimeA = System.currentTimeMillis();
                        try {
                            synchronized (object) {
                                object.wait();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        currentTimeA = 0;
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

        Builder( Context context) {
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

                            if (connectNumber != 0) {
                                connectNumber = 0;
                            }

                            if (cache != null) {

                                lifeListener.print(index, gpService, cache);

                            }else {

                                try {
                                    if (workQueue.size() > 0) {

                                        cache = workQueue.take();

                                        lifeListener.print(index, gpService, cache);
                                    }

                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                        } else {

                            if ((byte) (status & GpCom.STATE_TIMES_OUT) > 0) {

                                queryCurrentPrintConnectStatus();
                                connectNumber++;
                                //连接超时重试三次，如果超过就断开连接
                                if (connectNumber == MAXIMUM_RECONNECTION_NUMBER) {
                                    openStateListener.currentState(index, DISCONNECT, "连接超时,检查网络重新连接");
                                    closePort();
                                }
                            }
                            if ((byte) (status & GpCom.STATE_PAPER_ERR) > 0 || (byte) (status & GpCom.STATE_COVER_OPEN) > 0) {

                                openStateListener.currentState(index, DISCONNECT, "设备缺纸/开盖,处理完成重新连接");
                                closePort();
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

                    synchronized (object) {
                        object.notify();
                        currentTimeA = 0;
                        openStateListener.currentState(index, CONNECTING, "打印完毕");
                        if (lifeListener != null) {
                            lifeListener.out(index, cache);
                        }
                        cache = null;
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(GpCom.ACTION_DEVICE_REAL_STATUS);
        intentFilter.addAction(GpCom.ACTION_RECEIPT_RESPONSE);
        context.registerReceiver(realStatusBroadcastReceiver, intentFilter);
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


}
