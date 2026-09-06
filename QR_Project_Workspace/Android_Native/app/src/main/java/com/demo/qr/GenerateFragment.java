package com.demo.qr;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GenerateFragment extends Fragment {

    private enum TabType { TEXT, URL, WIFI }
    private TabType currentTab = TabType.URL;

    private LinearLayout tabText, tabUrl, tabWifi;
    private ImageView ivTabText, ivTabUrl, ivTabWifi;
    private TextView tvTabText, tvTabUrl, tvTabWifi;

    private LinearLayout groupText, groupUrl, groupWifi;
    private EditText etText, etUrl, etWifiSsid, etWifiPass;

    private TextView tvQrEmptyPlaceholder;
    private ImageView ivGeneratedQr;
    private FrameLayout btnDownload;
    private ImageView ivDownloadIcon;
    private TextView tvDownloadText;

    private Bitmap currentQrBitmap = null;
    private final ExecutorService qrExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isSaved = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_generate, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupTabs();
        setupInputWatchers();

        btnDownload.setOnClickListener(v -> handleDownload());
        updatePayloadAndRender();
    }

    private void bindViews(View v) {
        tabText = v.findViewById(R.id.tabText);
        tabUrl = v.findViewById(R.id.tabUrl);
        tabWifi = v.findViewById(R.id.tabWifi);

        ivTabText = v.findViewById(R.id.ivTabText);
        ivTabUrl = v.findViewById(R.id.ivTabUrl);
        ivTabWifi = v.findViewById(R.id.ivTabWifi);

        tvTabText = v.findViewById(R.id.tvTabText);
        tvTabUrl = v.findViewById(R.id.tvTabUrl);
        tvTabWifi = v.findViewById(R.id.tvTabWifi);

        groupText = v.findViewById(R.id.groupText);
        groupUrl = v.findViewById(R.id.groupUrl);
        groupWifi = v.findViewById(R.id.groupWifi);

        etText = v.findViewById(R.id.etText);
        etUrl = v.findViewById(R.id.etUrl);
        etWifiSsid = v.findViewById(R.id.etWifiSsid);
        etWifiPass = v.findViewById(R.id.etWifiPass);

        tvQrEmptyPlaceholder = v.findViewById(R.id.tvQrEmptyPlaceholder);
        ivGeneratedQr = v.findViewById(R.id.ivGeneratedQr);
        btnDownload = v.findViewById(R.id.btnDownload);
        ivDownloadIcon = v.findViewById(R.id.ivDownloadIcon);
        tvDownloadText = v.findViewById(R.id.tvDownloadText);
    }

    private void setupTabs() {
        tabText.setOnClickListener(v -> switchTab(TabType.TEXT));
        tabUrl.setOnClickListener(v -> switchTab(TabType.URL));
        tabWifi.setOnClickListener(v -> switchTab(TabType.WIFI));
    }

    private void switchTab(TabType tab) {
        currentTab = tab;
        int colorMint = ContextCompat.getColor(requireContext(), R.color.qr_mint);
        int colorMintDark = ContextCompat.getColor(requireContext(), R.color.qr_mint_dark);
        int colorMuted = ContextCompat.getColor(requireContext(), R.color.qr_muted);

        // Reset all
        tabText.setBackground(null);
        tabUrl.setBackground(null);
        tabWifi.setBackground(null);

        ivTabText.setColorFilter(colorMuted);
        ivTabUrl.setColorFilter(colorMuted);
        ivTabWifi.setColorFilter(colorMuted);

        tvTabText.setTextColor(colorMuted);
        tvTabUrl.setTextColor(colorMuted);
        tvTabWifi.setTextColor(colorMuted);

        groupText.setVisibility(View.GONE);
        groupUrl.setVisibility(View.GONE);
        groupWifi.setVisibility(View.GONE);

        // Highlight active
        switch (tab) {
            case TEXT:
                tabText.setBackgroundResource(R.drawable.bg_tab_selected);
                ivTabText.setColorFilter(colorMintDark);
                tvTabText.setTextColor(colorMintDark);
                groupText.setVisibility(View.VISIBLE);
                break;
            case URL:
                tabUrl.setBackgroundResource(R.drawable.bg_tab_selected);
                ivTabUrl.setColorFilter(colorMintDark);
                tvTabUrl.setTextColor(colorMintDark);
                groupUrl.setVisibility(View.VISIBLE);
                break;
            case WIFI:
                tabWifi.setBackgroundResource(R.drawable.bg_tab_selected);
                ivTabWifi.setColorFilter(colorMintDark);
                tvTabWifi.setTextColor(colorMintDark);
                groupWifi.setVisibility(View.VISIBLE);
                break;
        }

        updatePayloadAndRender();
    }

    private void setupInputWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePayloadAndRender();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        etText.addTextChangedListener(watcher);
        etUrl.addTextChangedListener(watcher);
        etWifiSsid.addTextChangedListener(watcher);
        etWifiPass.addTextChangedListener(watcher);
    }

    private String getPayload() {
        switch (currentTab) {
            case TEXT:
                return etText.getText().toString().trim();
            case URL:
                return etUrl.getText().toString().trim();
            case WIFI:
                String ssid = etWifiSsid.getText().toString().trim();
                String pass = etWifiPass.getText().toString().trim();
                if (ssid.isEmpty()) return "";
                return "WIFI:T:WPA;S:" + ssid + ";P:" + pass + ";;";
            default:
                return "";
        }
    }

    private void updatePayloadAndRender() {
        String payload = getPayload();

        if (payload.isEmpty()) {
            tvQrEmptyPlaceholder.setVisibility(View.VISIBLE);
            ivGeneratedQr.setVisibility(View.GONE);
            btnDownload.setAlpha(0.4f);
            btnDownload.setEnabled(false);
            currentQrBitmap = null;
        } else {
            btnDownload.setAlpha(1.0f);
            btnDownload.setEnabled(true);
            renderQrCode(payload);
        }
    }

    private void renderQrCode(String payload) {
        qrExecutor.execute(() -> {
            try {
                QRCodeWriter writer = new QRCodeWriter();
                BitMatrix bitMatrix = writer.encode(payload, BarcodeFormat.QR_CODE, 600, 600);
                int width = bitMatrix.getWidth();
                int height = bitMatrix.getHeight();
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

                int colorDark = Color.parseColor("#0B1220");
                int colorLight = Color.WHITE;

                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        bitmap.setPixel(x, y, bitMatrix.get(x, y) ? colorDark : colorLight);
                    }
                }

                mainHandler.post(() -> {
                    if (isAdded() && getPayload().equals(payload)) {
                        currentQrBitmap = bitmap;
                        ivGeneratedQr.setImageBitmap(bitmap);
                        ivGeneratedQr.setVisibility(View.VISIBLE);
                        tvQrEmptyPlaceholder.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void handleDownload() {
        if (currentQrBitmap == null || isSaved) return;

        try {
            String filename = "QR_" + System.currentTimeMillis() + ".png";
            boolean success = false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && getContext() != null) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QRScanner");

                Uri uri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
                        if (out != null) {
                            currentQrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                            success = true;
                        }
                    }
                }
            } else if (getContext() != null) {
                File dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                if (dir != null) {
                    File file = new File(dir, filename);
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        currentQrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        success = true;
                    }
                }
            }

            if (success) {
                isSaved = true;
                ivDownloadIcon.setImageResource(R.drawable.ic_check);
                tvDownloadText.setText("Đã lưu mã QR");
                Toast.makeText(getContext(), "Đã lưu mã QR thành công!", Toast.LENGTH_SHORT).show();

                mainHandler.postDelayed(() -> {
                    if (isAdded()) {
                        isSaved = false;
                        ivDownloadIcon.setImageResource(R.drawable.ic_download);
                        tvDownloadText.setText("Tải mã QR");
                    }
                }, 1800);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Lỗi khi lưu ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        qrExecutor.shutdown();
    }
}
