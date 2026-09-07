import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:wifi_iot/wifi_iot.dart';
import '../theme/app_colors.dart';

class WifiModel {
  final String ssid;
  final String password;
  final String securityType;
  final bool isHidden;

  WifiModel({
    required this.ssid,
    required this.password,
    required this.securityType,
    this.isHidden = false,
  });

  bool get isNoPassword =>
      securityType.toLowerCase() == 'nopass' || password.trim().isEmpty;
}

class WifiHelper {
  static WifiModel parseWifi(String raw) {
    String cleaned = raw.trim();
    String ssid = "";
    String password = "";
    String securityType = "WPA";
    bool isHidden = false;

    if (cleaned.toUpperCase().startsWith("WIFI:")) {
      String body = cleaned.substring(5);
      List<String> tokens = body.split(";");
      for (String token in tokens) {
        if (token.startsWith("S:") || token.startsWith("s:")) {
          ssid = token.substring(2);
        } else if (token.startsWith("P:") || token.startsWith("p:")) {
          password = token.substring(2);
        } else if (token.startsWith("T:") || token.startsWith("t:")) {
          securityType = token.substring(2);
        } else if (token.startsWith("H:") || token.startsWith("h:")) {
          isHidden = token.substring(2).toLowerCase() == 'true';
        }
      }
    } else {
      ssid = cleaned;
    }

    if (ssid.isEmpty) {
      ssid = "Mạng Wi-Fi";
    }

    return WifiModel(
      ssid: ssid,
      password: password,
      securityType: securityType,
      isHidden: isHidden,
    );
  }

  static Future<void> connectToWifi(BuildContext context, WifiModel model) async {
    try {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          backgroundColor: AppColors.card,
          content: Text("Đang kết nối tới ${model.ssid}…", style: const TextStyle(color: AppColors.foreground)),
          duration: const Duration(seconds: 2),
          behavior: SnackBarBehavior.floating,
        ),
      );

      NetworkSecurity security = NetworkSecurity.WPA;
      if (model.securityType.toUpperCase() == 'WEP') {
        security = NetworkSecurity.WEP;
      } else if (model.isNoPassword) {
        security = NetworkSecurity.NONE;
      }

      bool connected = await WiFiForIoTPlugin.connect(
        model.ssid,
        password: model.isNoPassword ? null : model.password,
        security: security,
        isHidden: model.isHidden,
        joinOnce: false,
      );

      if (context.mounted) {
        if (connected) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              backgroundColor: AppColors.card,
              content: Text("Đã kết nối thành công tới ${model.ssid}!", style: const TextStyle(color: AppColors.mint)),
              behavior: SnackBarBehavior.floating,
            ),
          );
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              backgroundColor: AppColors.card,
              content: Text("Không thể kết nối trực tiếp tới ${model.ssid}. Vui lòng kiểm tra trong cài đặt Wi-Fi.", style: const TextStyle(color: AppColors.foreground)),
              behavior: SnackBarBehavior.floating,
            ),
          );
        }
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            backgroundColor: AppColors.card,
            content: Text("Lỗi kết nối Wi-Fi: $e", style: const TextStyle(color: AppColors.foreground)),
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    }
  }

  static void showWifiModal(BuildContext context, String rawQr) {
    final model = parseWifi(rawQr);

    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (ctx) => _WifiBottomSheet(model: model),
    );
  }
}

class _WifiBottomSheet extends StatefulWidget {
  final WifiModel model;

  const _WifiBottomSheet({required this.model});

  @override
  State<_WifiBottomSheet> createState() => _WifiBottomSheetState();
}

class _WifiBottomSheetState extends State<_WifiBottomSheet> {
  bool _showPassword = false;

  @override
  Widget build(BuildContext context) {
    final model = widget.model;

    return Container(
      padding: const EdgeInsets.fromLTRB(24, 20, 24, 32),
      decoration: const BoxDecoration(
        color: AppColors.card,
        borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
        border: Border(top: BorderSide(color: AppColors.cardStroke, width: 1.5)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Drag handle
          Center(
            child: Container(
              width: 36,
              height: 4,
              decoration: BoxDecoration(
                color: AppColors.cardStroke,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          const SizedBox(height: 18),

          // Header
          Row(
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: AppColors.iconBg,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: const Icon(Icons.wifi, color: AppColors.mint, size: 24),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      "Thông tin mạng Wi-Fi",
                      style: TextStyle(
                        fontSize: 17,
                        fontWeight: FontWeight.bold,
                        color: AppColors.foreground,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      "Bảo mật: ${model.securityType.isEmpty ? 'WPA/WPA2' : model.securityType}",
                      style: const TextStyle(fontSize: 12, color: AppColors.mutedForeground),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 22),

          // SSID Section
          const Text(
            "Tên mạng Wi-Fi (SSID)",
            style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: AppColors.mutedForeground),
          ),
          const SizedBox(height: 6),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
            decoration: BoxDecoration(
              color: AppColors.background,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppColors.cardStroke),
            ),
            child: Text(
              model.ssid,
              style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: AppColors.foreground),
            ),
          ),
          const SizedBox(height: 16),

          // Password Section
          const Text(
            "Mật khẩu",
            style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: AppColors.mutedForeground),
          ),
          const SizedBox(height: 6),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
            decoration: BoxDecoration(
              color: AppColors.background,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppColors.cardStroke),
            ),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    model.isNoPassword
                        ? "Không có mật khẩu (Mạng mở)"
                        : (_showPassword ? model.password : "••••••••••••"),
                    style: TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.w600,
                      color: model.isNoPassword ? AppColors.mutedForeground : AppColors.foreground,
                    ),
                  ),
                ),
                if (!model.isNoPassword)
                  IconButton(
                    icon: Icon(
                      _showPassword ? Icons.visibility_off : Icons.visibility,
                      color: AppColors.mutedForeground,
                      size: 20,
                    ),
                    onPressed: () => setState(() => _showPassword = !_showPassword),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 24),

          // Buttons
          Row(
            children: [
              if (!model.isNoPassword) ...[
                Expanded(
                  child: OutlinedButton(
                    style: OutlinedButton.styleFrom(
                      side: const BorderSide(color: AppColors.cardStroke),
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                    ),
                    onPressed: () {
                      Clipboard.setData(ClipboardData(text: model.password));
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                          backgroundColor: AppColors.card,
                          content: Text("Đã sao chép mật khẩu Wi-Fi!", style: TextStyle(color: AppColors.foreground)),
                          behavior: SnackBarBehavior.floating,
                        ),
                      );
                    },
                    child: const Text(
                      "Sao chép MK",
                      style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppColors.foreground),
                    ),
                  ),
                ),
                const SizedBox(width: 12),
              ],
              Expanded(
                flex: model.isNoPassword ? 1 : 1,
                child: ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.mint,
                    foregroundColor: const Color(0xFF0B0E11),
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                    elevation: 0,
                  ),
                  onPressed: () {
                    Navigator.pop(context);
                    WifiHelper.connectToWifi(context, model);
                  },
                  child: const Text(
                    "Kết nối ngay",
                    style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
