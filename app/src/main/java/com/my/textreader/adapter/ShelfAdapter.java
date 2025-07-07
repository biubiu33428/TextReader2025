package com.my.textreader.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;

import com.my.textreader.Config;
import com.my.textreader.R;
import com.my.textreader.db.BookList;
import com.my.textreader.view.DragGridListener;
import com.my.textreader.view.DragGridView;


import org.litepal.crud.DataSupport;
import org.litepal.exceptions.DataSupportException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShelfAdapter extends BaseAdapter implements DragGridListener {
    private Context mContex;
    private List<BookList> bilist;
    private static LayoutInflater inflater = null;
    private int mHidePosition = -1;
    private Typeface typeface;
    protected List<AsyncTask<Void, Void, Boolean>> myAsyncTasks = new ArrayList<>();
    private int[] firstLocation;
    private Config config;

    public ShelfAdapter(Context context, List<BookList> bilist) {
        this.mContex = context;
        this.bilist = bilist;
        config = Config.getInstance();
        typeface = config.getTypeface();
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        //背景书架的draw需要用到item的高度
        if (bilist.size() < 10) {
            return 10;
        } else {
            return bilist.size();
        }
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return bilist.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(int position, View contentView, ViewGroup arg2) {
        // TODO Auto-generated method stub
        final ViewHolder viewHolder;
        if (contentView == null) {
            contentView = inflater.inflate(R.layout.shelfitem, null);
            viewHolder = new ViewHolder(contentView);
            viewHolder.name.setTypeface(typeface);
            contentView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) contentView.getTag();
        }

        if (bilist.size() > position) {
            //DragGridView  解决复用问题
            if (position == mHidePosition) {
                contentView.setVisibility(View.INVISIBLE);
            } else {
                contentView.setVisibility(View.VISIBLE);
            }
            if (DragGridView.getShowDeleteButton()) {
                viewHolder.deleteItem_IB.setVisibility(View.VISIBLE);
            } else {
                viewHolder.deleteItem_IB.setVisibility(View.INVISIBLE);
            }
            viewHolder.name.setVisibility(View.VISIBLE);
            String fileName = bilist.get(position).getBookname();
            viewHolder.name.setText(fileName);
            
            // 加载封面图片
            loadBookCover(viewHolder.cover, bilist.get(position));
        } else {
            contentView.setVisibility(View.INVISIBLE);
        }
        return contentView;
    }

    static class ViewHolder {
        ImageButton deleteItem_IB;
        TextView name;
        ImageView cover;

        public ViewHolder(View view) {

            deleteItem_IB = view.findViewById(R.id.ib_close);
            name = view.findViewById(R.id.tv_name);
            cover = view.findViewById(R.id.iv_cover);
        }
    }

    /**
     * Drag移动时item交换数据,并在数据库中更新交换后的位置数据
     *
     * @param oldPosition
     * @param newPosition
     */
    @Override
    public void reorderItems(int oldPosition, int newPosition) {

        BookList temp = bilist.get(oldPosition);
        List<BookList> bookLists1 = new ArrayList<>();
        bookLists1 = DataSupport.findAll(BookList.class);

        int tempId = bookLists1.get(newPosition).getId();
        // Log.d("oldposotion is",oldPosition+"");
        // Log.d("newposotion is", newPosition + "");
        if (oldPosition < newPosition) {
            for (int i = oldPosition; i < newPosition; i++) {
                //获得交换前的ID,必须是数据库的真正的ID，如果使用bilist获取id是错误的，因为bilist交换后id是跟着交换的
                List<BookList> bookLists = new ArrayList<>();
                bookLists = DataSupport.findAll(BookList.class);
                int dataBasesId = bookLists.get(i).getId();
                Collections.swap(bilist, i, i + 1);

                updateBookPosition(i, dataBasesId, bilist);

            }
        } else if (oldPosition > newPosition) {
            for (int i = oldPosition; i > newPosition; i--) {
                List<BookList> bookLists = new ArrayList<>();
                bookLists = DataSupport.findAll(BookList.class);
                int dataBasesId = bookLists.get(i).getId();

                Collections.swap(bilist, i, i - 1);

                updateBookPosition(i, dataBasesId, bilist);

            }
        }

        bilist.set(newPosition, temp);
        updateBookPosition(newPosition, tempId, bilist);

    }

    /**
     * 两个item数据交换结束后，把不需要再交换的item更新到数据库中
     *
     * @param position
     * @param bookLists
     */
    public void updateBookPosition(int position, int databaseId, List<BookList> bookLists) {
        BookList bookList = new BookList();
        BookList sourceBook = bookLists.get(position);
        
        bookList.setBookpath(sourceBook.getBookpath());
        bookList.setBookname(sourceBook.getBookname());
        bookList.setContent(sourceBook.getContent());
        bookList.setBegin(sourceBook.getBegin());
        bookList.setCharset(sourceBook.getCharset());
        // 保存新增字段
        bookList.setCategory(sourceBook.getCategory());
        bookList.setAuthor(sourceBook.getAuthor());
        bookList.setDescription(sourceBook.getDescription());
        bookList.setRating(sourceBook.getRating());
        bookList.setCoverPath(sourceBook.getCoverPath());
        //开线程保存改动的数据到数据库
        //使用litepal数据库框架update时每次只能update一个id中的一条信息，如果相同则不更新。
        upDateBookToSqlite3(databaseId, bookList);
    }

    /**
     * 隐藏item
     *
     * @param hidePosition
     */
    @Override
    public void setHideItem(int hidePosition) {
        this.mHidePosition = hidePosition;
        notifyDataSetChanged();
    }

    /**
     * 删除书本
     *
     * @param deletePosition
     */
    @Override
    public void removeItem(int deletePosition) {

        BookList bookToDelete = bilist.get(deletePosition);
        
        // 通过ID删除，这样对于文件型和内容型书籍都适用
        int bookId = bookToDelete.getId();
        DataSupport.delete(BookList.class, bookId);
        
        bilist.remove(deletePosition);
        Log.d("删除的书本是", bookToDelete.getBookname() + " (ID: " + bookId + ")");

        notifyDataSetChanged();

    }

    public void setBookList(List<BookList> bookLists) {
        this.bilist = bookLists;
        notifyDataSetChanged();
    }

    /**
     * Book打开后位置移动到第一位
     *
     * @param openPosition
     */
    @Override
    public void setItemToFirst(int openPosition) {

        List<BookList> bookLists1 = new ArrayList<>();
        bookLists1 = DataSupport.findAll(BookList.class);
        int tempId = bookLists1.get(0).getId();
        BookList temp = bookLists1.get(openPosition);
        // Log.d("setitem adapter ",""+openPosition);
        if (openPosition != 0) {
            for (int i = openPosition; i > 0; i--) {
                List<BookList> bookListsList = new ArrayList<>();
                bookListsList = DataSupport.findAll(BookList.class);
                int dataBasesId = bookListsList.get(i).getId();

                Collections.swap(bookLists1, i, i - 1);
                updateBookPosition(i, dataBasesId, bookLists1);
            }

            bookLists1.set(0, temp);
            updateBookPosition(0, tempId, bookLists1);
            for (int j = 0; j < bookLists1.size(); j++) {
                String bookpath = bookLists1.get(j).getBookpath();
                //  Log.d("移动到第一位",bookpath);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public void nitifyDataRefresh() {
        notifyDataSetChanged();
    }

    public void putAsyncTask(AsyncTask<Void, Void, Boolean> asyncTask) {
        myAsyncTasks.add(asyncTask.execute());
    }

    /**
     * 数据库书本信息更新
     *
     * @param databaseId 要更新的数据库的书本ID
     * @param bookList
     */
    public void upDateBookToSqlite3(final int databaseId, final BookList bookList) {

        putAsyncTask(new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {

            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    bookList.update(databaseId);
                } catch (DataSupportException e) {
                    return false;
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {

                } else {
                    Log.d("保存到数据库结果-->", "失败");
                }
            }
        });
    }
    
    /**
     * 加载书籍封面
     */
    private void loadBookCover(ImageView imageView, BookList book) {
        // 检查是否有自定义封面路径
        String coverPath = book.getCoverPath();
        if (coverPath != null && !coverPath.isEmpty()) {
            File coverFile = new File(coverPath);
            if (coverFile.exists()) {
                try {
                    // 加载自定义封面
                    Bitmap bitmap = BitmapFactory.decodeFile(coverPath);
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                        return;
                    }
                } catch (Exception e) {
                    Log.e("ShelfAdapter", "Error loading cover image: " + e.getMessage());
                }
            }
        }
        
        // 使用默认封面
        imageView.setImageResource(R.mipmap.cover_default_new);
    }

}
