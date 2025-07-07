package com.my.textreader.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.my.textreader.R;
import com.my.textreader.db.BookList;

import java.util.List;

public class ExportBookAdapter extends BaseAdapter {
    private Context context;
    private List<BookList> bookList;
    private LayoutInflater inflater;

    public ExportBookAdapter(Context context, List<BookList> bookList) {
        this.context = context;
        this.bookList = bookList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return bookList.size();
    }

    @Override
    public Object getItem(int position) {
        return bookList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_export_book, parent, false);
            holder = new ViewHolder();
            holder.tvBookName = convertView.findViewById(R.id.tv_book_name);
            holder.tvBookType = convertView.findViewById(R.id.tv_book_type);
            holder.tvBookSize = convertView.findViewById(R.id.tv_book_size);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        BookList book = bookList.get(position);
        holder.tvBookName.setText(book.getBookname());
        
        // 判断书籍类型
        if (book.getContent() != null && !book.getContent().isEmpty()) {
            holder.tvBookType.setText("内容型书籍");
            holder.tvBookSize.setText("大小：" + formatSize(book.getContent().length()));
        } else {
            holder.tvBookType.setText("文件型书籍");
            holder.tvBookSize.setText("文件路径：" + book.getBookpath());
        }

        return convertView;
    }

    private String formatSize(int size) {
        if (size < 1024) {
            return size + " 字符";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }
    }

    static class ViewHolder {
        TextView tvBookName;
        TextView tvBookType;
        TextView tvBookSize;
    }
} 