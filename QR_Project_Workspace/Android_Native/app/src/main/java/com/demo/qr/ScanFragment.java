package com.demo.qr;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
    private boolean isProcessing = false;

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

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 10);
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

    private void startCamera() {
        if (getContext() == null) return;
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                BarcodeScanner scanner = BarcodeScanning.getClient();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (isProcessing) {
                        imageProxy.close();
                        return;
                    }
                    processImageProxy(scanner, imageProxy);
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageAnalysis);

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
                    if (!barcodes.isEmpty()) {
                        isProcessing = true;
                        Barcode barcode = barcodes.get(0);
                        String code = barcode.getRawValue();

                        if (getContext() != null) {
                            Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
                            if (vibrator != null) vibrator.vibrate(100);

                            String type = "text";
                            if (code != null && (code.startsWith("http") || code.contains(".com"))) type = "url";
                            else if (code != null && code.startsWith("WIFI:")) type = "wifi";
                            else if (code != null && (code.contains("đ") || code.contains("vnd") || code.contains("240.000"))) type = "payment";
                            else if (code != null && code.contains("BEGIN:VCARD")) type = "contact";

                            String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
                            QrRecord record = new QrRecord(0, code != null ? code : "", type, timestamp);

                            HistoryRepository.getInstance().addRecord(requireContext(), record);

                            new Handler(Looper.getMainLooper()).post(() -> {
                                Toast.makeText(getContext(), "Đã quét: " + code, Toast.LENGTH_SHORT).show();
                            });
                        }

                        new Handler(Looper.getMainLooper()).postDelayed(() -> isProcessing = false, 2000);
                    }
                })
                .addOnCompleteListener(task -> imageProxy.close());
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
                startCamera();
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
        if (scanLineAnimator != null) {
            scanLineAnimator.cancel();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        HistoryRepository.getInstance().removeListener(this);
    }
}
