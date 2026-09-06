package com.demo.qr;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvFullHistory;
    private LinearLayout layoutEmpty;
    private HistoryAdapter adapter;
    private DatabaseHelper dbHelper;
    private ExecutorService executor;
    private List<QrRecord> allRecords = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                return windowInsets;
            });
        }

        dbHelper = new DatabaseHelper(this);
        executor = Executors.newSingleThreadExecutor();

        rvFullHistory = findViewById(R.id.rvFullHistory);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        EditText etSearch = findViewById(R.id.etSearch);
        FrameLayout iconContainer = findViewById(R.id.iconHistoryContainer);
        View btnDeleteAll = findViewById(R.id.btnDeleteAll);

        // Style icon container
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(Color.parseColor("#00F0FF"));
        bg.setCornerRadius(36);
        iconContainer.setBackground(bg);

        // Style search bar
        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setShape(GradientDrawable.RECTANGLE);
        searchBg.setColor(Color.parseColor("#1C1C1E"));
        searchBg.setCornerRadius(48);
        searchBg.setStroke(2, Color.parseColor("#33FFFFFF"));
        etSearch.setBackground(searchBg);
        etSearch.setPadding(48, 0, 48, 0);

        rvFullHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(allRecords, false);
        rvFullHistory.setAdapter(adapter);

        // Search filter
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Xóa tất cả
        btnDeleteAll.setOnClickListener(v -> {
            new AlertDialog.Builder(this, R.style.AlertDialogDark)
                .setTitle("Xóa toàn bộ lịch sử?")
                .setMessage("Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (d, w) -> {
                    executor.execute(() -> {
                        dbHelper.deleteAll();
                        runOnUiThread(() -> {
                            allRecords.clear();
                            adapter.updateData(allRecords);
                            showEmpty(true);
                            Toast.makeText(this, "Đã xóa toàn bộ lịch sử", Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
        });

        loadAllRecords();
    }

    private void loadAllRecords() {
        executor.execute(() -> {
            List<QrRecord> records = dbHelper.getAllRecords();
            runOnUiThread(() -> {
                allRecords.clear();
                allRecords.addAll(records);
                adapter.updateData(allRecords);
                showEmpty(allRecords.isEmpty());
            });
        });
    }

    private void filterList(String query) {
        List<QrRecord> filtered = new ArrayList<>();
        for (QrRecord r : allRecords) {
            if (r.getContent().toLowerCase().contains(query.toLowerCase())
                    || r.getType().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(r);
            }
        }
        adapter.updateData(filtered);
        showEmpty(filtered.isEmpty());
    }

    private void showEmpty(boolean empty) {
        layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvFullHistory.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
