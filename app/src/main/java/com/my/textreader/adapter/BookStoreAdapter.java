package com.my.textreader.adapter;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.my.textreader.R;
import com.my.textreader.bean.DownloadableBook;
import com.my.textreader.db.PurchaseRecord;

import org.litepal.crud.DataSupport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookStoreAdapter extends BaseAdapter {
    private Context context;
    private List<DownloadableBook> bookList;
    private LayoutInflater inflater;
    private Map<String, Boolean> purchaseStatusCache;

    public BookStoreAdapter(Context context, List<DownloadableBook> bookList) {
        this.context = context;
        this.bookList = bookList;
        this.inflater = LayoutInflater.from(context);
        this.purchaseStatusCache = new HashMap<>();
        
        // 异步加载购买状态
        loadPurchaseStatus();
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
            convertView = inflater.inflate(R.layout.item_book_store, parent, false);
            holder = new ViewHolder();
            holder.tvBookName = convertView.findViewById(R.id.tv_book_name);
            holder.tvAuthor = convertView.findViewById(R.id.tv_author);
            holder.tvDescription = convertView.findViewById(R.id.tv_description);
            holder.tvPrice = convertView.findViewById(R.id.tv_price);
            holder.tvStatus = convertView.findViewById(R.id.tv_status);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        DownloadableBook book = bookList.get(position);
        holder.tvBookName.setText(book.getBookName());
        holder.tvAuthor.setText("作者：" + book.getAuthor());
        holder.tvDescription.setText(book.getDescription());
        holder.tvPrice.setText("¥" + book.getPrice());

        // 显示购买状态
        Boolean isPurchased = purchaseStatusCache.get(book.getBookId());
        if (isPurchased != null && isPurchased) {
            holder.tvStatus.setText("已购买");
            holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            holder.tvStatus.setText("未购买");
            holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
        }

        return convertView;
    }

    private void loadPurchaseStatus() {
        new AsyncTask<Void, Void, Map<String, Boolean>>() {
            @Override
            protected Map<String, Boolean> doInBackground(Void... voids) {
                Map<String, Boolean> statusMap = new HashMap<>();
                for (DownloadableBook book : bookList) {
                    List<PurchaseRecord> records = DataSupport.where("bookId = ?", book.getBookId()).find(PurchaseRecord.class);
                    statusMap.put(book.getBookId(), !records.isEmpty());
                }
                return statusMap;
            }

            @Override
            protected void onPostExecute(Map<String, Boolean> statusMap) {
                purchaseStatusCache = statusMap;
                notifyDataSetChanged();
            }
        }.execute();
    }

    static class ViewHolder {
        TextView tvBookName;
        TextView tvAuthor;
        TextView tvDescription;
        TextView tvPrice;
        TextView tvStatus;
    }
} 