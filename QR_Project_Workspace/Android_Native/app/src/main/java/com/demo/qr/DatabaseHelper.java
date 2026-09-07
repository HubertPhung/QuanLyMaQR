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
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_QR_HISTORY = "qr_history";
    public static final String TABLE_QR_CREATED = "qr_created";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_CONTENT = "content";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createHistoryTable = "CREATE TABLE IF NOT EXISTS " + TABLE_QR_HISTORY + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CONTENT + " TEXT, " +
                COLUMN_TYPE + " TEXT, " +
                COLUMN_TIMESTAMP + " TEXT)";
        db.execSQL(createHistoryTable);

        String createCreatedTable = "CREATE TABLE IF NOT EXISTS " + TABLE_QR_CREATED + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CONTENT + " TEXT, " +
                COLUMN_TYPE + " TEXT, " +
                COLUMN_TIMESTAMP + " TEXT)";
        db.execSQL(createCreatedTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            String createCreatedTable = "CREATE TABLE IF NOT EXISTS " + TABLE_QR_CREATED + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_CONTENT + " TEXT, " +
                    COLUMN_TYPE + " TEXT, " +
                    COLUMN_TIMESTAMP + " TEXT)";
            db.execSQL(createCreatedTable);
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Đảm bảo bảng qr_created luôn tồn tại
        String createCreatedTable = "CREATE TABLE IF NOT EXISTS " + TABLE_QR_CREATED + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CONTENT + " TEXT, " +
                COLUMN_TYPE + " TEXT, " +
                COLUMN_TIMESTAMP + " TEXT)";
        db.execSQL(createCreatedTable);
    }

    // --- QUẢN LÝ MÃ ĐÃ QUÉT (TABLE_QR_HISTORY) ---

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
        return getRecordsFromTable(TABLE_QR_HISTORY);
    }

    public int getScannedCount() {
        return getCountFromTable(TABLE_QR_HISTORY);
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

    // --- QUẢN LÝ MÃ ĐÃ TẠO (TABLE_QR_CREATED) ---

    public void addCreatedRecord(QrRecord record) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CONTENT, record.getContent());
        values.put(COLUMN_TYPE, record.getType());
        values.put(COLUMN_TIMESTAMP, record.getTimestamp());

        db.insert(TABLE_QR_CREATED, null, values);
        db.close();
    }

    public List<QrRecord> getAllCreatedRecords() {
        List<QrRecord> records = getRecordsFromTable(TABLE_QR_CREATED);
        for (QrRecord r : records) {
            r.setCreated(true);
        }
        return records;
    }

    public int getCreatedCount() {
        return getCountFromTable(TABLE_QR_CREATED);
    }

    public void deleteCreatedRecord(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_QR_CREATED, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteAllCreated() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_QR_CREATED);
        db.close();
    }

    // --- TIỆN ÍCH CHUNG ---

    private int getCountFromTable(String tableName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        int count = 0;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + tableName, null);
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            db.close();
        }
        return count;
    }

    private List<QrRecord> getRecordsFromTable(String tableName) {
        List<QrRecord> records = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            String selectQuery = "SELECT * FROM " + tableName + " ORDER BY " + COLUMN_ID + " DESC";
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
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            db.close();
        }

        return records;
    }
}
