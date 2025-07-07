package com.my.textreader.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import com.my.textreader.R;
import com.my.textreader.bean.FileItem;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileListAdapter extends BaseAdapter {
    private Context context;
    private List<FileItem> fileItems;
    private List<FileItem> selectedFiles;
    private LayoutInflater inflater;
    private SimpleDateFormat dateFormat;
    
    public FileListAdapter(Context context, List<FileItem> fileItems, List<FileItem> selectedFiles) {
        this.context = context;
        this.fileItems = fileItems;
        this.selectedFiles = selectedFiles;
        this.inflater = LayoutInflater.from(context);
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    }
    
    @Override
    public int getCount() {
        return fileItems.size();
    }
    
    @Override
    public Object getItem(int position) {
        return fileItems.get(position);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_file_list, parent, false);
            holder = new ViewHolder();
            holder.ivIcon = convertView.findViewById(R.id.iv_icon);
            holder.tvName = convertView.findViewById(R.id.tv_name);
            holder.tvInfo = convertView.findViewById(R.id.tv_info);
            holder.cbSelect = convertView.findViewById(R.id.cb_select);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        FileItem item = fileItems.get(position);
        File file = item.getFile();
        
        // 设置文件名
        holder.tvName.setText(item.getDisplayName());
        
        // 设置文件信息
        if (item.isDirectory()) {
            holder.tvInfo.setText("文件夹");
            holder.cbSelect.setVisibility(View.GONE);
            holder.ivIcon.setImageResource(R.drawable.ic_folder);
        } else {
            // 显示文件大小和修改时间
            long size = file.length();
            String sizeStr = formatFileSize(size);
            String dateStr = dateFormat.format(new Date(file.lastModified()));
            holder.tvInfo.setText(String.format("%s • %s", sizeStr, dateStr));
            
            holder.cbSelect.setVisibility(View.VISIBLE);
            holder.cbSelect.setChecked(selectedFiles.contains(item));
            holder.ivIcon.setImageResource(R.drawable.ic_text_file);
        }
        
        // 设置背景
        if (item.isDirectory()) {
            convertView.setBackgroundResource(R.drawable.file_item_bg);
        } else {
            convertView.setBackgroundResource(selectedFiles.contains(item) ? 
                R.drawable.file_item_selected_bg : R.drawable.file_item_bg);
        }
        
        return convertView;
    }
    
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }
    }
    
    static class ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        TextView tvInfo;
        CheckBox cbSelect;
    }
} 