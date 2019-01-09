package com.example.ydd.dp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.gprinter.aidl.GpService;
import com.gprinter.command.EscCommand;
import com.gprinter.command.EscCommand.JUSTIFICATION;
import com.gprinter.command.GpCom;
import com.gprinter.command.GpUtils;
import com.gprinter.command.LabelCommand;
import com.gprinter.io.GpDevice;
import com.gprinter.io.PortParameters;
import com.gprinter.service.GpPrintService;

import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

class Monitor {

    BroadcastReceiver openStatusBroadcastReceiver;
    BroadcastReceiver realStatusBroadcastReceiver;
    private boolean isRun = false;
    private boolean isPause = true;
    private static final int CONNECTING = 0;
    static final int CONNECTED = 1;
    private static final int DISCONNECT = 2;
    private Context context;
    private String ip;
    private int index;
    private OpenStateListener openStateListener;
    private LinkedBlockingQueue<Object> linkedBlockingQueue = new LinkedBlockingQueue<>();
    private static final int MAIN_QUERY_PRINTER_STATUS = 0xfe;
    private static final int REQUEST_PRINT_LABEL = 0xfd;
    private static final int REQUEST_PRINT_RECEIPT = 0xfc;


    private GpService gpService;
    private final Object object = new Object();

    private Monitor(Builder builder) {
        this.index = builder.index;
        this.context = builder.context;
        this.ip = builder.ip;
        //注册打开打印机状态广播
        registerOpenPortBroadcast();
        registerRealStatusBroadcast();
        //发起打印机注册请求
        connectPort();


    }

    void setOpenStateListener(OpenStateListener openStateListener) {
        this.openStateListener = openStateListener;
    }

    public int getIndex() {
        return index;
    }

    public void queryCurrentPrintConnectStatus() {


        try {
            gpService.queryPrinterStatus(index, 800, MAIN_QUERY_PRINTER_STATUS);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    /**
     * 发起连接请求
     */
    private void connectPort() {

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

    public void addMsgToQueue(Object object) {

        try {
            linkedBlockingQueue.put(object);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static class Builder {
        private final Context context;

        Builder(@NonNull Context context) {
            this.context = context.getApplicationContext();
        }

        //默认数据
        private String ip;
        private int index = 0;

        Builder setIp(@NonNull String ip) {
            this.ip = ip;
            return this;
        }

        public Builder setIndex(int index) {
            this.index = index;
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

                        openStateListener.state(index, CONNECTING, "正在连接");

                    } else if (type == GpDevice.STATE_NONE) {

                        openStateListener.state(index, DISCONNECT, "未连接");
                        //解绑打开的状态广播
                        context.unregisterReceiver(openStatusBroadcastReceiver);

                    } else if (type == GpDevice.STATE_VALID_PRINTER) {

                        openStateListener.state(index, CONNECTED, "已连接");

                        processData();

                        //解绑打开的状态广播
                        context.unregisterReceiver(openStatusBroadcastReceiver);

                    } else if (type == GpDevice.STATE_INVALID_PRINTER) {

                        openStateListener.state(index, DISCONNECT, "打印机不可以使用");
                        //解绑打开的状态广播
                        context.unregisterReceiver(openStatusBroadcastReceiver);

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
                Log.e("TAG", action);
                int id = intent.getIntExtra(GpPrintService.PRINTER_ID, -1);
                if (id != index) return;
                // GpCom.ACTION_DEVICE_REAL_STATUS 为广播的IntentFilter
                if (action.equals(GpCom.ACTION_DEVICE_REAL_STATUS)) {
                    // 业务逻辑的请求码，对应哪里查询做什么操作
                    int requestCode = intent.getIntExtra(GpCom.EXTRA_PRINTER_REQUEST_CODE, -1);
                    // 判断请求码，是则进行业务操作
                    if (requestCode == MAIN_QUERY_PRINTER_STATUS) {

                        int status = intent.getIntExtra(GpCom.EXTRA_PRINTER_REAL_STATUS, 16);
                        String str;
                        if (status == GpCom.STATE_NO_ERR) {
                            str = "打印机正常";

                            try {
                                sendReceiptWithResponse((String) linkedBlockingQueue.take());
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }


                        } else {
                            str = "打印机 ";
                            if ((byte) (status & GpCom.STATE_OFFLINE) > 0) {
                                str += "脱机";

                            }
                            if ((byte) (status & GpCom.STATE_PAPER_ERR) > 0) {
                                str += "缺纸";
                            }
                            if ((byte) (status & GpCom.STATE_COVER_OPEN) > 0) {
                                str += "打印机开盖";
                            }
                            if ((byte) (status & GpCom.STATE_ERR_OCCURS) > 0) {
                                str += "打印机出错";
                            }
                            if ((byte) (status & GpCom.STATE_TIMES_OUT) > 0) {
                                str += "查询超时";
                                //重试连接
                                queryCurrentPrintConnectStatus();
                            }
                        }

                        Log.e("DOAING", " 状态：" + str);

                    } else if (requestCode == REQUEST_PRINT_LABEL) {
                        int status = intent.getIntExtra(GpCom.EXTRA_PRINTER_REAL_STATUS, 16);
                        if (status == GpCom.STATE_NO_ERR) {

                        } else {

                        }
                    } else if (requestCode == REQUEST_PRINT_RECEIPT) {
                        int status = intent.getIntExtra(GpCom.EXTRA_PRINTER_REAL_STATUS, 16);
                        if (status == GpCom.STATE_NO_ERR) {

                            Log.e("DOAING", "打印机" + id + ": REQUEST_PRINT_RECEIPT");
                        } else {

                        }
                    }
                } else if (action.equals(GpCom.ACTION_RECEIPT_RESPONSE)) {

                    synchronized(object){
                        object.notify();
                        Log.e("DOAING","打印成功");
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(GpCom.ACTION_DEVICE_REAL_STATUS);

        intentFilter.addAction(GpCom.ACTION_RECEIPT_RESPONSE);
        context.registerReceiver(realStatusBroadcastReceiver, intentFilter);
    }


    /**
     * 启动/重新启动数据处理线程
     */
    private void processData() {


        if (isRun) return;


        MyApplication.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                isRun = true;
                Log.e("DOAING", index + "数据处理启动了");
                while (isRun) {
                    if (linkedBlockingQueue.size() > 0) {


                        //查询打印机的状态，当前线程堵塞
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
     * 关闭当前的打印机监听与数据处理，不销毁当前队列数据
     */
    void close() {
        isRun = false;
    }

    interface OpenStateListener {
        void state(int index, int s, String msg);
    }

    void sendReceiptWithResponse(String msg) {
        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
        esc.addPrintAndFeedLines((byte) 3);
        esc.addSelectJustification(JUSTIFICATION.CENTER);// 设置打印居中
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);// 设置为倍高倍宽
        esc.addText("Sample\n"); // 打印文字
        esc.addPrintAndLineFeed();

        /* 打印繁体中文 需要打印机支持繁体字库 */
        String message = "佳博智匯票據打印機\n";
        // esc.addText(message,"BIG5");
        esc.addText(msg, "GB2312");
        esc.addPrintAndLineFeed();

        /*        *//*
         * QRCode命令打印 此命令只在支持QRCode命令打印的机型才能使用。 在不支持二维码指令打印的机型上，则需要发送二维条码图片
         *//*
        esc.addText("Print QRcode\n"); // 打印文字
        esc.addSelectErrorCorrectionLevelForQRCode((byte) 0x31); // 设置纠错等级
        esc.addSelectSizeOfModuleForQRCode((byte) 3);// 设置qrcode模块大小
        esc.addStoreQRCodeData("www.smarnet.cc");// 设置qrcode内容
        esc.addPrintQRCode();// 打印QRCode
        esc.addPrintAndLineFeed();*/

        /* 打印文字 */
        esc.addSelectJustification(JUSTIFICATION.CENTER);// 设置打印左对齐
        esc.addText("Completed!\r\n"); // 打印结束
        // 开钱箱
        esc.addGeneratePlus(LabelCommand.FOOT.F5, (byte) 255, (byte) 255);
        esc.addPrintAndFeedLines((byte) 8);

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
