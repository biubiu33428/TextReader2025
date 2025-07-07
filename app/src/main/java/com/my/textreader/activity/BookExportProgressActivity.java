package com.my.textreader.activity;

import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.my.textreader.R;
import com.my.textreader.base.BaseActivity;
import com.my.textreader.db.BookList;
import com.my.textreader.util.EncryptionUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BookExportProgressActivity extends BaseActivity {

    private Toolbar toolbar;
    private TextView tvBookInfo;
    private TextView tvExportPath;
    private ProgressBar progressBar;
    private TextView tvProgress;
    private TextView tvExportStatus;
    private Button btnClose;

    private BookList book;
    private boolean isExporting = false;
    private String exportFilePath;

    @Override
    public int getLayoutRes() {
        return R.layout.activity_book_export_progress;
    }

    @Override
    protected void initData() {
        toolbar = findViewById(R.id.toolbar);
        tvBookInfo = findViewById(R.id.tv_book_info);
        tvExportPath = findViewById(R.id.tv_export_path);
        progressBar = findViewById(R.id.progress_bar);
        tvProgress = findViewById(R.id.tv_progress);
        tvExportStatus = findViewById(R.id.tv_export_status);
        btnClose = findViewById(R.id.btn_close);

        setSupportActionBar(toolbar);
        toolbar.setTitle("导出书本");
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // 获取传递的书籍信息
        book = (BookList) getIntent().getSerializableExtra("book");
        if (book != null) {
            tvBookInfo.setText("正在导出：" + book.getBookname());
            
            // 生成导出文件路径
            String fileName = book.getBookname() + "_" + 
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".tre";
            
            // 使用应用专用外部存储目录，不需要额外权限
            File exportDir;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Android 10+ 使用应用专用目录
                exportDir = new File(getExternalFilesDir(null), "Export");
            } else {
                // Android 9 及以下使用公共目录
                exportDir = new File(Environment.getExternalStorageDirectory(), "TextReader/Export");
            }
            
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            
            exportFilePath = new File(exportDir, fileName).getAbsolutePath();
            tvExportPath.setText("导出路径：" + exportFilePath);
            
            // 自动开始导出
            startExport();
        }

        btnClose.setVisibility(View.GONE);
    }

    @Override
    protected void initListener() {
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isExporting) {
                    finish();
                }
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void startExport() {
        isExporting = true;
        tvExportStatus.setText("准备导出...");
        progressBar.setProgress(0);
        tvProgress.setText("0%");

        // 开始导出过程
        ExportTask task = new ExportTask();
        task.execute();
    }

    private class ExportTask extends AsyncTask<Void, Integer, Boolean> {
        
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                // 获取书籍内容
                publishProgress(10);
                String content = getBookContent();
                if (content == null || content.isEmpty()) {
                    return false;
                }
                
                // 加密内容
                publishProgress(30);
                String encryptedContent = EncryptionUtil.encrypt(content);
                if (encryptedContent == null) {
                    return false;
                }
                
                // 添加文件头标识
                publishProgress(50);
                String finalContent = EncryptionUtil.addEncryptedHeader(encryptedContent);
                
                // 模拟处理时间
                for (int i = 50; i <= 80; i += 10) {
                    if (isCancelled()) return false;
                    publishProgress(i);
                    Thread.sleep(300);
                }
                
                // 写入文件
                publishProgress(90);
                return writeToFile(finalContent);
                
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            int progress = values[0];
            progressBar.setProgress(progress);
            tvProgress.setText(progress + "%");
            
            if (progress <= 10) {
                tvExportStatus.setText("读取书籍内容...");
            } else if (progress <= 30) {
                tvExportStatus.setText("加密处理中...");
            } else if (progress <= 50) {
                tvExportStatus.setText("生成安全文件...");
            } else if (progress <= 80) {
                tvExportStatus.setText("准备写入文件...");
            } else if (progress <= 90) {
                tvExportStatus.setText("保存到本地...");
            } else {
                tvExportStatus.setText("完成导出...");
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            isExporting = false;
            progressBar.setProgress(100);
            tvProgress.setText("100%");
            
            if (success) {
                tvExportStatus.setText("导出成功！");
                tvExportStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                
                Toast.makeText(BookExportProgressActivity.this, 
                    "书籍已成功导出到：" + exportFilePath, Toast.LENGTH_LONG).show();
                
                // 延迟显示关闭按钮
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        btnClose.setVisibility(View.VISIBLE);
                    }
                }, 1000);
                
            } else {
                tvExportStatus.setText("导出失败！");
                tvExportStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                btnClose.setVisibility(View.VISIBLE);
                Toast.makeText(BookExportProgressActivity.this, "导出失败，请重试", Toast.LENGTH_LONG).show();
            }
        }
    }

    private String getBookContent() {
        if (book.getContent() != null && !book.getContent().isEmpty()) {
            // 内容型书籍，直接返回内容
            return book.getContent();
        } else if (book.getBookpath() != null && !book.getBookpath().isEmpty()) {
            // 文件型书籍，读取文件内容
            try {
                return com.my.textreader.util.FileUtils.readTextFile(book.getBookpath());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    private boolean writeToFile(String content) {
        try {
            File file = new File(exportFilePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes("UTF-8"));
            fos.close();
            
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        if (!isExporting) {
            super.onBackPressed();
        }
    }
} 