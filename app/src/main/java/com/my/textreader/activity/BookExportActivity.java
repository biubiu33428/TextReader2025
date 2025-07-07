package com.my.textreader.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.my.textreader.R;
import com.my.textreader.adapter.ExportBookAdapter;
import com.my.textreader.base.BaseActivity;
import com.my.textreader.db.BookList;

import org.litepal.crud.DataSupport;

import java.util.List;

public class BookExportActivity extends BaseActivity {

    private Toolbar toolbar;
    private ListView lvBooks;
    private ExportBookAdapter adapter;
    private List<BookList> bookList;

    @Override
    public int getLayoutRes() {
        return R.layout.activity_book_export;
    }

    @Override
    protected void initData() {
        toolbar = findViewById(R.id.toolbar);
        lvBooks = findViewById(R.id.lv_books);

        setSupportActionBar(toolbar);
        toolbar.setTitle("导出书本");
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // 加载书架上的书籍
        loadBooks();
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
                BookList book = bookList.get(position);
                startExport(book);
            }
        });
    }

    private void loadBooks() {
        new AsyncTask<Void, Void, List<BookList>>() {
            @Override
            protected List<BookList> doInBackground(Void... voids) {
                return DataSupport.findAll(BookList.class);
            }

            @Override
            protected void onPostExecute(List<BookList> books) {
                if (books.isEmpty()) {
                    Toast.makeText(BookExportActivity.this, "书架上没有书籍可以导出", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                
                bookList = books;
                adapter = new ExportBookAdapter(BookExportActivity.this, bookList);
                lvBooks.setAdapter(adapter);
            }
        }.execute();
    }

    private void startExport(BookList book) {
        // 跳转到导出进度页面
        Intent intent = new Intent(BookExportActivity.this, BookExportProgressActivity.class);
        intent.putExtra("book", book);
        startActivity(intent);
    }
} 