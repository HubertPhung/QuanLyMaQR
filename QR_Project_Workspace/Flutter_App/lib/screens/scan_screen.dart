import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:url_launcher/url_launcher.dart';
import '../database/history_notifier.dart';
import '../database/qr_record.dart';
import '../theme/app_colors.dart';
import '../widgets/wifi_helper.dart';

class ScanScreen extends StatefulWidget {
  final Function(int) onNavigate;
  final bool isActive;

  const ScanScreen({
    super.key,
    required this.onNavigate,
    this.isActive = true,
  });

  @override
  State<ScanScreen> createState() => _ScanScreenState();
}

class _ScanScreenState extends State<ScanScreen> with SingleTickerProviderStateMixin {
  final MobileScannerController _scannerController = MobileScannerController();
  late AnimationController _animController;
  late Animation<double> _laserAnimation;
  bool _isProcessing = false;

  @override
  void initState() {
    super.initState();
    _animController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 2200),
    )..repeat(reverse: true);
    _laserAnimation = Tween<double>(begin: 0.05, end: 0.95).animate(
      CurvedAnimation(parent: _animController, curve: Curves.easeInOut),
    );
  }

  @override
  void didUpdateWidget(covariant ScanScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.isActive != oldWidget.isActive) {
      if (widget.isActive) {
        _scannerController.start();
        _animController.repeat(reverse: true);
      } else {
        _scannerController.stop();
        _animController.stop();
      }
    }
  }

  @override
  void dispose() {
    _scannerController.dispose();
    _animController.dispose();
    super.dispose();
  }

  void _onDetect(BarcodeCapture capture) async {
    if (_isProcessing || !widget.isActive) return;
    final List<Barcode> barcodes = capture.barcodes;
    if (barcodes.isNotEmpty && barcodes.first.rawValue != null) {
      _isProcessing = true;
      final String code = barcodes.first.rawValue!;

      // Haptic feedback
      HapticFeedback.mediumImpact();

      // Phân loại
      final lower = code.toLowerCase();
      String type = "text";
      if (lower.startsWith("http://") ||
          lower.startsWith("https://") ||
          lower.contains(".com") ||
          lower.contains(".vn") ||
          lower.contains(".net") ||
          lower.contains(".org")) {
        type = "url";
      } else if (code.toUpperCase().startsWith("WIFI:")) {
        type = "wifi";
      } else if (code.contains("₫") || code.contains("VND") || code.contains("240.000")) {
        type = "payment";
      } else if (code.contains("BEGIN:VCARD")) {
        type = "contact";
      }

      await HistoryNotifier.instance.addRecord(QrRecord(
        content: code,
        type: type,
        timestamp: DateTime.now().toIso8601String(),
      ));

      if (mounted) {
        _handleScanAction(type, code);
      }

      await Future.delayed(const Duration(seconds: 2));
      _isProcessing = false;
    }
  }

  void _handleScanAction(String type, String code) async {
    if (!mounted) return;

    if (type == 'url') {
      String url = code;
      if (!url.startsWith("http://") && !url.startsWith("https://")) {
        url = "https://$url";
      }
      try {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            backgroundColor: AppColors.card,
            content: Text('Đang mở liên kết…', style: TextStyle(color: AppColors.mint)),
            behavior: SnackBarBehavior.floating,
          ),
        );
        final uri = Uri.parse(url);
        await launchUrl(uri, mode: LaunchMode.externalApplication);
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              backgroundColor: AppColors.card,
              content: Text('Không thể mở liên kết: $url', style: const TextStyle(color: AppColors.foreground)),
              behavior: SnackBarBehavior.floating,
            ),
          );
        }
      }
    } else if (type == 'wifi') {
      WifiHelper.showWifiModal(context, code);
      WifiHelper.connectToWifi(context, WifiHelper.parseWifi(code));
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          backgroundColor: AppColors.card,
          content: Text('Đã quét: $code', style: const TextStyle(color: AppColors.foreground)),
          behavior: SnackBarBehavior.floating,
        ),
      );
    }
  }

  void _handleItemClick(BuildContext context, QrRecord record) async {
    final t = record.type.toLowerCase();
    final c = record.content;
    final cl = c.toLowerCase();

    if (t == 'url' || cl.startsWith('http://') || cl.startsWith('https://') || cl.contains('.com') || cl.contains('.vn')) {
      String url = c;
      if (!url.startsWith("http://") && !url.startsWith("https://")) {
        url = "https://$url";
      }
      try {
        final uri = Uri.parse(url);
        await launchUrl(uri, mode: LaunchMode.externalApplication);
      } catch (_) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              backgroundColor: AppColors.card,
              content: Text('Không thể mở: $url', style: const TextStyle(color: AppColors.foreground)),
              behavior: SnackBarBehavior.floating,
            ),
          );
        }
      }
    } else if (t == 'wifi' || c.toUpperCase().startsWith('WIFI:')) {
      WifiHelper.showWifiModal(context, c);
    } else {
      Clipboard.setData(ClipboardData(text: c));
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          backgroundColor: AppColors.card,
          content: Text('Đã sao chép: $c', style: const TextStyle(color: AppColors.foreground)),
          behavior: SnackBarBehavior.floating,
        ),
      );
    }
  }

  void _pickFromGallery() {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        backgroundColor: AppColors.card,
        content: Text('Chọn ảnh từ thư viện', style: TextStyle(color: AppColors.foreground)),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 28),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              _buildHeader(),
              const SizedBox(height: 28),
              _buildScannerFrame(),
              const SizedBox(height: 32),
              const Text(
                "Giữ điện thoại ổn định và căn mã QR nằm gọn trong khung viền.",
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 13,
                  color: AppColors.mutedForeground,
                  height: 1.4,
                ),
              ),
              const SizedBox(height: 22),
              _buildActionRow(),
              const SizedBox(height: 32),
              _buildHistorySection(),
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
              "Quét mã QR",
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: AppColors.foreground,
              ),
            ),
            SizedBox(height: 2),
            Text(
              "Đưa mã vào khung để quét",
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

  Widget _buildScannerFrame() {
    return Stack(
      clipBehavior: Clip.none,
      alignment: Alignment.center,
      children: [
        // Glow effect
        Positioned.fill(
          child: Container(
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(40),
              boxShadow: [
                BoxShadow(
                  color: AppColors.mint.withValues(alpha: 0.12),
                  blurRadius: 36,
                  spreadRadius: 4,
                ),
              ],
            ),
          ),
        ),
        Container(
          width: 290,
          height: 290,
          decoration: BoxDecoration(
            color: AppColors.card,
            borderRadius: BorderRadius.circular(36),
            border: Border.all(color: AppColors.cardStroke),
          ),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(36),
            child: Stack(
              children: [
                // Chỉ bật Camera khi tab Quét mã đang active
                if (widget.isActive)
                  MobileScanner(
                    controller: _scannerController,
                    onDetect: _onDetect,
                  )
                else
                  Container(
                    color: const Color(0xFF0B1220),
                    child: const Center(
                      child: Icon(Icons.videocam_off_outlined, color: AppColors.mutedForeground, size: 42),
                    ),
                  ),

                // Scanner overlay with neon mint corner brackets & mock matrix
                CustomPaint(
                  size: const Size(290, 290),
                  painter: ScannerOverlayPainter(),
                ),

                // Animated laser scan line (chỉ chạy khi tab active)
                if (widget.isActive)
                  AnimatedBuilder(
                    animation: _laserAnimation,
                    builder: (context, _) {
                      return Positioned(
                        top: _laserAnimation.value * 270,
                        left: 20,
                        right: 20,
                        child: Container(
                          height: 2.5,
                          decoration: BoxDecoration(
                            color: AppColors.mint,
                            borderRadius: BorderRadius.circular(2),
                            boxShadow: [
                              BoxShadow(
                                color: AppColors.mint.withValues(alpha: 0.8),
                                blurRadius: 10,
                                spreadRadius: 2,
                              ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
              ],
            ),
          ),
        ),
        Positioned(
          bottom: -16,
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 8),
            decoration: BoxDecoration(
              color: AppColors.pillBg,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: AppColors.mint.withValues(alpha: 0.4)),
              boxShadow: const [
                BoxShadow(color: Colors.black54, blurRadius: 10, offset: Offset(0, 4)),
              ],
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(widget.isActive ? Icons.bolt : Icons.pause, color: AppColors.mint, size: 16),
                const SizedBox(width: 6),
                Text(
                  widget.isActive ? "Đang quét…" : "Tạm dừng",
                  style: const TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.bold,
                    color: AppColors.foreground,
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildActionRow() {
    return InkWell(
      onTap: _pickFromGallery,
      borderRadius: BorderRadius.circular(24),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 10),
        decoration: BoxDecoration(
          color: AppColors.card,
          borderRadius: BorderRadius.circular(24),
          border: Border.all(color: AppColors.cardStroke),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: const [
            Icon(Icons.image_outlined, color: AppColors.mint, size: 18),
            SizedBox(width: 8),
            Text(
              "Chọn từ thư viện",
              style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: AppColors.foreground),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHistorySection() {
    return AnimatedBuilder(
      animation: HistoryNotifier.instance,
      builder: (context, _) {
        final recentItems = HistoryNotifier.instance.getRecent(4);
        return SizedBox(
          width: double.infinity,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    "Lịch sử quét",
                    style: TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.bold,
                      color: AppColors.foreground,
                    ),
                  ),
                  InkWell(
                    onTap: () => widget.onNavigate(3),
                    child: const Padding(
                      padding: EdgeInsets.symmetric(vertical: 4, horizontal: 2),
                      child: Text(
                        "Xem tất cả",
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                          color: AppColors.mint,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              if (recentItems.isEmpty)
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(18),
                  decoration: BoxDecoration(
                    color: AppColors.card,
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(color: AppColors.cardStroke),
                  ),
                  child: const Center(
                    child: Text(
                      "Chưa có mã nào được quét.",
                      style: TextStyle(color: AppColors.mutedForeground, fontSize: 13),
                    ),
                  ),
                )
              else
                ListView.separated(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: recentItems.length,
                  separatorBuilder: (context, index) => const SizedBox(height: 10),
                  itemBuilder: (context, index) {
                    return _buildHistoryCard(recentItems[index]);
                  },
                ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildHistoryCard(QrRecord record) {
    String subtitle = "Văn bản";
    IconData icon = Icons.description_outlined;
    final t = record.type.toLowerCase();
    final c = record.content.toLowerCase();

    if (t == 'url' || c.startsWith('http') || c.contains('.com')) {
      subtitle = "Liên kết · Nhấn để mở web";
      icon = Icons.language;
    } else if (t == 'wifi' || c.startsWith('wifi:')) {
      subtitle = "Wi-Fi · Nhấn để xem & kết nối";
      icon = Icons.wifi;
    } else if (t == 'payment' || c.contains('đ') || c.contains('vnd')) {
      subtitle = "Thanh toán · Chuyển khoản QR";
      icon = Icons.credit_card;
    } else if (t == 'contact' || c.contains('begin:vcard')) {
      subtitle = "Danh bạ · Danh thiếp liên hệ";
      icon = Icons.badge_outlined;
    }

    String relativeTime = "Vừa xong";
    try {
      final date = DateTime.parse(record.timestamp);
      final diff = DateTime.now().difference(date);
      if (diff.inMinutes < 1) {
        relativeTime = "Vừa xong";
      } else if (diff.inMinutes < 60) {
        relativeTime = "${diff.inMinutes} phút trước";
      } else if (diff.inHours < 24) {
        relativeTime = "${diff.inHours} giờ trước";
      } else if (diff.inDays == 1) {
        relativeTime = "Hôm qua";
      } else {
        relativeTime = "${diff.inDays} ngày trước";
      }
    } catch (_) {}

    return InkWell(
      onTap: () => _handleItemClick(context, record),
      borderRadius: BorderRadius.circular(20),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        decoration: BoxDecoration(
          color: AppColors.card,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: AppColors.cardStroke),
        ),
        child: Row(
          children: [
            Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                color: AppColors.iconBg,
                borderRadius: BorderRadius.circular(14),
              ),
              child: Icon(icon, color: AppColors.mint, size: 20),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    record.content,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: AppColors.foreground,
                    ),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    subtitle,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontSize: 12, color: AppColors.mutedForeground),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            Text(
              "$relativeTime >",
              style: const TextStyle(fontSize: 12, color: AppColors.mutedForeground),
            ),
          ],
        ),
      ),
    );
  }
}

class ScannerOverlayPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    // 1. Faint grid
    final gridPaint = Paint()
      ..color = const Color(0x12FFFFFF)
      ..strokeWidth = 1;

    for (double i = 0; i < size.width; i += 28) {
      canvas.drawLine(Offset(i, 0), Offset(i, size.height), gridPaint);
    }
    for (double i = 0; i < size.height; i += 28) {
      canvas.drawLine(Offset(0, i), Offset(size.width, i), gridPaint);
    }

    // 2. Corner Brackets (Neon Mint)
    final bracketPaint = Paint()
      ..color = AppColors.mint
      ..strokeWidth = 3.5
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round;

    const double cornerLength = 32;
    const double radius = 16;
    const double inset = 16;

    // Top-Left
    final pathTL = Path()
      ..moveTo(inset, inset + cornerLength)
      ..lineTo(inset, inset + radius)
      ..arcToPoint(Offset(inset + radius, inset), radius: const Radius.circular(radius))
      ..lineTo(inset + cornerLength, inset);
    canvas.drawPath(pathTL, bracketPaint);

    // Top-Right
    final pathTR = Path()
      ..moveTo(size.width - inset - cornerLength, inset)
      ..lineTo(size.width - inset - radius, inset)
      ..arcToPoint(Offset(size.width - inset, inset + radius), radius: const Radius.circular(radius))
      ..lineTo(size.width - inset, inset + cornerLength);
    canvas.drawPath(pathTR, bracketPaint);

    // Bottom-Left
    final pathBL = Path()
      ..moveTo(inset, size.height - inset - cornerLength)
      ..lineTo(inset, size.height - inset - radius)
      ..arcToPoint(Offset(inset + radius, size.height - inset), radius: const Radius.circular(radius), clockwise: false)
      ..lineTo(inset + cornerLength, size.height - inset);
    canvas.drawPath(pathBL, bracketPaint);

    // Bottom-Right
    final pathBR = Path()
      ..moveTo(size.width - inset - cornerLength, size.height - inset)
      ..lineTo(size.width - inset - radius, size.height - inset)
      ..arcToPoint(Offset(size.width - inset, size.height - inset - radius), radius: const Radius.circular(radius), clockwise: false)
      ..lineTo(size.width - inset, size.height - inset - cornerLength);
    canvas.drawPath(pathBR, bracketPaint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
