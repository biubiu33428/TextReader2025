package com.my.textreader.db;

import org.litepal.crud.DataSupport;

import java.io.Serializable;

// 购买记录
public class PurchaseRecord extends DataSupport implements Serializable {
    private int id;
    private String bookId; // 书籍ID
    private String bookName; // 书籍名称
    private double price; // 购买价格
    private String purchaseTime; // 购买时间
    private boolean downloaded; // 是否已下载

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getPurchaseTime() {
        return purchaseTime;
    }

    public void setPurchaseTime(String purchaseTime) {
        this.purchaseTime = purchaseTime;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }
} 