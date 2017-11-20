package com.imperialsoupgmail.tesseractexample;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.telecom.Call;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.util.concurrent.Callable;
import java.util.regex.Pattern;

/**
 * Created by Sungkwon Kudo on 10/22/17.
 */

// Database in assets/databases folder
public class Database extends SQLiteAssetHelper {

    private static final String DATABASE_NAME = "JMdict.sqlite";
    private static final int DATABASE_VERSION = 1;

    // For threading
    private static String kanjiResult;

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Non-threaded method
    public SQLiteDatabase getDatabase() {

        final SQLiteDatabase db = getWritableDatabase();
        return db;
    }



}

