package com.my.textreader.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.AbsoluteLayout;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.molihuan.pathselector.PathSelector;
import com.molihuan.pathselector.entity.FileBean;
import com.molihuan.pathselector.entity.FontBean;
import com.molihuan.pathselector.fragment.BasePathSelectFragment;
import com.molihuan.pathselector.fragment.impl.PathSelectFragment;
import com.molihuan.pathselector.listener.CommonItemListener;
import com.molihuan.pathselector.utils.MConstants;
import com.my.textreader.Config;
import com.my.textreader.R;
import com.my.textreader.adapter.ShelfAdapter;
import com.my.textreader.animation.ContentScaleAnimation;
import com.my.textreader.animation.Rotate3DAnimation;
import com.my.textreader.base.BaseActivity;
import com.my.textreader.db.BookList;
import com.my.textreader.view.DragGridView;

import org.litepal.crud.DataSupport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends BaseActivity
        implements Animation.AnimationListener {

    Toolbar toolbar;
    FloatingActionButton fab;
    DragGridView bookShelf;
    TabLayout tabLayout;
    SearchView searchView;

    private WindowManager mWindowManager;
    private AbsoluteLayout wmRootView;
    private View rootView;
    private Typeface typeface;

    private List<BookList> bookLists;
    private List<BookList> originalBookLists; // 原始数据列表
    private ShelfAdapter adapter;
    private String[] categories = {"全部", "人文社科", "文学经典", "小说", "英文书籍"};
    private String currentCategory = "全部";
    private String currentSearchQuery = "";
    //点击书本的位置
    private int itemPosition;
    private TextView itemTextView;
    //点击书本在屏幕中的x，y坐标
    private int[] location = new int[2];

    private static TextView cover;
    private static ImageView content;
    //书本打开动画缩放比例
    private float scaleTimes;
    //书本打开缩放动画
    private static ContentScaleAnimation contentAnimation;
    private static Rotate3DAnimation coverAnimation;
    //书本打开缩放动画持续时间
    public static final int ANIMATION_DURATION = 800;
    //打开书本的第一个动画是否完成
    private boolean mIsOpen = false;
    //动画加载计数器  0 默认  1一个动画执行完毕   2二个动画执行完毕
    private int animationCount = 0;

    private static Boolean isExit = false;
    
    // 权限请求码
    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private static final int REQUEST_MANAGE_EXTERNAL_STORAGE = 1002;
    
    // 图片选择相关
    private static final int SELECT_IMAGE_REQUEST = 1003;
    private static final int PERMISSION_REQUEST_CODE = 1004;
    private List<String> pendingBooks = new ArrayList<>();
    private String selectedCoverPath = null;
    private ImageView currentCoverPreview = null;


    @Override
    public int getLayoutRes() {
        return R.layout.activity_main;
    }

    @Override
    protected void initData() {
        toolbar = findViewById(R.id.toolbar);
        fab = findViewById(R.id.fab);
        bookShelf = findViewById(R.id.bookShelf);
        tabLayout = findViewById(R.id.tab_layout);
        searchView = findViewById(R.id.search_view);
        
        setSupportActionBar(toolbar);
       // toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);//设置导航图标

        // 删除窗口背景
        getWindow().setBackgroundDrawable(null);
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        wmRootView = new AbsoluteLayout(this);
        rootView = getWindow().getDecorView();
        
        // 获取所有书架电子书
        originalBookLists = DataSupport.findAll(BookList.class);
        bookLists = new ArrayList<>(originalBookLists);
        adapter = new ShelfAdapter(MainActivity.this, bookLists);
        bookShelf.setAdapter(adapter);
        
        // 初始化 TabLayout
        initTabLayout();
        
        // 初始化 SearchView
        initSearchView();
        
        // 检查存储权限
        checkStoragePermission();
    }

    @Override
    protected void initListener() {
//        悬浮添加电子书
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 显示选择对话框：文件选择 或 手动输入
                showAddBookDialog();
            }
        });

//点击条目打开书本
        bookShelf.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (bookLists.size() > position) {
                    itemPosition = position;
                    String bookname = bookLists.get(itemPosition).getBookname();

                    adapter.setItemToFirst(itemPosition);
//                bookLists = DataSupport.findAll(BookList.class);
                    final BookList bookList = bookLists.get(itemPosition);
                    bookList.setId(bookLists.get(0).getId());
                    
                    // 检查是否是内容型书籍（通过content字段创建的书籍）
                    if (bookList.getContent() != null && !bookList.getContent().isEmpty()) {
                        // 内容型书籍，跳转到详情页
                        BookDetailActivity.openBookDetail(bookList, MainActivity.this);
                    } else {
                        // 文件型书籍，检查文件是否存在
                        final String path = bookList.getBookpath();
                        File file = new File(path);
                        if (!file.exists()) {
                                                    new AlertDialog.Builder(MainActivity.this)
                                .setTitle(MainActivity.this.getString(R.string.app_name))
                                .setMessage(path + "文件不存在,是否删除该书本？")
                                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        DataSupport.deleteAll(BookList.class, "bookpath = ?", path);
                                        originalBookLists = DataSupport.findAll(BookList.class);
                                        filterBooks();
                                    }
                                }).setCancelable(true).show();
                            return;
                        }
                        
                        // 文件型书籍，跳转到详情页
                        BookDetailActivity.openBookDetail(bookList, MainActivity.this);
                    }
               }
            }
        });
    }


    @Override
    protected void onRestart() {
        super.onRestart();
        System.out.println("-----------re----------");
        DragGridView.setIsShowDeleteButton(false);
        originalBookLists = DataSupport.findAll(BookList.class);
        filterBooks();
        closeBookAnimation();
    }

    @Override
    protected void onStop() {
        DragGridView.setIsShowDeleteButton(false);
        super.onStop();
    }
//关删除按钮
    @Override
    protected void onDestroy() {
        DragGridView.setIsShowDeleteButton(false);
        super.onDestroy();
    }



    /**
     * 在2秒内按下返回键两次才退出
     */
    private void exitBy2Click() {
        // press twice to exit
        Timer tExit;
        if (!isExit) {
            isExit = true; // ready to exit
            if (DragGridView.getShowDeleteButton()) {
                DragGridView.setIsShowDeleteButton(false);
                //要保证是同一个adapter对象,否则在Restart后无法notifyDataSetChanged
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, this.getResources().getString(R.string.press_twice_to_exit), Toast.LENGTH_SHORT).show();
            }
            tExit = new Timer();
            tExit.schedule(new TimerTask() {
                @Override
                public void run() {
                    isExit = false; // cancel exit
                }
            }, 2000); // 2 seconds cancel exit task

        } else {
            finish();
            // call fragments and end streams and services
            System.exit(0);
        }
    }


//按返回键
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
                exitBy2Click();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }
//打开和关闭书籍动画相关
    public void closeBookAnimation() {

        if (mIsOpen && wmRootView != null) {
            //因为书本打开后会移动到第一位置，所以要设置新的位置参数
            contentAnimation.setmPivotXValue(bookShelf.getFirstLocation()[0]);
            contentAnimation.setmPivotYValue(bookShelf.getFirstLocation()[1]);
            coverAnimation.setmPivotXValue(bookShelf.getFirstLocation()[0]);
            coverAnimation.setmPivotYValue(bookShelf.getFirstLocation()[1]);

            AbsoluteLayout.LayoutParams params = new AbsoluteLayout.LayoutParams(
                    itemTextView.getLayoutParams());
            params.x = bookShelf.getFirstLocation()[0];
            params.y = bookShelf.getFirstLocation()[1];//firstLocation[1]在滑动的时候回改变,所以要在dispatchDraw的时候获取该位置值
            wmRootView.updateViewLayout(cover, params);
            wmRootView.updateViewLayout(content, params);
            //动画逆向运行
            if (!contentAnimation.getMReverse()) {
                contentAnimation.reverse();
            }
            if (!coverAnimation.getMReverse()) {
                coverAnimation.reverse();
            }
            //清除动画再开始动画
            content.clearAnimation();
            content.startAnimation(contentAnimation);
            cover.clearAnimation();
            cover.startAnimation(coverAnimation);
        }
    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {
        //有两个动画监听会执行两次，所以要判断
        if (!mIsOpen) {
            animationCount++;
            if (animationCount >= 2) {
                mIsOpen = true;
                adapter.setItemToFirst(itemPosition);
//                bookLists = DataSupport.findAll(BookList.class);
                BookList bookList = bookLists.get(itemPosition);
                bookList.setId(bookLists.get(0).getId());
                ReadActivity.openBook(bookList, MainActivity.this);
            }

        } else {
            animationCount--;
            if (animationCount <= 0) {
                mIsOpen = false;
                wmRootView.removeView(cover);
                wmRootView.removeView(content);
                mWindowManager.removeView(wmRootView);
            }
        }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }



    // 创建菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // 处理菜单项点击
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_download_books) {
            // 跳转到书籍下载页面
            Intent intent = new Intent(MainActivity.this, BookStoreActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_select_file) {
            // 跳转到书籍导出页面
            Intent intent = new Intent(MainActivity.this, BookExportActivity.class);
            startActivity(intent);
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * 检查存储权限
     */
    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 需要管理外部存储权限
            if (!Environment.isExternalStorageManager()) {
                new AlertDialog.Builder(this)
                    .setTitle("需要存储权限")
                    .setMessage("应用需要存储权限来保存和读取书籍文件，请在设置中授予权限")
                    .setPositiveButton("去设置", (dialog, which) -> {
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE);
                        } catch (Exception e) {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                            startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE);
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            }
        } else {
            // Android 10 及以下版本
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }, REQUEST_STORAGE_PERMISSION);
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "存储权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "存储权限被拒绝，部分功能可能无法正常使用", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImageSelector();
            } else {
                Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MANAGE_EXTERNAL_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "存储权限已授予", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "存储权限被拒绝，部分功能可能无法正常使用", Toast.LENGTH_LONG).show();
                }
            }
        } else if (requestCode == 635 && resultCode == RESULT_OK && data != null) {
            // 处理文件选择结果
            ArrayList<String> selectedFiles = data.getStringArrayListExtra("selected_files");
            if (selectedFiles != null && !selectedFiles.isEmpty()) {
                pendingBooks.clear();
                pendingBooks.addAll(selectedFiles);
                
                // 显示选择文件数量的提示
                String message = selectedFiles.size() == 1 ? 
                    "已选择 1 个文件" : 
                    String.format("已选择 %d 个文件", selectedFiles.size());
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                
                // 弹出对话框输入书籍信息
                showBookInfoDialog();
            }
        } else if (requestCode == SELECT_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                try {
                    // 保存图片到私有存储
                    selectedCoverPath = saveImageToPrivateStorage(imageUri);
                    
                    // 更新预览
                    if (currentCoverPreview != null && selectedCoverPath != null) {
                        Bitmap bitmap = BitmapFactory.decodeFile(selectedCoverPath);
                        if (bitmap != null) {
                            currentCoverPreview.setImageBitmap(bitmap);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "选择图片失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void initTabLayout() {
        // 添加分类标签
        for (String category : categories) {
            tabLayout.addTab(tabLayout.newTab().setText(category));
        }
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentCategory = categories[tab.getPosition()];
                filterBooks();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // 不需要处理
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // 不需要处理
            }
        });
    }

    private void initSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query;
                filterBooks();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                filterBooks();
                return true;
            }
        });

        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    searchView.clearFocus();
                }
            }
        });
    }

    private void filterBooks() {
        List<BookList> filteredList = new ArrayList<>();
        
        for (BookList book : originalBookLists) {
            boolean matchesCategory = currentCategory.equals("全部") || 
                                    (book.getCategory() != null && book.getCategory().equals(currentCategory));
            
            boolean matchesSearch = currentSearchQuery.isEmpty() || 
                                  (book.getBookname() != null && book.getBookname().toLowerCase().contains(currentSearchQuery.toLowerCase())) ||
                                  (book.getAuthor() != null && book.getAuthor().toLowerCase().contains(currentSearchQuery.toLowerCase()));
            
            if (matchesCategory && matchesSearch) {
                filteredList.add(book);
            }
        }
        
        bookLists.clear();
        bookLists.addAll(filteredList);
        adapter.setBookList(bookLists);
    }

    private void showAddBookDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加书籍")
                .setItems(new String[]{"从文件选择", "手动输入内容"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            // 文件选择
                            selectFilesToAdd();
                        } else {
                            // 手动输入
                            Intent intent = new Intent(MainActivity.this, AddBookActivity.class);
                            startActivity(intent);
                        }
                    }
                });
        builder.show();
    }

    private void selectFilesToAdd() {
        Intent intent = new Intent(MainActivity.this, FileSelectActivity.class);
        startActivityForResult(intent, 635);
    }

    // 显示书籍信息输入对话框
    private void showBookInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_book_info, null);
        builder.setView(dialogView);

        // 初始化控件
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_category);
        EditText etAuthor = dialogView.findViewById(R.id.et_author);
        EditText etDescription = dialogView.findViewById(R.id.et_description);
        RatingBar ratingBar = dialogView.findViewById(R.id.rating_bar);
        ImageView ivCoverPreview = dialogView.findViewById(R.id.iv_cover_preview);
        Button btnSelectCover = dialogView.findViewById(R.id.btn_select_cover);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        // 设置分类选择器
        String[] dialogCategories = {"人文社科", "文学经典", "小说", "英文书籍"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dialogCategories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        // 保存当前预览控件的引用
        currentCoverPreview = ivCoverPreview;

        AlertDialog dialog = builder.create();

        // 选择封面按钮点击事件
        btnSelectCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCoverImage();
            }
        });

        // 取消按钮点击事件
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                pendingBooks.clear();
                selectedCoverPath = null;
                currentCoverPreview = null;
            }
        });

        // 确定按钮点击事件
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String category = spinnerCategory.getSelectedItem().toString();
                String author = etAuthor.getText().toString().trim();
                String description = etDescription.getText().toString().trim();
                float rating = ratingBar.getRating();

                // 创建书籍列表
                List<BookList> bookLists = new ArrayList<>();
                for (String filePath : pendingBooks) {
                    File file = new File(filePath);
                    BookList bookList = new BookList();
                    bookList.setBookname(file.getName());
                    bookList.setBookpath(filePath);
                    bookList.setCategory(category);
                    bookList.setAuthor(author.isEmpty() ? "未知作者" : author);
                    bookList.setDescription(description.isEmpty() ? "暂无简介" : description);
                    bookList.setRating(rating);
                    bookList.setCoverPath(selectedCoverPath);
                    bookList.setBegin(0);
                    bookList.setCharset("UTF-8");
                    bookLists.add(bookList);
                }

                // 保存到数据库
                SaveBookToSqlLiteTask mSaveBookToSqlLiteTask = new SaveBookToSqlLiteTask();
                mSaveBookToSqlLiteTask.execute(bookLists);

                dialog.dismiss();
                pendingBooks.clear();
                selectedCoverPath = null;
                currentCoverPreview = null;
            }
        });

        dialog.show();
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

    private class SaveBookToSqlLiteTask extends AsyncTask<List<BookList>, Void, Integer> {
        private static final int FAIL = 0;
        private static final int SUCCESS = 1;
        private static final int REPEAT = 2;
        private BookList repeatBookList;

        @Override
        protected Integer doInBackground(List<BookList>... params) {
            List<BookList> bookLists = params[0];
            for (BookList bookList : bookLists) {
                List<BookList> existingBooks = DataSupport.where("bookname = ?", bookList.getBookname()).find(BookList.class);
                if (!existingBooks.isEmpty()) {
                    repeatBookList = bookList;
                    return REPEAT;
                }
                bookList.save();
            }
            return SUCCESS;
        }

        @Override
        protected void onPostExecute(Integer result) {
            String msg = "";
            switch (result) {
                case FAIL:
                    msg = "导入书本失败";
                    break;
                case REPEAT:
                    msg = "《" + repeatBookList.getBookname() + "》已存在";
                    break;
                case SUCCESS:
                    msg = "导入书本成功";
                    // 重新加载数据并应用过滤
                    originalBookLists = DataSupport.findAll(BookList.class);
                    filterBooks();
                    break;
            }

            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
        }
    }
}
