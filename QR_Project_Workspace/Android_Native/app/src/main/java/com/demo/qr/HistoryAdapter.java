package com.demo.qr;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    public interface OnItemDeleteListener {
        void onDelete(QrRecord record);
    }

    private List<QrRecord> records = new ArrayList<>();
    private final boolean showDeleteButton;
    private final int limit;
    private OnItemDeleteListener deleteListener;

    public HistoryAdapter(List<QrRecord> records, boolean showDeleteButton) {
        this(records, showDeleteButton, 0);
    }

    public HistoryAdapter(List<QrRecord> records, boolean showDeleteButton, int limit) {
        if (records != null) {
            this.records = new ArrayList<>(records);
        }
        this.showDeleteButton = showDeleteButton;
        this.limit = limit;
    }

    public void setOnItemDeleteListener(OnItemDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void updateData(List<QrRecord> newRecords) {
        this.records = newRecords != null ? new ArrayList<>(newRecords) : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scan_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QrRecord record = records.get(position);
        Context ctx = holder.itemView.getContext();

        holder.tvTitle.setText(record.getContent());
        holder.tvSubtitle.setText(getFormattedSubtitle(record));

        int iconRes = getIconRes(record.getType(), record.getContent());
        holder.ivIcon.setImageResource(iconRes);

        if (holder.tvBadge != null) {
            holder.tvBadge.setVisibility(View.VISIBLE);
            if (record.isCreated()) {
                holder.tvBadge.setText("Đã tạo");
                holder.tvBadge.setBackgroundResource(R.drawable.bg_badge_created);
                holder.tvBadge.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.qr_mint));
            } else {
                holder.tvBadge.setText("Đã quét");
                holder.tvBadge.setBackgroundResource(R.drawable.bg_badge_scanned);
                holder.tvBadge.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.qr_muted));
            }
        }

        if (showDeleteButton) {
            holder.tvTime.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(record);
                }
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
            holder.tvTime.setVisibility(View.VISIBLE);
            holder.tvTime.setText(formatRelativeTime(record.getTimestamp()));
        }

        holder.itemView.setOnClickListener(v -> handleClick(ctx, record));
    }

    private void handleClick(Context ctx, QrRecord record) {
        String content = record.getContent();
        String type = record.getType() != null ? record.getType().toLowerCase() : "";

        try {
            if (type.equals("url") || content.startsWith("http://") || content.startsWith("https://") || content.contains(".com") || content.contains(".vn")) {
                String url = content;
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
            } else if (type.equals("wifi") || content.startsWith("WIFI:") || content.startsWith("wifi:")) {
                WifiHelper.showWifiDialog(ctx, content);
            } else {
                android.content.ClipboardManager clipboard =
                        (android.content.ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("QR", content);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(ctx, "Đã sao chép: " + content, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(ctx, "Nội dung: " + content, Toast.LENGTH_SHORT).show();
        }
    }

    private String getFormattedSubtitle(QrRecord record) {
        String type = record.getType() != null ? record.getType().toLowerCase() : "text";
        String content = record.getContent() != null ? record.getContent() : "";

        if (type.equals("url") || content.startsWith("http://") || content.startsWith("https://") || content.contains(".com")) {
            return "Liên kết · Liên kết website";
        } else if (type.equals("wifi") || content.startsWith("WIFI:")) {
            return "Wi-Fi · Mạng Wi-Fi";
        } else if (type.equals("payment") || content.contains("đ") || content.contains("vnd") || content.contains("240.000")) {
            return "Thanh toán · Chuyển khoản QR";
        } else if (type.equals("contact") || content.contains("BEGIN:VCARD")) {
            return "Danh bạ · Danh thiếp liên hệ";
        } else {
            return "Văn bản · Nội dung văn bản";
        }
    }

    private int getIconRes(String type, String content) {
        String t = type != null ? type.toLowerCase() : "";
        String c = content != null ? content : "";

        if (t.equals("url") || c.startsWith("http") || c.contains(".com")) {
            return R.drawable.ic_globe;
        } else if (t.equals("wifi") || c.startsWith("WIFI:")) {
            return R.drawable.ic_wifi_signal;
        } else if (t.equals("payment") || c.contains("đ") || c.contains("vnd") || c.contains("240.000")) {
            return R.drawable.ic_credit_card;
        } else if (t.equals("contact") || c.contains("BEGIN:VCARD")) {
            return R.drawable.ic_contact_card;
        } else {
            return R.drawable.ic_text_doc;
        }
    }

    private String formatRelativeTime(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return "Vừa xong >";

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(timestamp);
            if (date == null) return "Vừa xong >";

            long diffMillis = System.currentTimeMillis() - date.getTime();
            long minutes = diffMillis / (1000 * 60);
            long hours = minutes / 60;
            long days = hours / 24;

            if (minutes < 1) {
                return "Vừa xong >";
            } else if (minutes < 60) {
                return minutes + " phút trước >";
            } else if (hours < 24) {
                return hours + " giờ trước >";
            } else if (days == 1) {
                return "Hôm qua >";
            } else {
                return days + " ngày trước >";
            }
        } catch (Exception e) {
            return "Vừa xong >";
        }
    }

    @Override
    public int getItemCount() {
        if (limit > 0) {
            return Math.min(records.size(), limit);
        }
        return records.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, tvTime, tvBadge;
        ImageView ivIcon;
        FrameLayout iconContainer, btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvBadge = itemView.findViewById(R.id.tvBadge);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            iconContainer = itemView.findViewById(R.id.iconContainer);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
