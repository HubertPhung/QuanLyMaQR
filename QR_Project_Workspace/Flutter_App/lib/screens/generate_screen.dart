import 'dart:io';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';
import 'package:qr_flutter/qr_flutter.dart';
import '../database/history_notifier.dart';
import '../database/qr_record.dart';
import '../theme/app_colors.dart';

enum GenerateType { text, url, wifi }

class GenerateScreen extends StatefulWidget {
  const GenerateScreen({super.key});

  @override
  State<GenerateScreen> createState() => _GenerateScreenState();
}

class _GenerateScreenState extends State<GenerateScreen> {
  GenerateType _selectedType = GenerateType.url;
  final TextEditingController _textController = TextEditingController();
  final TextEditingController _urlController = TextEditingController(text: "");
  final TextEditingController _ssidController = TextEditingController();
  final TextEditingController _passController = TextEditingController();

  final GlobalKey _qrKey = GlobalKey();
  bool _isSaved = false;

  @override
  void dispose() {
    _textController.dispose();
    _urlController.dispose();
    _ssidController.dispose();
    _passController.dispose();
    super.dispose();
  }

  String get _payload {
    switch (_selectedType) {
      case GenerateType.text:
        return _textController.text.trim();
      case GenerateType.url:
        return _urlController.text.trim();
      case GenerateType.wifi:
        final ssid = _ssidController.text.trim();
        final pass = _passController.text.trim();
        if (ssid.isEmpty) return "";
        return "WIFI:T:WPA;S:$ssid;P:$pass;;";
    }
  }

  Future<void> _handleDownload() async {
    final payload = _payload;
    if (payload.isEmpty) return;

    try {
      final boundary = _qrKey.currentContext?.findRenderObject() as RenderRepaintBoundary?;
      if (boundary == null) return;

      final ui.Image image = await boundary.toImage(pixelRatio: 3.0);
      final ByteData? byteData = await image.toByteData(format: ui.ImageByteFormat.png);
      if (byteData == null) return;

      final Uint8List pngBytes = byteData.buffer.asUint8List();

      final dir = await getApplicationDocumentsDirectory();
      final filePath = '${dir.path}/qr_code_${DateTime.now().millisecondsSinceEpoch}.png';
      final file = File(filePath);
      await file.writeAsBytes(pngBytes);

      setState(() => _isSaved = true);

      String typeStr = "text";
      if (_selectedType == GenerateType.url) {
        typeStr = "url";
      } else if (_selectedType == GenerateType.wifi) {
        typeStr = "wifi";
      }

      await HistoryNotifier.instance.addCreatedRecord(QrRecord(
        content: payload,
        type: typeStr,
        timestamp: DateTime.now().toIso8601String(),
      ));

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            backgroundColor: AppColors.card,
            content: Text("Đã lưu mã QR vào: $filePath", style: const TextStyle(color: AppColors.foreground)),
            behavior: SnackBarBehavior.floating,
          ),
        );
      }

      Future.delayed(const Duration(milliseconds: 1800), () {
        if (mounted) setState(() => _isSaved = false);
      });
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            backgroundColor: AppColors.danger,
            content: Text("Lỗi khi lưu ảnh: $e"),
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final payload = _payload;
    final hasPayload = payload.isNotEmpty;

    return Scaffold(
      backgroundColor: AppColors.background,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 28),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              _buildHeader(),
              const SizedBox(height: 20),
              _buildTypeTabs(),
              const SizedBox(height: 24),
              _buildQrPreview(payload),
              const SizedBox(height: 24),
              _buildFormInputs(),
              const SizedBox(height: 24),
              _buildDownloadButton(hasPayload),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: const [
            Text(
              "Tạo mã QR",
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: AppColors.foreground,
              ),
            ),
            SizedBox(height: 2),
            Text(
              "Tạo mã của riêng bạn",
              style: TextStyle(fontSize: 12, color: AppColors.mutedForeground),
            ),
          ],
        ),
        Container(
          width: 42,
          height: 42,
          decoration: BoxDecoration(
            color: AppColors.card,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: AppColors.cardStroke),
          ),
          child: const Icon(Icons.share_outlined, color: Color(0xFFA0AEC0), size: 19),
        ),
      ],
    );
  }

  Widget _buildTypeTabs() {
    return Container(
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.cardStroke),
      ),
      child: Row(
        children: [
          _buildTabItem(GenerateType.text, "Văn bản", Icons.text_fields),
          _buildTabItem(GenerateType.url, "Liên kết", Icons.link),
          _buildTabItem(GenerateType.wifi, "Wi-Fi", Icons.wifi),
        ],
      ),
    );
  }

  Widget _buildTabItem(GenerateType type, String label, IconData icon) {
    final isSelected = _selectedType == type;
    return Expanded(
      child: GestureDetector(
        onTap: () {
          setState(() {
            _selectedType = type;
          });
        },
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 180),
          padding: const EdgeInsets.symmetric(vertical: 10),
          decoration: BoxDecoration(
            color: isSelected ? AppColors.mint : Colors.transparent,
            borderRadius: BorderRadius.circular(14),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                icon,
                size: 16,
                color: isSelected ? const Color(0xFF0B0E11) : AppColors.mutedForeground,
              ),
              const SizedBox(width: 6),
              Text(
                label,
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
                  color: isSelected ? const Color(0xFF0B0E11) : AppColors.mutedForeground,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildQrPreview(String payload) {
    return Center(
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: AppColors.card,
          borderRadius: BorderRadius.circular(32),
          border: Border.all(color: AppColors.cardStroke),
          boxShadow: [
            BoxShadow(
              color: AppColors.mint.withValues(alpha: 0.08),
              blurRadius: 30,
              spreadRadius: 2,
            ),
          ],
        ),
        child: RepaintBoundary(
          key: _qrKey,
          child: Container(
            width: 220,
            height: 220,
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(20),
            ),
            child: payload.isEmpty
                ? const Center(
                    child: Padding(
                      padding: EdgeInsets.symmetric(horizontal: 24),
                      child: Text(
                        "Nhập nội dung để tạo mã QR",
                        textAlign: TextAlign.center,
                        style: TextStyle(color: Color(0xFFA1A1AA), fontSize: 13),
                      ),
                    ),
                  )
                : Center(
                    child: QrImageView(
                      data: payload,
                      version: QrVersions.auto,
                      size: 200.0,
                      backgroundColor: Colors.white,
                      eyeStyle: const QrEyeStyle(
                        eyeShape: QrEyeShape.square,
                        color: Color(0xFF0B1220),
                      ),
                      dataModuleStyle: const QrDataModuleStyle(
                        dataModuleShape: QrDataModuleShape.square,
                        color: Color(0xFF0B1220),
                      ),
                    ),
                  ),
          ),
        ),
      ),
    );
  }

  Widget _buildFormInputs() {
    switch (_selectedType) {
      case GenerateType.text:
        return _buildInputGroup(
          label: "Văn bản",
          child: TextField(
            controller: _textController,
            onChanged: (_) => setState(() {}),
            maxLines: 3,
            style: const TextStyle(color: AppColors.foreground, fontSize: 14),
            decoration: _inputDecoration("Nhập nội dung văn bản…"),
          ),
        );

      case GenerateType.url:
        return _buildInputGroup(
          label: "Liên kết",
          child: TextField(
            controller: _urlController,
            onChanged: (_) => setState(() {}),
            style: const TextStyle(color: AppColors.foreground, fontSize: 14),
            decoration: _inputDecoration("https://vi-du.com"),
          ),
        );

      case GenerateType.wifi:
        return Column(
          children: [
            _buildInputGroup(
              label: "Wi-Fi",
              child: TextField(
                controller: _ssidController,
                onChanged: (_) => setState(() {}),
                style: const TextStyle(color: AppColors.foreground, fontSize: 14),
                decoration: _inputDecoration("Tên mạng (SSID)"),
              ),
            ),
            const SizedBox(height: 12),
            _buildInputGroup(
              label: "Mật khẩu",
              child: TextField(
                controller: _passController,
                onChanged: (_) => setState(() {}),
                obscureText: true,
                style: const TextStyle(color: AppColors.foreground, fontSize: 14),
                decoration: _inputDecoration("Mật khẩu Wi-Fi"),
              ),
            ),
          ],
        );
    }
  }

  Widget _buildInputGroup({required String label, required Widget child}) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w600,
            color: AppColors.mutedForeground,
          ),
        ),
        const SizedBox(height: 6),
        child,
      ],
    );
  }

  InputDecoration _inputDecoration(String placeholder) {
    return InputDecoration(
      hintText: placeholder,
      hintStyle: const TextStyle(color: AppColors.mutedForeground, fontSize: 14),
      filled: true,
      fillColor: AppColors.card,
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(18),
        borderSide: const BorderSide(color: AppColors.cardStroke),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(18),
        borderSide: BorderSide(color: AppColors.mint.withValues(alpha: 0.6), width: 1.5),
      ),
    );
  }

  Widget _buildDownloadButton(bool hasPayload) {
    return SizedBox(
      width: double.infinity,
      height: 52,
      child: ElevatedButton(
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.mint,
          foregroundColor: const Color(0xFF0B0E11),
          disabledBackgroundColor: AppColors.mint.withValues(alpha: 0.35),
          disabledForegroundColor: const Color(0xFF0B0E11).withValues(alpha: 0.5),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
          elevation: 0,
        ),
        onPressed: hasPayload ? _handleDownload : null,
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(_isSaved ? Icons.check : Icons.download, size: 18),
            const SizedBox(width: 8),
            Text(
              _isSaved ? "Đã lưu mã QR" : "Tải mã QR",
              style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
            ),
          ],
        ),
      ),
    );
  }
}
