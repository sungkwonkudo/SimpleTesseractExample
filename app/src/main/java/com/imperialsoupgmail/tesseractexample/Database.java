package com.imperialsoupgmail.tesseractexample;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

/**
 * Created by Sungkwon Kudo on 10/22/17.
 *
 *
 */

// Database in assets/databases folder
class Database extends SQLiteAssetHelper {

    private static final String DATABASE_NAME = "JMdict.sqlite";
    private static final int DATABASE_VERSION = 1;

    Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Non-threaded method
    SQLiteDatabase getDatabase() {
        return getWritableDatabase();
    }



}

