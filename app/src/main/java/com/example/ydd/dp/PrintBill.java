package com.example.ydd.dp;

import java.util.List;

/**
 * @author dong
 * <p>
 * 说明：分单打印中的总单，order数据的载体
 */
public class PrintBill {


    /**
     * 区域号
     */
    private String areaName;

    /**
     * 桌号
     */
    private String tableName;

    /**
     * 就餐人数
     */
    private int currentPersons;

    /**
     * 菜品集合
     */

    private List<PrintMerchandise> dishList;

    /**
     * 备注
     */
    private String description;

    /**
     * 服务人员
     */
    private String employeeName;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 整单打印机个数
     */
    private int[] ids;

    public boolean isPrinted() {
        return isPrinted;
    }

    public void setPrinted(boolean printed) {
        isPrinted = printed;
    }

    /**
     * 是否已经打印过了
     */
    private boolean isPrinted;

    public int[] getIds() {
        return ids;
    }

    public void setIds(int[] ids) {
        this.ids = ids;
    }

    public String getAreaName() {
        return this.areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getTableName() {
        return this.tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public int getCurrentPersons() {
        return this.currentPersons;
    }

    public void setCurrentPersons(int currentPersons) {
        this.currentPersons = currentPersons;
    }

    public List<PrintMerchandise> getDishList() {
        return this.dishList;
    }

    public void setDishList(List<PrintMerchandise> dishList) {
        this.dishList = dishList;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEmployeeName() {
        return this.employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
