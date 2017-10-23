package com.imperialsoupgmail.tesseractexample;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

/**
 * Created by Sungkwon Kudo on 10/22/17.
 */

// Database in assets/databases folder
public class Database extends SQLiteAssetHelper {

    private static final String DATABASE_NAME = "kanjidb.sqlite";
    private static final int DATABASE_VERSION = 1;

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public String getFirstKanjiResult(String kanji) {


        SQLiteDatabase db = getWritableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        String[] sqlSelect = {"meaning"};
        String sqlWhere = "kanji = " + "kanji";
        String sqlTables = "edict";
        Cursor cursor = qb.query(db, sqlSelect, sqlWhere, null, null, null, null);

        String result = "";
        if(cursor.moveToFirst()){
             result = cursor.getString(cursor.getColumnIndex("meaning"));
        };
        return result;
    }
}

