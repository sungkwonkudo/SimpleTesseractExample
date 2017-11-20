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
    public String getFirstKanjiResult(String kanji) {

        final SQLiteDatabase db = getWritableDatabase();
        final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        final String[] sqlSelect = {"gloss.value"};

        final String sqlWhere = "k_ele.value = " + "\"" + kanji + "\"";
        final String sqlTables = "entry LEFT JOIN k_ele ON entry.id = k_ele.fk "
                + "LEFT JOIN sense ON entry.id = sense.fk "
                + "LEFT JOIN gloss ON sense.id = gloss.fk";



        qb.setTables(sqlTables);
        Cursor cursor = qb.query(db, sqlSelect, sqlWhere, null, null, null, null);

        if(cursor.moveToFirst()){
            kanjiResult = cursor.getString(cursor.getColumnIndex("value"));
        }
        return kanjiResult;
    }

    // Threaded method
    class Query implements Callable<String> {
        String kanji;
        public Query(String input) {
            this.kanji = input;
        }

        @Override
        public String call() throws Exception {
            final SQLiteDatabase db = getWritableDatabase();
            final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

            final String[] sqlSelect = {"gloss.value"};

            final String sqlWhere = "k_ele.value = " + "\"" + kanji + "\"";
            final String sqlTables = "entry LEFT JOIN k_ele ON entry.id = k_ele.fk "
                    + "LEFT JOIN sense ON entry.id = sense.fk "
                    + "LEFT JOIN gloss ON sense.id = gloss.fk";



            qb.setTables(sqlTables);
            Cursor cursor = qb.query(db, sqlSelect, sqlWhere, null, null, null, null);

            if(cursor.moveToFirst()){
                kanjiResult = cursor.getString(cursor.getColumnIndex("value"));
            }
            return kanjiResult;
        }
    }

}

