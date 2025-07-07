package com.my.textreader.db;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;

//操作数据库相关 注册登录
public class DbHelper extends SQLiteOpenHelper {
//数据库名字
    public final static String DB_NAME = "user.db";
//    版本
    public final static int VERSION = 1;
    private static DbHelper instance = null;
    private SQLiteDatabase db;

    public DbHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }
//获取单例
    public static DbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DbHelper(context);
        }
        return instance;
    }

    private void openDatabase() {
        if (db == null) {
            db = getWritableDatabase();
        }
    }
//创建表
    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql2 = "create table user(id INTEGER PRIMARY KEY AUTOINCREMENT, name varchar(50) not null, password varchar(50) not null)";
        db.execSQL(sql2);


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

//插入用户 账号密码
    public long insterUser(String name, String pwd) {
        openDatabase();
        ContentValues value = new ContentValues();
        value.put("name", name);
        value.put("password", pwd);
        return db.insert("user", null, value);
    }

    /**
     * 匹配账号密码
     */
    public boolean isLogin(String name, String psw) {
        openDatabase();
        Cursor cursor = db.query("user", null, "name=? and password=?", new String[]{name, psw}, null, null, null);
        while (cursor.moveToNext()) {
            return true;
        }
        return false;
    }

    /**
     * 查用户 登录用户
     */
    public ArrayList<HashMap<String, Object>> selectUserinfo(String userinfo) {
        openDatabase();
        Cursor cursor = db.query("user", null, "name=?", new String[]{userinfo}, null, null, null);
        ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
        while (cursor.moveToNext()) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("name", cursor.getString(cursor.getColumnIndex("name")));
            map.put("password", cursor.getString(cursor.getColumnIndex("password")));
            list.add(map);
        }
        return list;
    }



}
