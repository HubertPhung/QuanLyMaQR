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
    }

    private static HistoryRepository instance;
    private final List<QrRecord> records = new ArrayList<>();
    private final List<OnHistoryChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isLoaded = false;

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

                // Khởi tạo 7 mục lịch sử mẫu ban đầu chuẩn qr-scanner-interface
                dbHelper.addRecord(new QrRecord(0, "vercel.com/dashboard", "url", sdf.format(new Date(now - 15 * 1000L))));
                dbHelper.addRecord(new QrRecord(0, "Coffee_House_5G", "wifi", sdf.format(new Date(now - 2 * 60 * 1000L))));
                dbHelper.addRecord(new QrRecord(0, "240.000đ", "payment", sdf.format(new Date(now - 18 * 60 * 1000L))));
                dbHelper.addRecord(new QrRecord(0, "Hoài Bo", "contact", sdf.format(new Date(now - 24 * 3600 * 1000L))));
                dbHelper.addRecord(new QrRecord(0, "github.com/vercel/next.js", "url", sdf.format(new Date(now - 26 * 3600 * 1000L))));
                dbHelper.addRecord(new QrRecord(0, "Home_Network_2.4G", "wifi", sdf.format(new Date(now - 2 * 24 * 3600 * 1000L))));
                dbHelper.addRecord(new QrRecord(0, "89.000đ", "payment", sdf.format(new Date(now - 3 * 24 * 3600 * 1000L))));

                dbRecords = dbHelper.getAllRecords();
            }

            final List<QrRecord> result = dbRecords;
            mainHandler.post(() -> {
                records.clear();
                records.addAll(result);
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

    private void notifyListeners() {
        List<QrRecord> copy = new ArrayList<>(records);
        for (OnHistoryChangeListener listener : listeners) {
            listener.onHistoryChanged(copy);
        }
    }

    public List<QrRecord> getRecords() {
        return new ArrayList<>(records);
    }

    public int getCount() {
        return records.size();
    }

    public List<QrRecord> getRecent(int limit) {
        if (records.size() <= limit) {
            return new ArrayList<>(records);
        }
        return new ArrayList<>(records.subList(0, limit));
    }
}
