package com.example.ydd.dp;

import android.util.Log;

import com.couchbase.lite.MutableArray;
import com.couchbase.lite.MutableDocument;

import java.util.List;

/**
 * 完成打印序列的加载以及分配
 */
public class MonitorSelector {

    private static boolean isRun = false;

    private static Monitor[] monitorList = new Monitor[20];

    public static void startService() {

        if (isRun) return;

        Log.e("DOAING", "启动了～～～～");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                isRun = true;
                Monitor monitor;
                while (isRun) {

                    if (monitorList.length == 0) {
                        continue;
                    }
                    try {

                        //每隔一秒询问一次所有的monitor
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    for (int i = 0; i < 20; i++) {

                        monitor = monitorList[i];

                        if (monitor != null) {

                            monitor.take();
                        }
                    }
                }
                Log.e("DOAING", "执行完了");
            }
        });

        thread.start();


    }


    /**
     * 添加新打印机
     *
     * @param monitor 打印机监听器
     * @return 添加状态
     */
    static void addMonitor(Monitor monitor) {

        monitorList[monitor.getPrinterIndex()] = monitor;

    }


    /**
     * @param index 打印机指针
     * @param msg   打印机数据
     * @return 追加到打印队列是否成功
     * {添加数据到指定的打印机上去}
     */
    static boolean addMsgToQueue(int index, Object msg) {

        Monitor monitor = monitorList[index];

        //打印机不存在也就没有打印序列了
        if (monitor == null) {
            return false;
        }
        monitor.addMsgToQueue(msg);
        
        return true;
    }

    /**
     * 将数据持久化到数据库
     *
     * @param msg 订单
     */
    private static void persistence(Object msg) {

        if (msg instanceof PrintBill) {

            PrintBill printBill = (PrintBill) msg;
            Log.e("DOAING", msg.toString());

            MutableDocument mutableDocument = new MutableDocument();

            mutableDocument.setBoolean("isPrinted", printBill.isPrinted());
            mutableDocument.setString("areaName", printBill.getAreaName());
            mutableDocument.setString("tableName", printBill.getTableName());
            mutableDocument.setString("employeeName", printBill.getEmployeeName());
            mutableDocument.setString("createTime", printBill.getCreateTime());
            mutableDocument.setString("description", printBill.getDescription());

            int[] ids = printBill.getIds();
            MutableArray mutableArray = new MutableArray();
            for (int i = 0; i < ids.length; i++) {

                mutableArray.addInt(ids[i]);
            }
            mutableDocument.setArray("ids", mutableArray);

            List<PrintMerchandise> printMerchandises = printBill.getDishList();


            MutableArray mutableArray1 = new MutableArray();

            for (int i = 0; i < printMerchandises.size(); i++) {

                MutableDocument mutableDocument1 = new MutableDocument();


                PrintMerchandise printMerchandise = printMerchandises.get(i);

                mutableDocument1.setString("", printMerchandise.getDescription());


                mutableArray1.addValue(mutableDocument1);


            }


        }
    }


    /**
     * 关闭打印机监听
     */
    static void stopService() {

        isRun = false;
    }
}
