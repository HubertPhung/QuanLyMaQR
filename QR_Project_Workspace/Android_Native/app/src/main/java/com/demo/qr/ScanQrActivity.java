package com.demo.qr;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanQrActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private RecyclerView rvHistory;
    private HistoryAdapter historyAdapter;
    private DatabaseHelper dbHelper;
    private ExecutorService cameraExecutor;
    private ExecutorService dbExecutor;
    private ProcessCameraProvider cameraProvider;
    private ValueAnimator animator;

    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                return windowInsets;
            });
        }

        dbHelper = new DatabaseHelper(this);
        cameraExecutor = Executors.newSingleThreadExecutor();
        dbExecutor = Executors.newSingleThreadExecutor();

        viewFinder = findViewById(R.id.viewFinder);
        rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        setupScannerAnimation();
        loadHistoryFromDb();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 10);
        }

        findViewById(R.id.btnGallery).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 100);
        });

        View btnViewAll = findViewById(R.id.btnViewAll);
        if (btnViewAll != null) {
            btnViewAll.setOnClickListener(v -> {
                Intent intent = new Intent(ScanQrActivity.this, HistoryActivity.class);
                startActivity(intent);
            });
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

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
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
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

                        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                        if (vibrator != null) vibrator.vibrate(100);

                        String lower = code != null ? code.toLowerCase() : "";
                        String type = "text";
                        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.contains(".com") || lower.contains(".vn")) {
                            type = "url";
                        } else if (code != null && code.toUpperCase().startsWith("WIFI:")) {
                            type = "wifi";
                        }

                        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
                        QrRecord record = new QrRecord(0, code, type, timestamp);

                        dbExecutor.execute(() -> {
                            dbHelper.addRecord(record);
                            loadHistoryFromDb();
                        });

                        final String finalType = type;
                        final String finalCode = code;
                        runOnUiThread(() -> {
                            if ("url".equals(finalType)) {
                                String url = finalCode;
                                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                    url = "https://" + url;
                                }
                                try {
                                    Toast.makeText(ScanQrActivity.this, "Đang mở liên kết...", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                } catch (Exception e) {
                                    Toast.makeText(ScanQrActivity.this, "Đã quét: " + finalCode, Toast.LENGTH_SHORT).show();
                                }
                            } else if ("wifi".equals(finalType)) {
                                WifiHelper.showWifiDialog(ScanQrActivity.this, finalCode);
                                WifiHelper.connectToWifi(ScanQrActivity.this, WifiHelper.parseWifiQr(finalCode));
                            } else {
                                Toast.makeText(ScanQrActivity.this, "Đã quét: " + finalCode, Toast.LENGTH_SHORT).show();
                            }
                        });

                        new Handler(Looper.getMainLooper()).postDelayed(() -> isProcessing = false, 2500);
                    }
                })
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void loadHistoryFromDb() {
        dbExecutor.execute(() -> {
            java.util.List<QrRecord> records = dbHelper.getAllRecords();
            if (records.isEmpty()) {
                long now = System.currentTimeMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                dbHelper.addRecord(new QrRecord(0, "Hoài Bo", "contact", sdf.format(new Date(now - 86400000L))));
                dbHelper.addRecord(new QrRecord(0, "₫ 240.000", "payment", sdf.format(new Date(now - 18 * 60 * 1000L))));
                dbHelper.addRecord(new QrRecord(0, "Coffee_House_5G", "wifi", sdf.format(new Date(now - 2 * 60 * 1000L))));
                dbHelper.addRecord(new QrRecord(0, "vercel.com/dashboard", "url", sdf.format(new Date(now - 15000L))));
                records = dbHelper.getAllRecords();
            }
            final java.util.List<QrRecord> finalRecords = records;
            runOnUiThread(() -> {
                if (historyAdapter == null) {
                    historyAdapter = new HistoryAdapter(finalRecords, true);
                    rvHistory.setAdapter(historyAdapter);
                } else {
                    historyAdapter.updateData(finalRecords);
                }
            });
        });
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void setupScannerAnimation() {
        View scanLine = findViewById(R.id.scanLine);
        if (scanLine == null) return;
        animator = ValueAnimator.ofFloat(0f, 780f);
        animator.setDuration(2200);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animation -> {
            scanLine.setTranslationY((float) animation.getAnimatedValue());
        });
        animator.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraProvider != null) {
            try {
                cameraProvider.unbindAll();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (animator != null) {
            animator.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (allPermissionsGranted()) {
            startCamera();
        }
        if (animator != null) {
            animator.resume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) {
            try {
                cameraProvider.unbindAll();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (animator != null) {
            animator.cancel();
        }
        cameraExecutor.shutdown();
        dbExecutor.shutdown();
    }
}
