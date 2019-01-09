package com.example.ydd.dp;

import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

public class MonitorSelector {
    private static MonitorSelector monitorSelector;
    /**
     * 用来保存开启成功的打印机监听
     */
    private static Monitor[] monitors = new Monitor[20];
    /**
     * 用来保存失败的数据
     */
    private static LinkedBlockingQueue<Object> failureMsgQueue = new LinkedBlockingQueue<>();

    public static MonitorSelector getInstance() {
        if (monitorSelector == null) {
            monitorSelector = new MonitorSelector();
        }
        return monitorSelector;
    }

    public boolean addMonitor(Monitor monitor) {

        if (monitor == null) {
            return false;
        }

        if (monitors[monitor.getIndex()] == null) {
            monitors[monitor.getIndex()] = monitor;
            return true;
        }
        return false;
    }

    /**
     * @param index 打印机指针
     * @param msg   打印机数据
     * @return 追加到打印队列是否成功
     * {添加数据到指定的打印机上去}
     */
    void addMsgToQueue(int index, Object msg) {

        Monitor monitor = monitors[index];
        //没有找到打印机就添加到失败的消息队列去
        if (monitor == null) {
            addFailureMsgQueue(msg);

        } else {
            monitor.addMsgToQueue(msg);
        }
    }

    private void addFailureMsgQueue(Object msg) {
        try {
            failureMsgQueue.put(msg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void getIndexPrinterStatus(int index) {

        if (monitors[index] == null) return;

        monitors[index].queryCurrentPrintConnectStatus();
    }

    /**
     * 一个一个的把为找到打印机的单子拿出来
     *
     * @return
     */
    public Object takeFailMsgOneByOne() {

        if (failureMsgQueue.size() > 0) {
            try {
                return failureMsgQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
