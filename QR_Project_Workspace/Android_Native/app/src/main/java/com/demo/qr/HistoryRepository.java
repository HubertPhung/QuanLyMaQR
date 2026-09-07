package com.demo.qr;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryRepository {

    public interface OnHistoryChangeListener {
        void onHistoryChanged(List<QrRecord> records);
        default void onStatsChanged(int scannedCount, int createdCount) {}
    }

    private static HistoryRepository instance;
    private final List<QrRecord> scannedRecords = new ArrayList<>();
    private final List<QrRecord> createdRecords = new ArrayList<>();
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
            listener.onHistoryChanged(getAllRecords());
            listener.onStatsChanged(scannedRecords.size(), createdRecords.size());
        }
    }

    public void removeListener(OnHistoryChangeListener listener) {
        listeners.remove(listener);
    }

    public void load(Context context) {
        executor.execute(() -> {
            DatabaseHelper dbHelper = new DatabaseHelper(context.getApplicationContext());
            List<QrRecord> dbScanned = dbHelper.getAllRecords();
            List<QrRecord> dbCreated = dbHelper.getAllCreatedRecords();

            mainHandler.post(() -> {
                scannedRecords.clear();
                scannedRecords.addAll(dbScanned);
                createdRecords.clear();
                createdRecords.addAll(dbCreated);
                isLoaded = true;
                notifyListeners();
            });
        });
    }

    public void deleteItem(Context context, int id, boolean isCreated) {
        executor.execute(() -> {
            DatabaseHelper dbHelper = new DatabaseHelper(context.getApplicationContext());
            if (isCreated) {
                dbHelper.deleteCreatedRecord(id);
            } else {
                dbHelper.deleteRecord(id);
            }
            mainHandler.post(() -> {
                if (isCreated) {
                    for (int i = 0; i < createdRecords.size(); i++) {
                        if (createdRecords.get(i).getId() == id) {
                            createdRecords.remove(i);
                            break;
                        }
                    }
                } else {
                    for (int i = 0; i < scannedRecords.size(); i++) {
                        if (scannedRecords.get(i).getId() == id) {
                            scannedRecords.remove(i);
                            break;
                        }
                    }
                }
                notifyListeners();
            });
        });
    }

    public void deleteItem(Context context, int id) {
        deleteItem(context, id, false);
    }

    public void clearAllScanned(Context context) {
        executor.execute(() -> {
            DatabaseHelper dbHelper = new DatabaseHelper(context.getApplicationContext());
            dbHelper.deleteAll();
            mainHandler.post(() -> {
                scannedRecords.clear();
                notifyListeners();
            });
        });
    }

    public void clearAllCreated(Context context) {
        executor.execute(() -> {
            DatabaseHelper dbHelper = new DatabaseHelper(context.getApplicationContext());
            dbHelper.deleteAllCreated();
            mainHandler.post(() -> {
                createdRecords.clear();
                notifyListeners();
            });
        });
    }

    public void clearAll(Context context) {
        executor.execute(() -> {
            DatabaseHelper dbHelper = new DatabaseHelper(context.getApplicationContext());
            dbHelper.deleteAll();
            dbHelper.deleteAllCreated();
            mainHandler.post(() -> {
                scannedRecords.clear();
                createdRecords.clear();
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
                scannedRecords.clear();
                scannedRecords.addAll(dbRecords);
                notifyListeners();
            });
        });
    }

    public void addCreatedRecord(Context context, QrRecord record) {
        executor.execute(() -> {
            DatabaseHelper dbHelper = new DatabaseHelper(context.getApplicationContext());
            dbHelper.addCreatedRecord(record);
            List<QrRecord> dbCreated = dbHelper.getAllCreatedRecords();
            mainHandler.post(() -> {
                createdRecords.clear();
                createdRecords.addAll(dbCreated);
                notifyListeners();
            });
        });
    }

    private void notifyListeners() {
        List<QrRecord> all = getAllRecords();
        int scanned = scannedRecords.size();
        int created = createdRecords.size();
        for (OnHistoryChangeListener listener : listeners) {
            listener.onHistoryChanged(all);
            listener.onStatsChanged(scanned, created);
        }
    }

    public List<QrRecord> getAllRecords() {
        List<QrRecord> all = new ArrayList<>();
        all.addAll(scannedRecords);
        all.addAll(createdRecords);
        Collections.sort(all, (a, b) -> {
            String tA = a.getTimestamp() != null ? a.getTimestamp() : "";
            String tB = b.getTimestamp() != null ? b.getTimestamp() : "";
            return tB.compareTo(tA);
        });
        return all;
    }

    public List<QrRecord> getScannedRecords() {
        return new ArrayList<>(scannedRecords);
    }

    public List<QrRecord> getCreatedRecords() {
        return new ArrayList<>(createdRecords);
    }

    public List<QrRecord> getRecords() {
        return getAllRecords();
    }

    public int getCount() {
        return scannedRecords.size();
    }

    public int getScannedCount() {
        return scannedRecords.size();
    }

    public int getCreatedCount() {
        return createdRecords.size();
    }

    public int getTotalCount() {
        return scannedRecords.size() + createdRecords.size();
    }

    public List<QrRecord> getRecent(int limit) {
        List<QrRecord> all = getAllRecords();
        if (all.size() <= limit) {
            return new ArrayList<>(all);
        }
        return new ArrayList<>(all.subList(0, limit));
    }
}
