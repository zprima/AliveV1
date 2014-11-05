package com.sgt.primoz.alivev1;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Primoz on 4.11.2014.
 */
public class DBHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "alivedb";

    public static final String SETTING_TABLE_NAME = "setting";
    private static final String SETTING_TABLE_CREATE =
            "CREATE TABLE " + SETTING_TABLE_NAME + " (" +
                    "Id TEXT primary key," +
                    "Username TEXT, " +
                    "Password TEXT," +
                    "Url TEXT);";

    DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SETTING_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {

    }


}
