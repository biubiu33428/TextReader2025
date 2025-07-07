package com.my.textreader.db;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.io.FileOutputStream;
import android.util.Log;
import java.io.IOException;




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

    // 插入默认用户
    db.execSQL("INSERT INTO user (name, password) VALUES ('u1', '123')");
    db.execSQL("INSERT INTO user (name, password) VALUES ('u2', '123')");
    db.execSQL("INSERT INTO user (name, password) VALUES ('u3', '123')");
    db.execSQL("INSERT INTO user (name, password) VALUES ('biubiu', '123')");
    db.execSQL("INSERT INTO user (name, password) VALUES ('down', '1234')");
    db.execSQL("INSERT INTO user (name, password) VALUES ('yeahyeah', '123')");

    // 审计日志表
    db.execSQL("CREATE TABLE IF NOT EXISTS audit_log (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "user TEXT, " +
            "action TEXT, " +
            "table_name TEXT, " +
            "sql_text TEXT, " +
            "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");

}

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void exportAuditLogToCSV(Context context) {
        openDatabase();
        Cursor cursor = db.query("audit_log", null, null, null, null, null, "timestamp DESC");
        StringBuilder csv = new StringBuilder();
        csv.append("user,action,table,sql_text,timestamp\n");

        while (cursor.moveToNext()) {
            csv.append(cursor.getString(cursor.getColumnIndex("user"))).append(",");
            csv.append(cursor.getString(cursor.getColumnIndex("action"))).append(",");
            csv.append(cursor.getString(cursor.getColumnIndex("table_name"))).append(",");
            csv.append(cursor.getString(cursor.getColumnIndex("sql_text")).replace(",", " ")).append(",");
            csv.append(cursor.getString(cursor.getColumnIndex("timestamp"))).append("\n");
        }

        cursor.close();

        try {
            File file = new File(context.getExternalFilesDir(null), "audit_log.csv");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(csv.toString().getBytes());
            fos.close();
            Log.d("Export", "导出成功: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // 插入审计日志
    public void insertAuditLog(String user, String action, String table, String sqlText) {
        openDatabase();
        ContentValues values = new ContentValues();
        values.put("user", user);
        values.put("action", action);
        values.put("table_name", table);
        values.put("sql_text", sqlText);
        db.insert("audit_log", null, values);
    }

    // 插入用户
    public long insterUser(String name, String pwd) {
        openDatabase();
        ContentValues value = new ContentValues();
        value.put("name", name);
        value.put("password", pwd);
        long result = db.insert("user", null, value);

        // 插入审计日志
        String sqlText = "INSERT INTO user (name, password) VALUES ('" + name + "', '" + pwd + "')";
        insertAuditLog(name, "INSERT", "user", sqlText);

        return result;
    }



    /**
     * 匹配账号密码
     */
    public boolean isLogin(String name, String psw) {
        openDatabase();
        Cursor cursor = db.query("user", null, "name=? and password=?", new String[]{name, psw}, null, null, null);
        if (cursor.moveToNext()) {
            insertAuditLog(name, "LOGIN_SUCCESS", "user", "User '" + name + "' login success.");
            return true;
        } else {
            insertAuditLog(name, "LOGIN_FAIL", "user", "User '" + name + "' login failed.");
            return false;
        }
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
