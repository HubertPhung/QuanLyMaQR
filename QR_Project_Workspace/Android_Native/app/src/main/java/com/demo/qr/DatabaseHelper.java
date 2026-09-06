package com.demo.qr;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "qr_database.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_QR_HISTORY = "qr_history";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_CONTENT = "content";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_QR_HISTORY + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CONTENT + " TEXT, " +
                COLUMN_TYPE + " TEXT, " +
                COLUMN_TIMESTAMP + " TEXT)";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_QR_HISTORY);
        onCreate(db);
    }

    public void addRecord(QrRecord record) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CONTENT, record.getContent());
        values.put(COLUMN_TYPE, record.getType());
        values.put(COLUMN_TIMESTAMP, record.getTimestamp());

        db.insert(TABLE_QR_HISTORY, null, values);
        db.close();
    }

    public List<QrRecord> getAllRecords() {
        List<QrRecord> records = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            String selectQuery = "SELECT * FROM " + TABLE_QR_HISTORY + " ORDER BY " + COLUMN_ID + " DESC";
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    QrRecord record = new QrRecord();
                    int idIndex = cursor.getColumnIndex(COLUMN_ID);
                    int contentIndex = cursor.getColumnIndex(COLUMN_CONTENT);
                    int typeIndex = cursor.getColumnIndex(COLUMN_TYPE);
                    int timestampIndex = cursor.getColumnIndex(COLUMN_TIMESTAMP);

                    if (idIndex != -1) record.setId(cursor.getInt(idIndex));
                    if (contentIndex != -1) record.setContent(cursor.getString(contentIndex));
                    if (typeIndex != -1) record.setType(cursor.getString(typeIndex));
                    if (timestampIndex != -1) record.setTimestamp(cursor.getString(timestampIndex));

                    records.add(record);
                } while (cursor.moveToNext());
            }
        } finally {
            // Đóng Cursor ở khối finally để tránh memory leak
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            db.close();
        }

        return records;
    }

    public void deleteRecord(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_QR_HISTORY, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_QR_HISTORY);
        db.close();
    }
}
