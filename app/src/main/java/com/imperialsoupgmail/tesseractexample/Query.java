package com.imperialsoupgmail.tesseractexample;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import java.util.concurrent.Callable;

/**
 * Created by wyki on 11/19/17.
 */

public class Query {

    String kanji;
    String kResult;
    SQLiteDatabase db;

    public Query(String input, SQLiteDatabase database) {
        this.kanji = input;
        this.db = database;
    }


    public String call(){
        final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        final String[] sqlSelect = {"gloss.value"};

        final String sqlWhere = "k_ele.value = " + "\"" + kanji + "\"";
        final String sqlTables = "entry INNER JOIN k_ele ON entry.id = k_ele.fk "
                + "INNER JOIN sense ON entry.id = sense.fk "
                + "INNER JOIN gloss ON sense.id = gloss.fk";



        qb.setTables(sqlTables);
        Cursor cursor = qb.query(db, sqlSelect, sqlWhere, null, null, null, null);

        if(cursor.moveToFirst()){
            kResult = cursor.getString(cursor.getColumnIndex("value"));
        }
        if(kResult == null) return " ";
        return kResult;
    }
}

