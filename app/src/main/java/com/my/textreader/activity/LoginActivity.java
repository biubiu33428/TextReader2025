package com.my.textreader.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.my.textreader.R;
import com.my.textreader.db.DbHelper;

import java.util.ArrayList;
import java.util.HashMap;



public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

//定义变量

//    用户名输入框
    private EditText etxtUser;
//    密码输入框
    private EditText etxtPwd;
//    登录按钮
    private Button fbtnLogin;
//    注册按钮
    private TextView mTvzhuce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initView();
    }
//查找控件
    public void initView() {

        etxtUser = (EditText) findViewById(R.id.etxt_user);
        etxtPwd = (EditText) findViewById(R.id.etxt_pwd);
        fbtnLogin = (Button) findViewById(R.id.fbtn_login);
        mTvzhuce = (TextView) findViewById(R.id.tvzhuce);
        mTvzhuce.setOnClickListener(this);
        fbtnLogin.setOnClickListener(this);

    }

//登录注册按钮点击事件
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.fbtn_login:
                login();
                break;
            case R.id.tvzhuce:
                register();
                break;
        }
    }
//跳转注册页面
    private void register() {
        startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
    }
//登录逻辑
    private void login() {
//        获取用户输入的用户名密码
        String username = etxtUser.getText().toString().trim();
        String password = etxtPwd.getText().toString().trim();
//校验用户输入的数据
        if (username.length() == 0 || password.length() == 0) {
            Toast.makeText(this, "账号或密码不能为空", Toast.LENGTH_SHORT).show();

            return;
        }
//        调用数据库操作方法判断用户是否被注册
        ArrayList<HashMap<String, Object>> hashMaps = DbHelper.getInstance(LoginActivity.this).selectUserinfo(username);
//       判断验证结果注册结果
        if (hashMaps.size() == 0) {
            Toast.makeText(this, "没有注册，请先注册", Toast.LENGTH_SHORT).show();
            return;
        }

//调用数据库方法 判断是否登录成功
        boolean login = DbHelper.getInstance(LoginActivity.this).isLogin(username, password);
//        登录成功跳转主界面
        if (login) {
            Toast.makeText(this, "成功", Toast.LENGTH_SHORT).show();

            // 登录成功后保存用户名
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            prefs.edit().putString("username", username).apply();

            // 登录成功后导出审计日志
            DbHelper.getInstance(LoginActivity.this).exportAuditLogToCSV(LoginActivity.this);
            Toast.makeText(this, "审计日志已导出", Toast.LENGTH_SHORT).show();


            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("name", username);
            startActivity(intent);
            finish();

        } else {
//            登录失败提示
            Toast.makeText(this, "账号或密码错误", Toast.LENGTH_SHORT).show();

        }

    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
