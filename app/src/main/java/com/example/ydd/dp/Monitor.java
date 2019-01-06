package com.example.ydd.dp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import com.gprinter.command.EscCommand;
import com.gprinter.command.GpCom;
import com.gprinter.command.GpUtils;
import com.gprinter.command.LabelCommand;
import com.gprinter.io.PortParameters;

import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import static com.gprinter.command.EscCommand.FONT.FONTA;
import static com.gprinter.service.GpPrintService.PRINTER_ID;

/**
 * @author dong
 * <p>
 * {打印机的管理器，打开关闭打印机，处理回调信息}
 */

public class Monitor {

    private boolean restart = false;
    private String ip;
    private int mPrinterIndex = -1;
    private static final int MAIN_QUERY_PRINTER_STATUS = 0xfe;
    private MyApplication myApplication;

    private boolean valid;


    private Object temporary;
    /**
     * 当前监视器的打印队列
     */
    private LinkedBlockingQueue<Object> linkedBlockingQueue = new LinkedBlockingQueue<>();


    /**
     * 打印机的所有状态在这个接口进行回调监听
     */
    private StatusChange statusChange;


    void addStatusChangeListener(StatusChange statusChange) {
        this.statusChange = statusChange;
    }

    Monitor(Context context) {

        myApplication = (MyApplication) context.getApplicationContext();

        BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                //打印机的id
                int id = intent.getIntExtra(PRINTER_ID, -1);

                if (id != mPrinterIndex) {
                    return;
                }

                // GpCom.ACTION_DEVICE_REAL_STATUS 为广播的IntentFilter
                if (GpCom.ACTION_DEVICE_REAL_STATUS.equals(action)) {

                    // 业务逻辑的请求码，对应哪里查询做什么操作
                    int requestCode = intent.getIntExtra(GpCom.EXTRA_PRINTER_REQUEST_CODE, -1);
                    // 判断请求码，是则进行业务操作
                    if (requestCode == MAIN_QUERY_PRINTER_STATUS) {

                        int status = intent.getIntExtra(GpCom.EXTRA_PRINTER_REAL_STATUS, 16);

                        String re;
                        if (status == GpCom.STATE_NO_ERR) {

                            if (restart) {
                                restart = false;
                            }

                            re = "打印机正常";
                            statusChange.onChanged(id, "已连接", re);
                            try {
                                //打印机连接是正常的，可以出栈了
                                if (linkedBlockingQueue.size() > 0) {


                                    temporary = linkedBlockingQueue.take();

                                    sendReceiptWithResponse(temporary);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        } else {
                            re = "打印机 " + id;
                            if ((byte) (status & GpCom.STATE_OFFLINE) > 0) {

                                re += "脱机--尝试重连";

                                if (!restart) {
                                    closePort(mPrinterIndex);
                                    openPort(mPrinterIndex, ip);
                                }
                                statusChange.onChanged(id, "已断开", re);
                            }
                            if ((byte) (status & GpCom.STATE_PAPER_ERR) > 0) {
                                re += "打印机缺纸";
                            }
                            if ((byte) (status & GpCom.STATE_COVER_OPEN) > 0) {
                                re += "打印机开盖";
                            }
                            if ((byte) (status & GpCom.STATE_ERR_OCCURS) > 0) {
                                re += "打印机出错";
                            }
                            if ((byte) (status & GpCom.STATE_TIMES_OUT) > 0) {

                                re += "连接超时关闭";
                                closePort(mPrinterIndex);
                                valid = false;
                                statusChange.onChanged(id, "已断开", re);
                            }
                        }

                    }
                } else if (GpCom.ACTION_RECEIPT_RESPONSE.equals(action)) {

                    temporary = null;

                    statusChange.onChanged(id, "已连接", "打印完成");

                }
            }
        };
        IntentFilter intentFilter = new IntentFilter(GpCom.ACTION_DEVICE_REAL_STATUS);
        intentFilter.addAction(GpCom.ACTION_RECEIPT_RESPONSE);


        // 注册实时状态查询广播
        myApplication.registerReceiver(mBroadcastReceiver, intentFilter);


    }


    /**
     * 仅仅是使用网络打印机
     *
     * @param mPrinterIndex 打印机的指针
     * @param ip            打印机的地址
     * @return 返回状态
     */

    int openPort(int mPrinterIndex, String ip) {

        valid = true;
        restart = false;
        this.ip = ip;

        this.mPrinterIndex = mPrinterIndex;

        int re = 0;
        try {
            re = myApplication.getmGpService().openPort(mPrinterIndex, PortParameters.ETHERNET, ip, 9100);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return re;
    }


    void closePort(int mPrinterIndex) {

        try {
            myApplication.getmGpService().closePort(mPrinterIndex);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    /**
     * 添加数据到队列里
     *
     * @param msg 传输数据
     */
    void addMsgToQueue(Object msg) {

        try {
            linkedBlockingQueue.put(msg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void take() {


        //上一次不为空说明打印失败，先保存，然后再去打印下一个
        if (temporary != null) {

            try {
                linkedBlockingQueue.put(temporary);

                temporary = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        //打印前检查一下打印机启用状态
        if (!valid) {
            return;
        }

        //如果队列不是空的去查询当前打印机的状态
        if (linkedBlockingQueue.size() > 0) {

            //打印前检查一下打印机的状态，获取返回值值再去打印
            getStatus();
        }
    }

    private void getStatus() {

        try {

            myApplication.getmGpService().queryPrinterStatus(mPrinterIndex, 500, MAIN_QUERY_PRINTER_STATUS);

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    /**
     * 实际要打印的方法
     *
     * @param msg 打印的数据
     */
    private void sendReceiptWithResponse(Object msg) {


        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
        //  打印文字
        esc.addText(msg.toString());

        esc.addSelectPrintModes(FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);// 取消倍高倍宽

        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);// 设置打印左对齐
        //  打印文字
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);// 设置打印左对齐
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
            rs = myApplication.getmGpService().sendEscCommand(mPrinterIndex, sss);
            GpCom.ERROR_CODE r = GpCom.ERROR_CODE.values()[rs];
            if (r != GpCom.ERROR_CODE.SUCCESS) {

                Log.e("DOAING", GpCom.getErrorText(r));
            }
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * @return 打印机的唯一索引
     */
    int getPrinterIndex() {
        return mPrinterIndex;
    }

    interface StatusChange {

        void onChanged(int index, String code, String massage);
    }

}
