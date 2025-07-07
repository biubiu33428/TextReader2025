package com.my.textreader.activity;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.my.textreader.R;
import com.my.textreader.base.BaseActivity;
import com.my.textreader.bean.DownloadableBook;
import com.my.textreader.db.PurchaseRecord;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PaymentActivity extends BaseActivity {

    private Toolbar toolbar;
    private TextView tvBookInfo;
    private TextView tvPrice;
    private ImageView ivQrCode;
    private Button btnPay;
    private ProgressBar progressBar;
    private TextView tvPaymentStatus;

    private DownloadableBook book;
    private boolean isPaymentProcessing = false;

    @Override
    public int getLayoutRes() {
        return R.layout.activity_payment;
    }

    @Override
    protected void initData() {
        toolbar = findViewById(R.id.toolbar);
        tvBookInfo = findViewById(R.id.tv_book_info);
        tvPrice = findViewById(R.id.tv_price);
        ivQrCode = findViewById(R.id.iv_qr_code);
        btnPay = findViewById(R.id.btn_pay);
        progressBar = findViewById(R.id.progress_bar);
        tvPaymentStatus = findViewById(R.id.tv_payment_status);

        setSupportActionBar(toolbar);
        toolbar.setTitle("模拟支付");
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // 获取传递的书籍信息
        book = (DownloadableBook) getIntent().getSerializableExtra("book");
        if (book != null) {
            tvBookInfo.setText(book.getBookName() + "\n作者：" + book.getAuthor());
            tvPrice.setText("支付金额：¥" + book.getPrice());
            
            // 生成二维码
            generateQRCode();
        }
    }

    @Override
    protected void initListener() {
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPaymentProcessing) {
                    finish();
                }
            }
        });

        btnPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPaymentProcessing) {
                    startPayment();
                }
            }
        });
    }

    private void generateQRCode() {
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... voids) {
                try {
                    // 生成支付二维码内容
                    String qrContent = "TextReader://pay?bookId=" + book.getBookId() + "&price=" + book.getPrice();
                    
                    QRCodeWriter writer = new QRCodeWriter();
                    BitMatrix bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 300, 300);
                    
                    int width = bitMatrix.getWidth();
                    int height = bitMatrix.getHeight();
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                    
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                        }
                    }
                    
                    return bitmap;
                } catch (WriterException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    ivQrCode.setImageBitmap(bitmap);
                } else {
                    Toast.makeText(PaymentActivity.this, "二维码生成失败", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    private void startPayment() {
        isPaymentProcessing = true;
        
        // 显示支付进度
        btnPay.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        tvPaymentStatus.setVisibility(View.VISIBLE);
        tvPaymentStatus.setText("正在处理支付...");

        // 模拟支付过程
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                tvPaymentStatus.setText("验证支付信息...");
            }
        }, 1000);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                tvPaymentStatus.setText("连接支付服务器...");
            }
        }, 2000);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                tvPaymentStatus.setText("确认支付结果...");
            }
        }, 3000);

        // 模拟支付成功
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                onPaymentSuccess();
            }
        }, 4000);
    }

    private void onPaymentSuccess() {
        // 保存购买记录
        SavePurchaseTask task = new SavePurchaseTask();
        task.execute();
    }

    private class SavePurchaseTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                PurchaseRecord record = new PurchaseRecord();
                record.setBookId(book.getBookId());
                record.setBookName(book.getBookName());
                record.setPrice(book.getPrice());
                record.setPurchaseTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                record.setDownloaded(false);
                
                return record.save();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            isPaymentProcessing = false;
            progressBar.setVisibility(View.GONE);
            
            if (success) {
                tvPaymentStatus.setText("支付成功！");
                tvPaymentStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                
                Toast.makeText(PaymentActivity.this, "购买成功，现在可以下载了！", Toast.LENGTH_LONG).show();
                
                // 延迟关闭页面
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setResult(RESULT_OK);
                        finish();
                    }
                }, 2000);
                
            } else {
                tvPaymentStatus.setText("支付失败，请重试");
                tvPaymentStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                btnPay.setEnabled(true);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!isPaymentProcessing) {
            super.onBackPressed();
        }
    }
} 