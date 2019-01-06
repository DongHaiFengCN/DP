package com.example.ydd.dp;

import java.util.List;

/**
 * @author dong
 *
 * 说明：{ 这是打印需要的具体商品的信息 }
 */
public class PrintMerchandise {

    private String name;
    private float count;
    private String tasteName;
    private String description;
    private float price;
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
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public float getCount() {
        return count;
    }

    public void setCount(float count) {
        this.count = count;
    }

    public String getTasteName() {
        return tasteName;
    }

    public void setTasteName(String tasteName) {
        this.tasteName = tasteName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }


}
