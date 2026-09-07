package com.demo.qr;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryFragment extends Fragment implements HistoryRepository.OnHistoryChangeListener {

    private TextView tvHistoryCount;
    private View btnDeleteAll;
    private LinearLayout layoutEmpty;
    private RecyclerView rvFullHistory;
    private HistoryAdapter adapter;

    private TextView tabAll, tabScanned, tabCreated;
    private TextView tvEmptyTitle, tvEmptySubtitle;
    private ImageView ivEmptyIcon;

    // 0: Tất cả, 1: Đã quét, 2: Đã tạo
    private int currentFilter = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvHistoryCount = view.findViewById(R.id.tvHistoryCount);
        btnDeleteAll = view.findViewById(R.id.btnDeleteAll);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        rvFullHistory = view.findViewById(R.id.rvFullHistory);

        tabAll = view.findViewById(R.id.tabAll);
        tabScanned = view.findViewById(R.id.tabScanned);
        tabCreated = view.findViewById(R.id.tabCreated);

        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = view.findViewById(R.id.tvEmptySubtitle);
        ivEmptyIcon = view.findViewById(R.id.ivEmptyIcon);

        rvFullHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HistoryAdapter(getCurrentList(), true, 0);
        adapter.setOnItemDeleteListener(record -> {
            if (getContext() != null) {
                HistoryRepository.getInstance().deleteItem(requireContext(), record.getId(), record.isCreated());
                Toast.makeText(getContext(), "Đã xóa mục lịch sử", Toast.LENGTH_SHORT).show();
            }
        });
        rvFullHistory.setAdapter(adapter);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(0);
            }
        });

        tabAll.setOnClickListener(v -> selectFilter(0));
        tabScanned.setOnClickListener(v -> selectFilter(1));
        tabCreated.setOnClickListener(v -> selectFilter(2));

        btnDeleteAll.setOnClickListener(v -> handleConfirmDeleteAll());

        HistoryRepository.getInstance().addListener(this);
        updateListDisplay();
    }

    private void selectFilter(int filter) {
        currentFilter = filter;
        if (getContext() == null) return;

        int colorMintDark = ContextCompat.getColor(requireContext(), R.color.qr_mint_dark);
        int colorMuted = ContextCompat.getColor(requireContext(), R.color.qr_muted);

        tabAll.setBackground(null);
        tabScanned.setBackground(null);
        tabCreated.setBackground(null);

        tabAll.setTextColor(colorMuted);
        tabScanned.setTextColor(colorMuted);
        tabCreated.setTextColor(colorMuted);

        if (filter == 1) {
            tabScanned.setBackgroundResource(R.drawable.bg_tab_selected);
            tabScanned.setTextColor(colorMintDark);
        } else if (filter == 2) {
            tabCreated.setBackgroundResource(R.drawable.bg_tab_selected);
            tabCreated.setTextColor(colorMintDark);
        } else {
            tabAll.setBackgroundResource(R.drawable.bg_tab_selected);
            tabAll.setTextColor(colorMintDark);
        }

        updateListDisplay();
    }

    private List<QrRecord> getCurrentList() {
        if (currentFilter == 1) {
            return HistoryRepository.getInstance().getScannedRecords();
        } else if (currentFilter == 2) {
            return HistoryRepository.getInstance().getCreatedRecords();
        }
        return HistoryRepository.getInstance().getAllRecords();
    }

    private void updateListDisplay() {
        List<QrRecord> currentList = getCurrentList();
        int total = HistoryRepository.getInstance().getTotalCount();
        int scanned = HistoryRepository.getInstance().getScannedCount();
        int created = HistoryRepository.getInstance().getCreatedCount();

        if (tabAll != null) tabAll.setText("Tất cả (" + total + ")");
        if (tabScanned != null) tabScanned.setText("Đã quét (" + scanned + ")");
        if (tabCreated != null) tabCreated.setText("Đã tạo (" + created + ")");

        if (tvHistoryCount != null) {
            if (currentFilter == 1) {
                tvHistoryCount.setText(currentList.size() + " mã đã quét");
            } else if (currentFilter == 2) {
                tvHistoryCount.setText(currentList.size() + " mã đã tạo");
            } else {
                tvHistoryCount.setText(currentList.size() + " mã trong lịch sử");
            }
        }

        if (btnDeleteAll != null) {
            btnDeleteAll.setVisibility(currentList.isEmpty() ? View.GONE : View.VISIBLE);
        }

        if (layoutEmpty != null && rvFullHistory != null) {
            if (currentList.isEmpty()) {
                layoutEmpty.setVisibility(View.VISIBLE);
                rvFullHistory.setVisibility(View.GONE);

                if (tvEmptyTitle != null && tvEmptySubtitle != null) {
                    if (currentFilter == 1) {
                        tvEmptyTitle.setText("Chưa có mã đã quét");
                        tvEmptySubtitle.setText("Các mã QR bạn quét qua Camera sẽ xuất hiện tại đây.");
                        if (ivEmptyIcon != null) ivEmptyIcon.setImageResource(R.drawable.ic_nav_scan);
                    } else if (currentFilter == 2) {
                        tvEmptyTitle.setText("Chưa có mã đã tạo");
                        tvEmptySubtitle.setText("Các mã QR bạn tự tạo sẽ xuất hiện tại đây.");
                        if (ivEmptyIcon != null) ivEmptyIcon.setImageResource(R.drawable.ic_nav_generate);
                    } else {
                        tvEmptyTitle.setText("Chưa có lịch sử");
                        tvEmptySubtitle.setText("Các mã QR bạn quét hoặc tạo sẽ xuất hiện tại đây.");
                        if (ivEmptyIcon != null) ivEmptyIcon.setImageResource(R.drawable.ic_nav_history);
                    }
                }
            } else {
                layoutEmpty.setVisibility(View.GONE);
                rvFullHistory.setVisibility(View.VISIBLE);
            }
        }

        if (adapter != null) {
            adapter.updateData(currentList);
        }
    }

    private void handleConfirmDeleteAll() {
        if (getContext() == null) return;

        String title = "Xóa toàn bộ lịch sử?";
        String message = "Tất cả các mã đã quét và đã tạo sẽ bị xóa vĩnh viễn.";

        if (currentFilter == 1) {
            title = "Xóa lịch sử quét?";
            message = "Tất cả các mã đã quét sẽ bị xóa vĩnh viễn.";
        } else if (currentFilter == 2) {
            title = "Xóa lịch sử tạo mã?";
            message = "Tất cả các mã bạn đã tạo sẽ bị xóa vĩnh viễn.";
        }

        new AlertDialog.Builder(requireContext(), R.style.AlertDialogDark)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    if (currentFilter == 1) {
                        HistoryRepository.getInstance().clearAllScanned(requireContext());
                    } else if (currentFilter == 2) {
                        HistoryRepository.getInstance().clearAllCreated(requireContext());
                    } else {
                        HistoryRepository.getInstance().clearAll(requireContext());
                    }
                    Toast.makeText(getContext(), "Đã xóa lịch sử thành công", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onHistoryChanged(List<QrRecord> records) {
        updateListDisplay();
    }

    @Override
    public void onStatsChanged(int scannedCount, int createdCount) {
        updateListDisplay();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        HistoryRepository.getInstance().removeListener(this);
    }
}
