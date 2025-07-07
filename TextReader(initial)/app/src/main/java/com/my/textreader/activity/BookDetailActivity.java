package com.my.textreader.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.my.textreader.R;
import com.my.textreader.base.BaseActivity;
import com.my.textreader.db.BookList;

import java.io.File;

public class BookDetailActivity extends BaseActivity implements View.OnClickListener {
    
    private static final String EXTRA_BOOK = "bookList";
    
    private ImageView ivCover;
    private TextView tvTitle;
    private TextView tvAuthor;
    private TextView tvCategory;
    private RatingBar ratingBar;
    private TextView tvDescription;
    private Button btnStartReading;
    private Button btnBack;
    
    private BookList bookList;

    @Override
    public int getLayoutRes() {
        return R.layout.activity_book_detail;
    }

    @Override
    protected void initData() {
        // 初始化控件
        ivCover = findViewById(R.id.iv_book_cover);
        tvTitle = findViewById(R.id.tv_book_title);
        tvAuthor = findViewById(R.id.tv_book_author);
        tvCategory = findViewById(R.id.tv_book_category);
        ratingBar = findViewById(R.id.rating_book);
        tvDescription = findViewById(R.id.tv_book_description);
        btnStartReading = findViewById(R.id.btn_start_reading);
        btnBack = findViewById(R.id.btn_back);
        
        // 获取传递的书籍信息
        Intent intent = getIntent();
        bookList = (BookList) intent.getSerializableExtra(EXTRA_BOOK);
        
        if (bookList != null) {
            loadBookData();
        }
    }

    @Override
    protected void initListener() {
        btnStartReading.setOnClickListener(this);
        btnBack.setOnClickListener(this);
    }
    
    private void loadBookData() {
        // 设置书名
        String bookName = bookList.getBookname();
        if (bookName != null) {
            // 去掉文件扩展名
            if (bookName.endsWith(".txt")) {
                bookName = bookName.substring(0, bookName.length() - 4);
            }
            tvTitle.setText(bookName);
        } else {
            tvTitle.setText("未知书籍");
        }
        
        // 设置作者
        String author = bookList.getAuthor();
        if (author != null && !author.trim().isEmpty()) {
            tvAuthor.setText("作者：" + author);
        } else {
            tvAuthor.setText("作者：未知");
        }
        
        // 设置分类
        String category = bookList.getCategory();
        if (category != null && !category.trim().isEmpty()) {
            tvCategory.setText("分类：" + category);
        } else {
            tvCategory.setText("分类：未分类");
        }
        
        // 设置评分
        float rating = bookList.getRating();
        if (rating > 0) {
            ratingBar.setRating(rating);
        } else {
            ratingBar.setRating(5.0f); // 默认5星
        }
        
        // 设置简介
        String description = bookList.getDescription();
        if (description != null && !description.trim().isEmpty()) {
            tvDescription.setText(description);
        } else {
            tvDescription.setText("暂无图书简介");
        }
        
        // 设置封面
        loadBookCover();
    }
    
    private void loadBookCover() {
        String coverPath = bookList.getCoverPath();
        if (coverPath != null && !coverPath.trim().isEmpty()) {
            File coverFile = new File(coverPath);
            if (coverFile.exists()) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(coverPath);
                    if (bitmap != null) {
                        ivCover.setImageBitmap(bitmap);
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        // 如果没有封面或加载失败，使用默认封面
        ivCover.setImageResource(R.mipmap.cover_default_new);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start_reading:
                // 开始阅读
                ReadActivity.openBook(bookList, this);
                finish();
                break;
            case R.id.btn_back:
                // 返回
                finish();
                break;
        }
    }
    
    public static void openBookDetail(BookList bookList, Activity context) {
        if (bookList == null) {
            throw new NullPointerException("BookList cannot be null");
        }
        
        Intent intent = new Intent(context, BookDetailActivity.class);
        intent.putExtra(EXTRA_BOOK, bookList);
        context.startActivity(intent);
    }
} 