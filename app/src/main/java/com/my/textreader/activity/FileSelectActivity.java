package com.my.textreader.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.my.textreader.R;
import com.my.textreader.adapter.FileListAdapter;
import com.my.textreader.bean.FileItem;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileSelectActivity extends AppCompatActivity {
    private ListView listView;
    private TextView tvPath;
    private Button btnConfirm;
    private Button btnCancel;
    private FileListAdapter adapter;
    private List<FileItem> fileItems;
    private List<FileItem> selectedFiles;
    private File currentDir;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_select);
        
        initViews();
        initData();
        initListeners();
    }
    
    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("📚 选择文本文件");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        listView = findViewById(R.id.lv_files);
        tvPath = findViewById(R.id.tv_path);
        btnConfirm = findViewById(R.id.btn_confirm);
        btnCancel = findViewById(R.id.btn_cancel);
        
        fileItems = new ArrayList<>();
        selectedFiles = new ArrayList<>();
        adapter = new FileListAdapter(this, fileItems, selectedFiles);
        listView.setAdapter(adapter);
    }
    
    private void initData() {
        currentDir = Environment.getExternalStorageDirectory();
        loadFiles(currentDir);
    }
    
    private void initListeners() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FileItem item = fileItems.get(position);
                if (item.isDirectory()) {
                    loadFiles(item.getFile());
                } else {
                    // 切换文件选择状态
                    boolean wasSelected = selectedFiles.contains(item);
                    if (wasSelected) {
                        selectedFiles.remove(item);
                        Toast.makeText(FileSelectActivity.this, "取消选择: " + item.getDisplayName(), Toast.LENGTH_SHORT).show();
                    } else {
                        selectedFiles.add(item);
                        Toast.makeText(FileSelectActivity.this, "已选择: " + item.getDisplayName(), Toast.LENGTH_SHORT).show();
                    }
                    adapter.notifyDataSetChanged();
                    updateConfirmButton();
                }
            }
        });
        
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedFiles.isEmpty()) {
                    Toast.makeText(FileSelectActivity.this, "请选择至少一个文本文件", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 返回选择的文件
                Intent result = new Intent();
                ArrayList<String> filePaths = new ArrayList<>();
                for (FileItem item : selectedFiles) {
                    filePaths.add(item.getFile().getAbsolutePath());
                }
                result.putStringArrayListExtra("selected_files", filePaths);
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        });
        
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });
    }
    
    private void loadFiles(File dir) {
        if (dir == null || !dir.exists()) return;
        
        currentDir = dir;
        tvPath.setText(dir.getAbsolutePath());
        
        fileItems.clear();
        
        // 添加返回上级目录选项
        if (dir.getParent() != null) {
            fileItems.add(new FileItem(new File(dir.getParent()), "📁 返回上级目录", true));
        }
        
        File[] files = dir.listFiles();
        if (files != null) {
            List<File> fileList = Arrays.asList(files);
            Collections.sort(fileList, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    if (f1.isDirectory() && !f2.isDirectory()) {
                        return -1;
                    } else if (!f1.isDirectory() && f2.isDirectory()) {
                        return 1;
                    } else {
                        return f1.getName().compareToIgnoreCase(f2.getName());
                    }
                }
            });
            
            for (File file : fileList) {
                if (file.isDirectory()) {
                    fileItems.add(new FileItem(file, "📁 " + file.getName(), true));
                } else if (file.getName().toLowerCase().endsWith(".txt")) {
                    fileItems.add(new FileItem(file, "📄 " + file.getName(), false));
                }
            }
        }
        
        adapter.notifyDataSetChanged();
        updateConfirmButton();
    }
    
    private void updateConfirmButton() {
        if (selectedFiles.isEmpty()) {
            btnConfirm.setText("📖 加入书架");
            btnConfirm.setBackgroundColor(getResources().getColor(R.color.gray));
            btnConfirm.setEnabled(false);
            getSupportActionBar().setTitle("📚 选择文本文件");
        } else {
            btnConfirm.setText(String.format("📖 加入书架 (%d)", selectedFiles.size()));
            btnConfirm.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            btnConfirm.setEnabled(true);
            getSupportActionBar().setTitle(String.format("📚 已选择 %d 个文件", selectedFiles.size()));
        }
    }
    
    @Override
    public void onBackPressed() {
        if (currentDir != null && currentDir.getParent() != null) {
            loadFiles(currentDir.getParentFile());
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 