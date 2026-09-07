package com.demo.qr;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

public class WifiHelper {

    public static class WifiModel {
        public String ssid = "";
        public String password = "";
        public String securityType = "WPA";
        public boolean isHidden = false;

        public boolean isNoPassword() {
            return "nopass".equalsIgnoreCase(securityType) || password == null || password.isEmpty();
        }
    }

    public static WifiModel parseWifiQr(String raw) {
        WifiModel model = new WifiModel();
        if (raw == null || raw.trim().isEmpty()) {
            return model;
        }

        String cleaned = raw.trim();
        if (cleaned.toUpperCase().startsWith("WIFI:")) {
            // Chuẩn dạng: WIFI:T:WPA;S:MySSID;P:mypass;H:false;;
            String body = cleaned.substring(5);
            String[] tokens = body.split(";");
            for (String token : tokens) {
                if (token.startsWith("S:") || token.startsWith("s:")) {
                    model.ssid = token.substring(2);
                } else if (token.startsWith("P:") || token.startsWith("p:")) {
                    model.password = token.substring(2);
                } else if (token.startsWith("T:") || token.startsWith("t:")) {
                    model.securityType = token.substring(2);
                } else if (token.startsWith("H:") || token.startsWith("h:")) {
                    model.isHidden = Boolean.parseBoolean(token.substring(2));
                }
            }
        } else {
            // Không theo chuẩn WIFI:, xem cả chuỗi là SSID
            model.ssid = cleaned;
        }

        if (model.ssid.isEmpty()) {
            model.ssid = "Mạng Wi-Fi";
        }
        return model;
    }

    public static void connectToWifi(Context context, WifiModel model) {
        if (context == null || model == null || model.ssid.isEmpty()) return;

        Handler mainHandler = new Handler(Looper.getMainLooper());

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (API 29+) sử dụng WifiNetworkSpecifier
                WifiNetworkSpecifier.Builder specifierBuilder = new WifiNetworkSpecifier.Builder()
                        .setSsid(model.ssid);

                if (!model.isNoPassword() && model.password != null && !model.password.isEmpty()) {
                    specifierBuilder.setWpa2Passphrase(model.password);
                }

                NetworkRequest networkRequest = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .setNetworkSpecifier(specifierBuilder.build())
                        .build();

                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    Toast.makeText(context, "Đang gửi yêu cầu kết nối tới: " + model.ssid, Toast.LENGTH_SHORT).show();
                    cm.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(@NonNull Network network) {
                            cm.bindProcessToNetwork(network);
                            mainHandler.post(() -> Toast.makeText(context, "Đã kết nối thành công với Wi-Fi: " + model.ssid, Toast.LENGTH_LONG).show());
                        }

                        @Override
                        public void onUnavailable() {
                            mainHandler.post(() -> {
                                Toast.makeText(context, "Không thể kết nối trực tiếp. Đang mở cài đặt Wi-Fi...", Toast.LENGTH_SHORT).show();
                                openWifiSettings(context);
                            });
                        }
                    });
                }
            } else {
                // Android 9 trở xuống
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wifiManager != null) {
                    if (!wifiManager.isWifiEnabled()) {
                        wifiManager.setWifiEnabled(true);
                    }

                    WifiConfiguration conf = new WifiConfiguration();
                    conf.SSID = "\"" + model.ssid + "\"";

                    if (model.isNoPassword()) {
                        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    } else {
                        conf.preSharedKey = "\"" + model.password + "\"";
                    }

                    int netId = wifiManager.addNetwork(conf);
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(netId, true);
                    wifiManager.reconnect();

                    Toast.makeText(context, "Đang kết nối tới: " + model.ssid, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(context, "Lỗi kết nối Wi-Fi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            openWifiSettings(context);
        }
    }

    public static void openWifiSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showWifiDialog(Context context, String rawQr) {
        WifiModel model = parseWifiQr(rawQr);

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_wifi_info, null);
        TextView tvSsid = dialogView.findViewById(R.id.tvWifiSsid);
        EditText etPass = dialogView.findViewById(R.id.etWifiPassword);
        TextView tvSec = dialogView.findViewById(R.id.tvWifiSecurity);
        ImageView ivTogglePass = dialogView.findViewById(R.id.ivTogglePassword);
        View btnCopy = dialogView.findViewById(R.id.btnCopyPassword);
        View btnConnect = dialogView.findViewById(R.id.btnConnectWifi);

        tvSsid.setText(model.ssid);
        tvSec.setText("Bảo mật: " + (model.securityType.isEmpty() ? "WPA/WPA2" : model.securityType));

        if (model.isNoPassword()) {
            etPass.setText("Không có mật khẩu (Mạng mở)");
            btnCopy.setVisibility(View.GONE);
            ivTogglePass.setVisibility(View.GONE);
        } else {
            etPass.setText(model.password);
        }

        final boolean[] isPasswordVisible = {false};
        ivTogglePass.setOnClickListener(v -> {
            isPasswordVisible[0] = !isPasswordVisible[0];
            if (isPasswordVisible[0]) {
                etPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivTogglePass.setImageResource(R.drawable.ic_check);
            } else {
                etPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivTogglePass.setImageResource(R.drawable.ic_lock);
            }
            etPass.setSelection(etPass.getText().length());
        });

        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("Wifi Password", model.password));
                Toast.makeText(context, "Đã sao chép mật khẩu Wi-Fi!", Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(context, R.style.AlertDialogDark)
                .setView(dialogView)
                .create();

        btnConnect.setOnClickListener(v -> {
            dialog.dismiss();
            connectToWifi(context, model);
        });

        View btnClose = dialogView.findViewById(R.id.btnCloseWifi);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
}
