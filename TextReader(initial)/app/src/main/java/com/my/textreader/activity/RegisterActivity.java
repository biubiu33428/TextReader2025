package com.my.textreader.activity;


import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.my.textreader.R;
import com.my.textreader.db.DbHelper;

import java.util.ArrayList;
import java.util.HashMap;



public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

//定义变量
    private EditText etxtUser;

    private EditText etxtPwd;

    private Button fbtnLogin;
    private EditText etpswag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        //设置ActionBar返回箭头 和 标题
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("注册");
        //设置ActionBar背景颜色
        ColorDrawable colorDrawable = new ColorDrawable(Color.parseColor("#7E431B"));
        getSupportActionBar().setBackgroundDrawable(colorDrawable);
        initView();
    }
//找控件
    public void initView() {

        etxtUser = (EditText) findViewById(R.id.etxt_user);
        etxtPwd = (EditText) findViewById(R.id.etxt_pwd);
        fbtnLogin = (Button) findViewById(R.id.fbtn_login);
        etpswag = (EditText) findViewById(R.id.et_pwda);
        fbtnLogin.setOnClickListener(this);
    }
//注册按钮点击事件
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.fbtn_login:
                register();
                break;
        }
    }
//注册逻辑
    private void register() {
//校验数据
        if (etxtUser.getText().toString().trim().length() == 0 || etpswag.getText().toString().trim().length() == 0 || etxtPwd.getText().toString().trim().length() == 0) {
            Toast.makeText(this, "账号或密码不能为空", Toast.LENGTH_SHORT).show();

            return;
        }
//        判断账号是否被注册
        ArrayList<HashMap<String, Object>> hashMaps = DbHelper.getInstance(this).selectUserinfo(etxtUser.getText().toString().trim());
        if (hashMaps.size() != 0) {
            Toast.makeText(this, "已被注册", Toast.LENGTH_SHORT).show();
            return;
        }
//密码是否一致
        if (etxtPwd.getText().toString().trim().equals(etpswag.getText().toString().trim()) == false) {
            Toast.makeText(this, "密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

//进行注册
        DbHelper.getInstance(this).insterUser(etxtUser.getText().toString().trim(), etpswag.getText().toString().trim());
        Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show();

        finish();
    }

    //返回箭头点击事件 销毁退出页面
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (android.R.id.home == item.getItemId()) {
            finish();
        }
        return true;
    }
}
