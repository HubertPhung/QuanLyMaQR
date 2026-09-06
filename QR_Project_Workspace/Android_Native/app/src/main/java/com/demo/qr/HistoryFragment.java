package com.demo.qr;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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

        rvFullHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HistoryAdapter(HistoryRepository.getInstance().getRecords(), true, 0);
        adapter.setOnItemDeleteListener(record -> {
            if (getContext() != null) {
                HistoryRepository.getInstance().deleteItem(requireContext(), record.getId());
                Toast.makeText(getContext(), "Đã xóa mục lịch sử", Toast.LENGTH_SHORT).show();
            }
        });
        rvFullHistory.setAdapter(adapter);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(0);
            }
        });

        btnDeleteAll.setOnClickListener(v -> {
            if (getContext() == null) return;
            new AlertDialog.Builder(requireContext(), R.style.AlertDialogDark)
                    .setTitle("Xóa toàn bộ lịch sử?")
                    .setMessage("Tất cả các mã đã quét sẽ bị xóa vĩnh viễn.")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        HistoryRepository.getInstance().clearAll(requireContext());
                        Toast.makeText(getContext(), "Đã xóa toàn bộ lịch sử", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        HistoryRepository.getInstance().addListener(this);
    }

    @Override
    public void onHistoryChanged(List<QrRecord> records) {
        if (tvHistoryCount != null) {
            tvHistoryCount.setText(records.size() + " mã đã quét");
        }

        if (btnDeleteAll != null) {
            btnDeleteAll.setVisibility(records.isEmpty() ? View.GONE : View.VISIBLE);
        }

        if (layoutEmpty != null && rvFullHistory != null) {
            if (records.isEmpty()) {
                layoutEmpty.setVisibility(View.VISIBLE);
                rvFullHistory.setVisibility(View.GONE);
            } else {
                layoutEmpty.setVisibility(View.GONE);
                rvFullHistory.setVisibility(View.VISIBLE);
            }
        }

        if (adapter != null) {
            adapter.updateData(records);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        HistoryRepository.getInstance().removeListener(this);
    }
}
