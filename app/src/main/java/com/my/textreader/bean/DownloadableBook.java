package com.my.textreader.bean;

import java.io.Serializable;

// 可下载的书籍
public class DownloadableBook implements Serializable {
    private String bookId;
    private String bookName;
    private String author;
    private String description;
    private double price;
    private String downloadUrl; // 模拟下载链接
    private long fileSize; // 文件大小（字节）

    public DownloadableBook() {
    }

    public DownloadableBook(String bookId, String bookName, String author, String description, double price, String downloadUrl, long fileSize) {
        this.bookId = bookId;
        this.bookName = bookName;
        this.author = author;
        this.description = description;
        this.price = price;
        this.downloadUrl = downloadUrl;
        this.fileSize = fileSize;
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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
} 