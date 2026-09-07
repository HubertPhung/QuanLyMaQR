package com.demo.qr;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryRepository {

    public interface OnHistoryChangeListener {
        void onHistoryChanged(List<QrRecord> records);
        default void onStatsChanged(int scannedCount, int createdCount) {}
    }

    private static HistoryRepository instance;
    private final List<QrRecord> records = new ArrayList<>();
    private final List<OnHistoryChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isLoaded = false;
    private int createdCount = 0;

    private HistoryRepository() {}

    public static synchronized HistoryRepository getInstance() {
        if (instance == null) {
            instance = new HistoryRepository();
        }
        return instance;
    }

    public void addListener(OnHistoryChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
        if (isLoaded) {
            listener.onHistoryChanged(new ArrayList<>(records));
            listener.onStatsChanged(records.size(), createdCount);
        }
    }

    public void removeListener(OnHistoryChangeListener listener) {
        listeners.remove(listener);
    }

    public void load(Context context) {
        executor.execute(() -> {
            DatabaseHelper dbHelper = new DatabaseHelper(context.getApplicationContext());
            List<QrRecord> dbRecords = dbHelper.getAllRecords();

            if (dbRecords.isEmpty()) {
                long now = System.currentTimeMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

                // Khởi tạo 7 mục lịch sử mẫu ban đầu chuẩn giao diện
                dbHelper.addRecord(new QrRecord(0, "vercel.com/dashboard", "url", sdf.format(new Date(now - 15 * 1000L))));
                dbHelper.addRecord(new QrRecord(0, "WIFI:T:WPA;S:Coffee_House_5G;P:Coffee@2026;;", "wifi", sdf.format(new Date(now - 2 * 60 * 1000L))));
                dbHelper.addRecord(new QrRecord(0, "240.000đ", "payment", sdf.format(new Date(now - 18 * 60 * 1000L))));
                dbHelper.addRecord(new QrRecord(0, "Hoài Bo", "contact", sdf.format(new Date(now - 24 * 3600 * 1000L))));
                dbHelper.addRecord(new QrRecord(0, "github.com/vercel/next.js", "url", sdf.format(new Date(now - 26 * 3600 * 1000L))));
                dbHelper.addRecord(new QrRecord(0, "WIFI:T:WPA;S:Home_Network_2.4G;P:Password123;;", "wifi", sdf.format(new Date(now - 2 * 24 * 3600 * 1000L))));
                dbHelper.addRecord(new QrRecord(0, "89.000đ", "payment", sdf.format(new Date(now - 3 * 24 * 3600 * 1000L))));

                dbRecords = dbHelper.getAllRecords();
            }

            // Kiểm tra và khởi tạo mã tạo mẫu nếu chưa có
            int cCount = dbHelper.getCreatedCount();
            if (cCount == 0) {
                long now = System.currentTimeMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                dbHelper.addCreatedRecord(new QrRecord(0, "https://github.com/HubertPhung", "url", sdf.format(new Date(now - 3600 * 1000L))));
                dbHelper.addCreatedRecord(new QrRecord(0, "WIFI:T:WPA;S:Studio_Guest;P:guest2026;;", "wifi", sdf.format(new Date(now - 7200 * 1000L))));
                dbHelper.addCreatedRecord(new QrRecord(0, "Demo Project QR 2026", "text", sdf.format(new Date(now - 10800 * 1000L))));
                cCount = dbHelper.getCreatedCount();
            }

            final List<QrRecord> result = dbRecords;
            final int finalCreatedCount = cCount;
            mainHandler.post(() -> {
                records.clear();
                records.addAll(result);
                createdCount = finalCreatedCount;
                isLoaded = true;
                notifyListeners();
            });
        });
    }

    public void deleteItem(Context context, int id) {
        executor.execute(() -> {
            DatabaseHelper dbHelper = new DatabaseHelper(context.getApplicationContext());
            dbHelper.deleteRecord(id);
            mainHandler.post(() -> {
                for (int i = 0; i < records.size(); i++) {
                    if (records.get(i).getId() == id) {
                        records.remove(i);
                        break;
                    }
                }
                notifyListeners();
            });
        });
    }

    public void clearAll(Context context) {
        executor.execute(() -> {
            DatabaseHelper dbHelper = new DatabaseHelper(context.getApplicationContext());
            dbHelper.deleteAll();
            mainHandler.post(() -> {
                records.clear();
                notifyListeners();
            });
        });
    }

    public void addRecord(Context context, QrRecord record) {
        executor.execute(() -> {
            DatabaseHelper dbHelper = new DatabaseHelper(context.getApplicationContext());
            dbHelper.addRecord(record);
            List<QrRecord> dbRecords = dbHelper.getAllRecords();
            mainHandler.post(() -> {
                records.clear();
                records.addAll(dbRecords);
                notifyListeners();
            });
        });
    }

    public void addCreatedRecord(Context context, QrRecord record) {
        executor.execute(() -> {
            DatabaseHelper dbHelper = new DatabaseHelper(context.getApplicationContext());
            dbHelper.addCreatedRecord(record);
            int newCount = dbHelper.getCreatedCount();
            mainHandler.post(() -> {
                createdCount = newCount;
                notifyListeners();
            });
        });
    }

    private void notifyListeners() {
        List<QrRecord> copy = new ArrayList<>(records);
        int scanned = copy.size();
        int created = createdCount;
        for (OnHistoryChangeListener listener : listeners) {
            listener.onHistoryChanged(copy);
            listener.onStatsChanged(scanned, created);
        }
    }

    public List<QrRecord> getRecords() {
        return new ArrayList<>(records);
    }

    public int getCount() {
        return records.size();
    }

    public int getScannedCount() {
        return records.size();
    }

    public int getCreatedCount() {
        return createdCount;
    }

    public List<QrRecord> getRecent(int limit) {
        if (records.size() <= limit) {
            return new ArrayList<>(records);
        }
        return new ArrayList<>(records.subList(0, limit));
    }
}
