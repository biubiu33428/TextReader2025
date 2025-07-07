package com.my.textreader.activity;

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
import android.provider.MediaStore;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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

import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.SearchView;
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
    
    // 图片选择相关
    private static final int SELECT_IMAGE_REQUEST = 1001;
    private static final int PERMISSION_REQUEST_CODE = 1002;
    private List<FileBean> pendingBooks = new ArrayList<>();
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
    }

    @Override
    protected void initListener() {
//        悬浮添加电子书
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PathSelectFragment selector = PathSelector.build(MainActivity.this, MConstants.BUILD_ACTIVITY)
                        .setRequestCode(635)
                        .setShowSelectStorageBtn(false)
                        .setShowFileTypes("txt")
                        .setSelectFileTypes("txt")
                        .setMaxCount(-1)
                        .setTitlebarMainTitle(new FontBean("选择文件", 18, Color.WHITE, null))
                        .setMorePopupItemListeners(
                                new CommonItemListener(new FontBean("加入书架", 18, Color.WHITE, null)) {
                                    @Override
                                    public boolean onClick(View v, TextView tv, List<FileBean> selectedFiles, String currentPath, BasePathSelectFragment pathSelectFragment) {

                                        if (selectedFiles.size() == 0) {
                                            Toast.makeText(MainActivity.this, "未选择文件", Toast.LENGTH_SHORT).show();
                                            return false;
                                        }
                                        // 保存待处理的书籍文件
                                        pendingBooks = selectedFiles;
                                        pathSelectFragment.mActivity.finish();
                                        // 弹出对话框输入书籍信息
                                        showBookInfoDialog();
                                        return false;
                                    }
                                }
                        )
                        .show();
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
                                        bookLists = DataSupport.findAll(BookList.class);
                                        adapter.setBookList(bookLists);
                                    }
                                }).setCancelable(true).show();
                        return;
                    }

                    // 跳转到书籍详情页
                    BookDetailActivity.openBookDetail(bookList, MainActivity.this);

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

    // 初始化 TabLayout
    private void initTabLayout() {
        // 添加标签页
        for (String category : categories) {
            tabLayout.addTab(tabLayout.newTab().setText(category));
        }
        
        // 设置标签页选择监听器
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

    // 初始化 SearchView
    private void initSearchView() {
        if (searchView == null) {
            System.out.println("SearchView is null!");
            return;
        }
        
        // 设置 SearchView 的样式和行为
        searchView.setIconified(false);
        searchView.setIconifiedByDefault(false);
        searchView.setSubmitButtonEnabled(false);
        searchView.setQueryHint("搜索书名或作者...");
        searchView.clearFocus();
        
        // 设置文字颜色和样式
        try {
            // 获取 SearchView 内部的 EditText
            int searchEditTextId = androidx.appcompat.R.id.search_src_text;
            EditText searchEditText = searchView.findViewById(searchEditTextId);
            if (searchEditText != null) {
                searchEditText.setTextColor(Color.BLACK);  // 设置输入文字颜色为黑色
                searchEditText.setHintTextColor(Color.GRAY); // 设置提示文字颜色为灰色
                searchEditText.setBackgroundColor(Color.WHITE); // 确保背景是白色
                searchEditText.setPadding(10, 10, 10, 10); // 设置内边距
            }
        } catch (Exception e) {
            System.out.println("Error setting SearchView text color: " + e.getMessage());
        }
        
        System.out.println("SearchView initialized successfully");
        
        // 设置搜索监听器
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                System.out.println("Search submitted: " + query);
                currentSearchQuery = query;
                filterBooks();
                searchView.clearFocus(); // 隐藏键盘
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                System.out.println("Search text changed: " + newText);
                currentSearchQuery = newText;
                filterBooks();
                return true;
            }
        });
        
        // 设置焦点变化监听器
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                System.out.println("SearchView focus changed: " + hasFocus);
                if (!hasFocus) {
                    // 当失去焦点时，如果搜索框为空，清除搜索
                    if (currentSearchQuery.isEmpty()) {
                        searchView.setQuery("", false);
                    }
                }
            }
        });
    }

    // 过滤书籍数据
    private void filterBooks() {
        System.out.println("Filtering books - Category: " + currentCategory + ", Search: " + currentSearchQuery);
        List<BookList> filteredBooks = new ArrayList<>();
        
        for (BookList book : originalBookLists) {
            boolean categoryMatch = true;
            boolean searchMatch = true;
            
            // 按分类过滤
            if (!currentCategory.equals("全部")) {
                String bookCategory = book.getCategory();
                if (bookCategory == null || !bookCategory.equals(currentCategory)) {
                    categoryMatch = false;
                }
            }
            
            // 按书名和作者搜索
            if (!currentSearchQuery.isEmpty()) {
                String searchLower = currentSearchQuery.toLowerCase();
                String bookName = book.getBookname();
                String author = book.getAuthor();
                
                boolean bookNameMatch = false;
                boolean authorMatch = false;
                
                // 检查书名匹配
                if (bookName != null) {
                    // 去掉文件扩展名进行搜索
                    String displayName = bookName;
                    if (displayName.endsWith(".txt")) {
                        displayName = displayName.substring(0, displayName.length() - 4);
                    }
                    bookNameMatch = displayName.toLowerCase().contains(searchLower);
                }
                
                // 检查作者匹配
                if (author != null) {
                    authorMatch = author.toLowerCase().contains(searchLower);
                }
                
                // 只要书名或作者有一个匹配就算匹配
                searchMatch = bookNameMatch || authorMatch;
            }
            
            if (categoryMatch && searchMatch) {
                filteredBooks.add(book);
            }
        }
        
        System.out.println("Filtered results: " + filteredBooks.size() + " books");
        bookLists.clear();
        bookLists.addAll(filteredBooks);
        adapter.setBookList(bookLists);
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



//创建菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
//菜单按钮选择电子书
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_select_file) {
            PathSelectFragment selector = PathSelector.build(this, MConstants.BUILD_ACTIVITY)
                    .setRequestCode(635)
                    .setShowSelectStorageBtn(false)
                    .setShowFileTypes("txt")
                    .setSelectFileTypes("txt")
                    .setMaxCount(-1)
                    .setTitlebarMainTitle(new FontBean("选择文件", 18, Color.WHITE, null))
                    .setMorePopupItemListeners(
                            new CommonItemListener(new FontBean("加入书架", 18, Color.WHITE, null)) {
                                @Override
                                public boolean onClick(View v, TextView tv, List<FileBean> selectedFiles, String currentPath, BasePathSelectFragment pathSelectFragment) {

                                    if (selectedFiles.size() == 0) {
                                        Toast.makeText(MainActivity.this, "未选择文件", Toast.LENGTH_SHORT).show();
                                        return false;
                                    }
                                    // 保存待处理的书籍文件
                                    pendingBooks = selectedFiles;
                                    pathSelectFragment.mActivity.finish();
                                    // 弹出对话框输入书籍信息
                                    showBookInfoDialog();
                                    return false;
                                }
                            }
                    )
                    .show();

        }

        return super.onOptionsItemSelected(item);
    }

    private class SaveBookToSqlLiteTask extends AsyncTask<List<BookList>, Void, Integer> {
        private static final int FAIL = 0;
        private static final int SUCCESS = 1;
        private static final int REPEAT = 2;
        private BookList repeatBookList;

        @Override
        protected Integer doInBackground(List<BookList>... params) {
            List<BookList> bookLists = params[0];

            try {
                DataSupport.saveAll(bookLists);
            } catch (Exception e) {
                e.printStackTrace();
                return FAIL;
            }
            return SUCCESS;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            String msg = "";
            switch (result) {
                case FAIL:
                    msg = "由于一些原因添加书本失败";
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
        String[] categories = {"人文社科", "文学经典", "小说", "英文书籍"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
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
                for (FileBean item : pendingBooks) {
                    BookList bookList = new BookList();
                    bookList.setBookname(item.getName());
                    bookList.setBookpath(item.getPath());
                    bookList.setCategory(category);
                    bookList.setAuthor(author);
                    bookList.setDescription(description);
                    bookList.setRating(rating);
                    bookList.setCoverPath(selectedCoverPath);
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

    // 处理图片选择结果
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
    
    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImageSelector();
            } else {
                Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show();
            }
        }
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

}
