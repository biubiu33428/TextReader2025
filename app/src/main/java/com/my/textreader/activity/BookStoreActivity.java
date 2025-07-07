package com.my.textreader.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.my.textreader.R;
import com.my.textreader.adapter.BookStoreAdapter;
import com.my.textreader.base.BaseActivity;
import com.my.textreader.bean.DownloadableBook;
import com.my.textreader.db.PurchaseRecord;

import org.litepal.crud.DataSupport;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookStoreActivity extends BaseActivity {

    private Toolbar toolbar;
    private ListView lvBooks;
    private BookStoreAdapter adapter;
    private List<DownloadableBook> bookList;

    @Override
    public int getLayoutRes() {
        return R.layout.activity_book_store;
    }

    @Override
    protected void initData() {
        toolbar = findViewById(R.id.toolbar);
        lvBooks = findViewById(R.id.lv_books);

        setSupportActionBar(toolbar);
        toolbar.setTitle("书籍商店");
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // 初始化模拟书籍数据
        initBookData();
        
        adapter = new BookStoreAdapter(this, bookList);
        lvBooks.setAdapter(adapter);
    }

    @Override
    protected void initListener() {
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        lvBooks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DownloadableBook book = bookList.get(position);
                handleBookClick(book);
            }
        });
    }

    private void initBookData() {
        bookList = new ArrayList<>();
        
        // 模拟书籍数据
        bookList.add(new DownloadableBook("book_001", "三国演义", "罗贯中", 
                "中国古典四大名著之一，描述了东汉末年至西晋初年的历史故事", 9.9, 
                "http://example.com/sanguo.txt", 2048000));
        
        bookList.add(new DownloadableBook("book_002", "水浒传", "施耐庵", 
                "中国古典四大名著之一，讲述了梁山好汉的故事", 9.9, 
                "http://example.com/shuihu.txt", 1856000));
        
        bookList.add(new DownloadableBook("book_003", "西游记", "吴承恩", 
                "中国古典四大名著之一，孙悟空保护唐僧西天取经的故事", 9.9, 
                "http://example.com/xiyou.txt", 1792000));
        
        bookList.add(new DownloadableBook("book_004", "红楼梦", "曹雪芹", 
                "中国古典四大名著之一，贾宝玉与林黛玉的爱情悲剧", 9.9, 
                "http://example.com/honglou.txt", 2304000));
        
        bookList.add(new DownloadableBook("book_005", "平凡的世界", "路遥", 
                "描写中国西北农村生活变迁的现实主义小说", 9.9, 
                "http://example.com/pingfan.txt", 1536000));
        
        bookList.add(new DownloadableBook("book_006", "活着", "余华", 
                "讲述一个人一生的故事，体现人性的坚韧与生命的意义", 9.9, 
                "http://example.com/huozhe.txt", 512000));
    }

    private void handleBookClick(DownloadableBook book) {
        // 检查是否已购买
        CheckPurchaseTask task = new CheckPurchaseTask(book);
        task.execute();
    }

    private class CheckPurchaseTask extends AsyncTask<Void, Void, Boolean> {
        private DownloadableBook book;

        public CheckPurchaseTask(DownloadableBook book) {
            this.book = book;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            List<PurchaseRecord> records = DataSupport.where("bookId = ?", book.getBookId()).find(PurchaseRecord.class);
            return !records.isEmpty();
        }

        @Override
        protected void onPostExecute(Boolean isPurchased) {
            if (isPurchased) {
                // 已购买，直接下载
                startDownload(book);
            } else {
                // 未购买，跳转到支付页面
                Intent intent = new Intent(BookStoreActivity.this, PaymentActivity.class);
                intent.putExtra("book", book);
                startActivityForResult(intent, 1001);
            }
        }
    }

    private void startDownload(DownloadableBook book) {
        Intent intent = new Intent(BookStoreActivity.this, DownloadActivity.class);
        intent.putExtra("book", book);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // 支付成功，更新适配器
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }
} 