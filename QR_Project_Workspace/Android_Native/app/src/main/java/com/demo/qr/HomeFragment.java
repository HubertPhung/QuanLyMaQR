package com.demo.qr;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HomeFragment extends Fragment implements HistoryRepository.OnHistoryChangeListener {

    private TextView tvScanCount, tvRecentEmpty;
    private RecyclerView rvRecent;
    private HistoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvScanCount = view.findViewById(R.id.tvScanCount);
        tvRecentEmpty = view.findViewById(R.id.tvRecentEmpty);
        rvRecent = view.findViewById(R.id.rvRecent);

        rvRecent.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HistoryAdapter(HistoryRepository.getInstance().getRecent(3), false, 3);
        rvRecent.setAdapter(adapter);

        view.findViewById(R.id.cardQuickScan).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(1);
            }
        });

        view.findViewById(R.id.cardQuickCreate).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(2);
            }
        });

        view.findViewById(R.id.btnViewAllRecent).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(3);
            }
        });

        HistoryRepository.getInstance().addListener(this);
    }

    @Override
    public void onHistoryChanged(List<QrRecord> records) {
        if (tvScanCount != null) {
            tvScanCount.setText(String.valueOf(records.size()));
        }
        if (adapter != null) {
            List<QrRecord> recent = HistoryRepository.getInstance().getRecent(3);
            adapter.updateData(recent);
            if (tvRecentEmpty != null && rvRecent != null) {
                if (recent.isEmpty()) {
                    tvRecentEmpty.setVisibility(View.VISIBLE);
                    rvRecent.setVisibility(View.GONE);
                } else {
                    tvRecentEmpty.setVisibility(View.GONE);
                    rvRecent.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        HistoryRepository.getInstance().removeListener(this);
    }
}
