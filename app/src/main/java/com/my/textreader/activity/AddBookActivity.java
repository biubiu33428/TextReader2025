package com.my.textreader.activity;

import android.os.AsyncTask;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.my.textreader.R;
import com.my.textreader.base.BaseActivity;
import com.my.textreader.db.BookList;

import org.litepal.crud.DataSupport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class AddBookActivity extends BaseActivity {

    private Toolbar toolbar;
    private EditText etBookTitle;
    private EditText etBookContent;
    private Spinner spinnerCategory;
    private EditText etAuthor;
    private EditText etDescription;
    private RatingBar ratingBar;
    private ImageView ivCoverPreview;
    private Button btnSelectCover;
    private String selectedCoverPath = null;
    
    private static final int SELECT_IMAGE_REQUEST = 1003;
    private static final int PERMISSION_REQUEST_CODE = 1004;

    @Override
    public int getLayoutRes() {
        return R.layout.activity_add_book;
    }

    @Override
    protected void initData() {
        toolbar = findViewById(R.id.toolbar);
        etBookTitle = findViewById(R.id.et_book_title);
        etBookContent = findViewById(R.id.et_book_content);
        spinnerCategory = findViewById(R.id.spinner_category);
        etAuthor = findViewById(R.id.et_author);
        etDescription = findViewById(R.id.et_description);
        ratingBar = findViewById(R.id.rating_bar);
        ivCoverPreview = findViewById(R.id.iv_cover_preview);
        btnSelectCover = findViewById(R.id.btn_select_cover);

        // 设置分类选择器
        String[] categories = {"人文社科", "文学经典", "小说", "英文书籍"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        setSupportActionBar(toolbar);
        toolbar.setTitle("添加书籍");
        
        // 显示返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        // 设置示例内容，帮助用户理解目录功能
        etBookContent.setHint("请输入书籍内容...\n\n提示：为了让目录功能正常工作，请在内容中包含章节标题，如：\n第一章 开始\n第二章 发展\n第三章 结束\n\n或者：\n第一回 故事开始\n第二回 情节发展");
    }

    @Override
    protected void initListener() {
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        btnSelectCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCoverImage();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_book, menu);
        // 添加示例书籍菜单项
        menu.add(0, R.id.action_add_sample, 0, "添加示例书籍");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save_book) {
            saveBook();
            return true;
        } else if (id == R.id.action_add_sample) {
            addSampleBook();
            return true;
        } else if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveBook() {
        String title = etBookTitle.getText().toString().trim();
        String content = etBookContent.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "请输入书籍标题", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "请输入书籍内容", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查书名是否重复
        SaveBookTask saveBookTask = new SaveBookTask();
        saveBookTask.execute(title, content);
    }
    
    private void addSampleBook() {
        String sampleTitle = "示例小说";
        String sampleContent = "第一章 开始的故事\n\n" +
                "    这是一个关于勇气和成长的故事。主人公小明是一个普通的高中生，直到有一天他发现了一个神秘的世界。\n\n" +
                "    在这个世界里，他遇到了许多奇妙的生物和不可思议的事件。\n\n" +
                "第二章 奇遇\n\n" +
                "    小明在森林里遇到了一只会说话的狐狸。狐狸告诉他，这个世界正面临着巨大的危机。\n\n" +
                "    只有找到传说中的七颗宝石，才能拯救这个世界。\n\n" +
                "第三章 冒险开始\n\n" +
                "    小明决定帮助狐狸寻找宝石。他们的第一个目标是位于雪山之巅的冰之宝石。\n\n" +
                "    路途艰险，但小明的决心让他克服了重重困难。\n\n" +
                "第四章 雪山之巅\n\n" +
                "    经过三天的艰苦跋涉，小明和狐狸终于到达了雪山之巅。\n\n" +
                "    在那里，他们遇到了守护宝石的冰龙。\n\n" +
                "第五章 智慧的考验\n\n" +
                "    冰龙并不想战斗，而是给小明出了一个谜题。\n\n" +
                "    只有解开谜题，才能获得冰之宝石。\n\n" +
                "第六章 宝石的力量\n\n" +
                "    小明成功解开了谜题，获得了第一颗宝石。\n\n" +
                "    宝石散发出美丽的光芒，让小明感受到了前所未有的力量。\n\n" +
                "第七章 新的伙伴\n\n" +
                "    在回程的路上，小明遇到了一个受伤的精灵。\n\n" +
                "    精灵愿意加入他们的队伍，一起寻找剩下的宝石。\n\n" +
                "第八章 火之试炼\n\n" +
                "    第二颗宝石位于火山深处。这次的考验更加危险。\n\n" +
                "    小明必须证明自己有足够的勇气和智慧。\n\n" +
                "第九章 友谊的力量\n\n" +
                "    在最关键的时刻，小明的伙伴们展现了真正的友谊。\n\n" +
                "    他们齐心协力，克服了看似不可能的挑战。\n\n" +
                "第十章 胜利的曙光\n\n" +
                "    随着最后一颗宝石的获得，世界重新恢复了和平。\n\n" +
                "    小明也从这次冒险中学到了很多宝贵的东西。";
        
        // 填写示例信息
        etBookTitle.setText(sampleTitle);
        etBookContent.setText(sampleContent);
        
        // 设置示例分类（小说）
        spinnerCategory.setSelection(2); // "小说"是第3个选项，索引为2
        
        // 设置示例作者
        etAuthor.setText("示例作者");
        
        // 设置示例简介
        etDescription.setText("这是一个充满冒险和友谊的奇幻故事。主人公小明在神秘世界中寻找七颗宝石，拯救世界的同时也收获了成长。");
        
        // 设置示例评分
        ratingBar.setRating(4.5f);
        
        Toast.makeText(this, "已添加示例书籍内容，您可以直接保存或修改", Toast.LENGTH_LONG).show();
    }

    private class SaveBookTask extends AsyncTask<String, Void, Integer> {
        private static final int SUCCESS = 1;
        private static final int DUPLICATE = 2;
        private static final int FAIL = 0;
        private String errorMessage = "";

        @Override
        protected Integer doInBackground(String... params) {
            String title = params[0];
            String content = params[1];

            try {
                Log.d("AddBookActivity", "开始保存书籍: " + title);
                
                // 检查是否已存在同名书籍
                List<BookList> existingBooks = DataSupport.where("bookname = ?", title).find(BookList.class);
                if (!existingBooks.isEmpty()) {
                    Log.d("AddBookActivity", "发现重复书名: " + title);
                    return DUPLICATE;
                }

                // 获取用户输入的信息
                String category = spinnerCategory.getSelectedItem().toString();
                String author = etAuthor.getText().toString().trim();
                String description = etDescription.getText().toString().trim();
                float rating = ratingBar.getRating();

                // 创建新书籍
                BookList newBook = new BookList();
                newBook.setBookname(title);
                newBook.setContent(content);
                newBook.setBookpath(""); // 设置为空，表示这是通过内容创建的书籍
                newBook.setBegin(0);
                newBook.setCharset("UTF-8");
                // 设置用户输入的信息
                newBook.setCategory(category);
                newBook.setAuthor(author.isEmpty() ? "未知作者" : author);
                newBook.setDescription(description.isEmpty() ? "暂无简介" : description);
                newBook.setRating(rating);
                newBook.setCoverPath(selectedCoverPath != null ? selectedCoverPath : "");

                Log.d("AddBookActivity", "准备保存到数据库...");
                boolean success = newBook.save();
                Log.d("AddBookActivity", "保存结果: " + success);
                
                if (success) {
                    Log.d("AddBookActivity", "书籍保存成功，ID: " + newBook.getId());
                } else {
                    Log.e("AddBookActivity", "书籍保存失败，save()返回false");
                }
                
                return success ? SUCCESS : FAIL;
            } catch (Exception e) {
                Log.e("AddBookActivity", "保存书籍时发生异常", e);
                errorMessage = e.getMessage();
                return FAIL;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            switch (result) {
                case SUCCESS:
                    Toast.makeText(AddBookActivity.this, "书籍添加成功", Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                case DUPLICATE:
                    Toast.makeText(AddBookActivity.this, "已存在同名书籍", Toast.LENGTH_SHORT).show();
                    break;
                case FAIL:
                    String message = "添加失败，请重试";
                    if (!TextUtils.isEmpty(errorMessage)) {
                        message += "\n错误: " + errorMessage;
                    }
                    Toast.makeText(AddBookActivity.this, message, Toast.LENGTH_LONG).show();
                    Log.e("AddBookActivity", "最终添加失败");
                    break;
            }
        }
    }
    
    // 选择封面图片
    private void selectCoverImage() {
        // 检查权限
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 
                    PERMISSION_REQUEST_CODE);
        } else {
            openImageSelector();
        }
    }
    
    // 打开图片选择器
    private void openImageSelector() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, SELECT_IMAGE_REQUEST);
    }
    
    // 保存图片到私有存储
    private String saveImageToPrivateStorage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                return null;
            }

            // 创建私有存储目录
            File coversDir = new File(getFilesDir(), "covers");
            if (!coversDir.exists()) {
                coversDir.mkdirs();
            }

            // 生成唯一的文件名
            String fileName = "cover_" + System.currentTimeMillis() + ".jpg";
            File outputFile = new File(coversDir, fileName);

            // 解码bitmap并压缩保存
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            if (bitmap == null) {
                return null;
            }

            // 压缩bitmap并保存
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            outputStream.close();

            // 回收bitmap
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }

            return outputFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImageSelector();
            } else {
                Toast.makeText(this, "需要存储权限来选择封面图片", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                try {
                    // 保存图片到私有存储
                    selectedCoverPath = saveImageToPrivateStorage(imageUri);
                    
                    // 更新预览
                    if (ivCoverPreview != null && selectedCoverPath != null) {
                        Bitmap bitmap = BitmapFactory.decodeFile(selectedCoverPath);
                        if (bitmap != null) {
                            ivCoverPreview.setImageBitmap(bitmap);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "选择图片失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
} 