package com.sgt.primoz.alivev1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Created by Primoz on 4.11.2014.
 */
public class DBSettingRepo {
    private SQLiteDatabase db;
    private DBHelper dbHelper;
    private String[] allColumns = {"Id","Username","Password","Url"};

    public DBSettingRepo(Context context){
        dbHelper = new DBHelper(context);
    }

    public void open() throws SQLException{
        db = dbHelper.getWritableDatabase();
    }

    public void close() throws SQLException{
        dbHelper.close();
    }

    public DBSetting getSetting(){
        Cursor c = db.query(DBHelper.SETTING_TABLE_NAME,allColumns,null,null,null,null,null);
        c.moveToFirst();
        DBSetting s = null;
        if(c.getCount()>0) {
            s = cursorToSetting(c);
        }
        c.close();
        return s;
    }

    public DBSetting find(String Id){
        Cursor c = db.query(DBHelper.SETTING_TABLE_NAME,allColumns,"Id = '" + Id + "'", null,null,null,null);
        c.moveToFirst();
        DBSetting s = null;
        if(c.getCount()>0){
            s = cursorToSetting(c);
        }
        c.close();
        return s;
    }

    private DBSetting cursorToSetting(Cursor c){
        DBSetting s = new DBSetting();
        s.Id = c.getString(0);
        s.Username = c.getString(1);
        s.Password = c.getString(2);
        s.Url = c.getString(3);
        return s;
    }

    public void save(DBSetting setting) throws SQLException {
        DBSetting existing = find(setting.Id);
        ContentValues values = new ContentValues();
        values.put("Id",setting.Id);
        values.put("Username",setting.Username);
        values.put("Password",setting.Password);
        values.put("Url",setting.Url);

        if(existing==null){
            open();
            db.insert(DBHelper.SETTING_TABLE_NAME,null,values);
            close();
        }
        else{
            open();
            db.update(DBHelper.SETTING_TABLE_NAME,values,"Id = '" + existing.Id +"'", null);
            close();
        }
    }
}
