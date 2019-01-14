package com.example.ydd.dp;

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

     void addMonitor(Monitor monitor) {

        monitors[monitor.getIndex()] = monitor;


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
            monitor.addMsgToWorkQueue(msg);
        }
    }

    public void addFailureMsgQueue(Object msg) {
        try {
            failureMsgQueue.put(msg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
    public void closeIndexMonitor(int index){

        monitors[index].closePort();
    }
}
