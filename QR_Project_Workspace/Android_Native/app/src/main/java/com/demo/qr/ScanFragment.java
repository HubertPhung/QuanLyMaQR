package com.demo.qr;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanFragment extends Fragment implements HistoryRepository.OnHistoryChangeListener {

    private PreviewView viewFinder;
    private RecyclerView rvHistory;
    private TextView tvScanEmpty;
    private HistoryAdapter historyAdapter;
    private ExecutorService cameraExecutor;
    private ValueAnimator scanLineAnimator;
    private ProcessCameraProvider cameraProvider;
    private boolean isProcessing = false;
    private boolean isCameraActive = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cameraExecutor = Executors.newSingleThreadExecutor();
        viewFinder = view.findViewById(R.id.viewFinder);
        rvHistory = view.findViewById(R.id.rvHistory);
        tvScanEmpty = view.findViewById(R.id.tvScanEmpty);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        historyAdapter = new HistoryAdapter(HistoryRepository.getInstance().getRecent(4), false, 4);
        rvHistory.setAdapter(historyAdapter);

        setupScannerAnimation(view.findViewById(R.id.scanLine));

        // Chỉ bật camera nếu fragment không bị ẩn
        if (!isHidden()) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 10);
            }
        }

        view.findViewById(R.id.btnGallery).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Chọn ảnh từ thư viện", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.btnViewAll).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(3);
            }
        });

        HistoryRepository.getInstance().addListener(this);
    }

    /**
     * TẮT/BẬT CAMERA KHI CHUYỂN TAB TRONG MAIN ACTIVITY
     */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            stopCamera();
            if (scanLineAnimator != null) {
                scanLineAnimator.pause();
            }
        } else {
            if (allPermissionsGranted()) {
                startCamera();
            }
            if (scanLineAnimator != null) {
                scanLineAnimator.resume();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopCamera();
        if (scanLineAnimator != null) {
            scanLineAnimator.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden() && allPermissionsGranted()) {
            startCamera();
            if (scanLineAnimator != null) {
                scanLineAnimator.resume();
            }
        }
    }

    private void stopCamera() {
        isCameraActive = false;
        if (cameraProvider != null) {
            try {
                cameraProvider.unbindAll();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void startCamera() {
        if (getContext() == null || isHidden()) return;
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                if (isHidden() || !isAdded()) {
                    cameraProvider.unbindAll();
                    return;
                }

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                BarcodeScanner scanner = BarcodeScanning.getClient();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (isProcessing || !isCameraActive) {
                        imageProxy.close();
                        return;
                    }
                    processImageProxy(scanner, imageProxy);
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageAnalysis);
                isCameraActive = true;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @androidx.camera.core.ExperimentalGetImage
    private void processImageProxy(BarcodeScanner scanner, ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }
        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (!barcodes.isEmpty() && !isProcessing) {
                        isProcessing = true;
                        Barcode barcode = barcodes.get(0);
                        String code = barcode.getRawValue();

                        if (getContext() != null && code != null && !code.trim().isEmpty()) {
                            Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
                            if (vibrator != null) vibrator.vibrate(100);

                            String lower = code.toLowerCase();
                            String type = "text";
                            if (lower.startsWith("http://") || lower.startsWith("https://") || lower.contains(".com") || lower.contains(".vn") || lower.contains(".net") || lower.contains(".org")) {
                                type = "url";
                            } else if (code.toUpperCase().startsWith("WIFI:")) {
                                type = "wifi";
                            } else if (lower.contains("đ") || lower.contains("vnd") || lower.contains("240.000")) {
                                type = "payment";
                            } else if (code.contains("BEGIN:VCARD")) {
                                type = "contact";
                            }

                            String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
                            QrRecord record = new QrRecord(0, code, type, timestamp);
                            HistoryRepository.getInstance().addRecord(requireContext(), record);

                            final String finalType = type;
                            final String finalCode = code;

                            new Handler(Looper.getMainLooper()).post(() -> {
                                handleScanResultAction(finalType, finalCode);
                            });
                        }

                        new Handler(Looper.getMainLooper()).postDelayed(() -> isProcessing = false, 2500);
                    }
                })
                .addOnCompleteListener(task -> imageProxy.close());
    }

    /**
     * XỬ LÝ HÀNH ĐỘNG TỰ ĐỘNG SAU KHI QUÉT:
     * 1. URL: Tự động mở link trong trình duyệt
     * 2. Wi-Fi: Hiển thị dialog thông tin & tự động kết nối Wi-Fi
     */
    private void handleScanResultAction(String type, String code) {
        if (!isAdded() || getContext() == null) return;

        if ("url".equals(type)) {
            String url = code;
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            try {
                Toast.makeText(getContext(), "Đang mở liên kết...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Không thể mở liên kết: " + code, Toast.LENGTH_SHORT).show();
            }
        } else if ("wifi".equals(type)) {
            WifiHelper.WifiModel wifiModel = WifiHelper.parseWifiQr(code);
            Toast.makeText(getContext(), "Đã phát hiện mạng Wi-Fi: " + wifiModel.ssid, Toast.LENGTH_SHORT).show();
            // Tự động hiển thị popup thông tin & thử kết nối
            WifiHelper.showWifiDialog(requireContext(), code);
            WifiHelper.connectToWifi(requireContext(), wifiModel);
        } else {
            Toast.makeText(getContext(), "Đã quét: " + code, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupScannerAnimation(View scanLine) {
        if (scanLine == null) return;
        scanLineAnimator = ValueAnimator.ofFloat(0f, 680f);
        scanLineAnimator.setDuration(2200);
        scanLineAnimator.setRepeatMode(ValueAnimator.REVERSE);
        scanLineAnimator.setRepeatCount(ValueAnimator.INFINITE);
        scanLineAnimator.addUpdateListener(animation -> {
            scanLine.setTranslationY((float) animation.getAnimatedValue());
        });
        scanLineAnimator.start();
    }

    private boolean allPermissionsGranted() {
        if (getContext() == null) return false;
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 10) {
            if (allPermissionsGranted()) {
                if (!isHidden()) {
                    startCamera();
                }
            } else {
                Toast.makeText(getContext(), "Không có quyền Camera để quét mã", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onHistoryChanged(List<QrRecord> records) {
        if (historyAdapter != null) {
            List<QrRecord> recent = HistoryRepository.getInstance().getRecent(4);
            historyAdapter.updateData(recent);
            if (tvScanEmpty != null && rvHistory != null) {
                if (recent.isEmpty()) {
                    tvScanEmpty.setVisibility(View.VISIBLE);
                    rvHistory.setVisibility(View.GONE);
                } else {
                    tvScanEmpty.setVisibility(View.GONE);
                    rvHistory.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopCamera();
        if (scanLineAnimator != null) {
            scanLineAnimator.cancel();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        HistoryRepository.getInstance().removeListener(this);
    }
}
