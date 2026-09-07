import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';
import '../database/history_notifier.dart';
import '../database/qr_record.dart';
import '../theme/app_colors.dart';
import '../widgets/wifi_helper.dart';

class HistoryScreen extends StatefulWidget {
  final VoidCallback onBack;

  const HistoryScreen({
    super.key,
    required this.onBack,
  });

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  // 0: Tất cả, 1: Đã quét, 2: Đã tạo
  int _selectedFilter = 0;

  void _confirmDeleteAll(BuildContext context) {
    String title = "Xóa toàn bộ lịch sử?";
    String message = "Tất cả các mã đã quét và đã tạo sẽ bị xóa vĩnh viễn.";

    if (_selectedFilter == 1) {
      title = "Xóa lịch sử quét?";
      message = "Tất cả các mã đã quét sẽ bị xóa vĩnh viễn.";
    } else if (_selectedFilter == 2) {
      title = "Xóa lịch sử tạo mã?";
      message = "Tất cả các mã bạn đã tạo sẽ bị xóa vĩnh viễn.";
    }

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.card,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
        title: Text(title, style: const TextStyle(color: AppColors.foreground, fontWeight: FontWeight.bold)),
        content: Text(
          message,
          style: const TextStyle(color: AppColors.mutedForeground, fontSize: 13),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text("Hủy", style: TextStyle(color: AppColors.mutedForeground)),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(ctx);
              if (_selectedFilter == 1) {
                HistoryNotifier.instance.clearAllScanned();
              } else if (_selectedFilter == 2) {
                HistoryNotifier.instance.clearAllCreated();
              } else {
                HistoryNotifier.instance.clearAll();
              }

              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  backgroundColor: AppColors.card,
                  content: Text("Đã xóa lịch sử thành công", style: TextStyle(color: AppColors.foreground)),
                  behavior: SnackBarBehavior.floating,
                ),
              );
            },
            child: const Text("Xóa", style: TextStyle(color: AppColors.danger, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildTopHeader(),
              const SizedBox(height: 20),
              _buildSubHeader(context),
              const SizedBox(height: 16),
              _buildFilterTabs(),
              const SizedBox(height: 18),
              _buildHistoryList(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTopHeader() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: const [
            Text(
              "Lịch sử",
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: AppColors.foreground,
              ),
            ),
            SizedBox(height: 2),
            Text(
              "Quản lý mã đã quét và đã tạo",
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
          child: const Icon(Icons.history, color: Color(0xFFA0AEC0), size: 19),
        ),
      ],
    );
  }

  Widget _buildSubHeader(BuildContext context) {
    return AnimatedBuilder(
      animation: HistoryNotifier.instance,
      builder: (context, _) {
        final currentRecords = _getCurrentRecords();
        final count = currentRecords.length;

        String countText;
        if (_selectedFilter == 1) {
          countText = "$count mã đã quét";
        } else if (_selectedFilter == 2) {
          countText = "$count mã đã tạo";
        } else {
          countText = "$count mã trong lịch sử";
        }

        return Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Row(
              children: [
                InkWell(
                  onTap: widget.onBack,
                  borderRadius: BorderRadius.circular(12),
                  child: Container(
                    width: 38,
                    height: 38,
                    decoration: BoxDecoration(
                      color: AppColors.card,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: AppColors.cardStroke),
                    ),
                    child: const Icon(Icons.chevron_left, color: AppColors.foreground, size: 22),
                  ),
                ),
                const SizedBox(width: 12),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      "Nhật ký hoạt động",
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        color: AppColors.foreground,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      countText,
                      style: const TextStyle(fontSize: 12, color: AppColors.mutedForeground),
                    ),
                  ],
                ),
              ],
            ),
            if (count > 0)
              InkWell(
                onTap: () => _confirmDeleteAll(context),
                borderRadius: BorderRadius.circular(20),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
                  decoration: BoxDecoration(
                    color: AppColors.dangerBg,
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(color: AppColors.danger.withValues(alpha: 0.3)),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: const [
                      Icon(Icons.delete_outline, color: AppColors.danger, size: 16),
                      SizedBox(width: 5),
                      Text(
                        "Xóa tất cả",
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                          color: AppColors.danger,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
          ],
        );
      },
    );
  }

  Widget _buildFilterTabs() {
    return AnimatedBuilder(
      animation: HistoryNotifier.instance,
      builder: (context, _) {
        final totalCount = HistoryNotifier.instance.totalCount;
        final scannedCount = HistoryNotifier.instance.scannedCount;
        final createdCount = HistoryNotifier.instance.createdCount;

        return Container(
          padding: const EdgeInsets.all(4),
          decoration: BoxDecoration(
            color: AppColors.card,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: AppColors.cardStroke),
          ),
          child: Row(
            children: [
              _buildTabItem(0, "Tất cả ($totalCount)"),
              _buildTabItem(1, "Đã quét ($scannedCount)"),
              _buildTabItem(2, "Đã tạo ($createdCount)"),
            ],
          ),
        );
      },
    );
  }

  Widget _buildTabItem(int index, String title) {
    final isSelected = _selectedFilter == index;
    return Expanded(
      child: GestureDetector(
        onTap: () {
          setState(() {
            _selectedFilter = index;
          });
        },
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 180),
          padding: const EdgeInsets.symmetric(vertical: 9),
          decoration: BoxDecoration(
            color: isSelected ? AppColors.mint : Colors.transparent,
            borderRadius: BorderRadius.circular(12),
          ),
          child: Center(
            child: Text(
              title,
              style: TextStyle(
                fontSize: 12,
                fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
                color: isSelected ? const Color(0xFF0B0E11) : AppColors.mutedForeground,
              ),
            ),
          ),
        ),
      ),
    );
  }

  List<QrRecord> _getCurrentRecords() {
    if (_selectedFilter == 1) {
      return HistoryNotifier.instance.scannedRecords;
    } else if (_selectedFilter == 2) {
      return HistoryNotifier.instance.createdRecords;
    }
    return HistoryNotifier.instance.allRecords;
  }

  Widget _buildHistoryList() {
    return AnimatedBuilder(
      animation: HistoryNotifier.instance,
      builder: (context, _) {
        final records = _getCurrentRecords();

        if (records.isEmpty) {
          return _buildEmptyState();
        }

        return ListView.separated(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          itemCount: records.length,
          separatorBuilder: (context, index) => const SizedBox(height: 10),
          itemBuilder: (context, index) {
            final item = records[index];
            return _buildItemCard(context, item);
          },
        );
      },
    );
  }

  Widget _buildItemCard(BuildContext context, QrRecord item) {
    IconData icon;
    String label;
    final t = item.type.toLowerCase();
    final c = item.content;
    final cl = c.toLowerCase();

    if (t == 'url' || cl.startsWith('http') || cl.contains('.com')) {
      icon = Icons.language;
      label = "Liên kết";
    } else if (t == 'wifi' || c.toUpperCase().startsWith('WIFI:')) {
      icon = Icons.wifi;
      label = "Wi-Fi";
    } else if (t == 'payment' || cl.contains('đ') || cl.contains('vnd')) {
      icon = Icons.credit_card;
      label = "Thanh toán";
    } else if (t == 'contact' || cl.contains('begin:vcard')) {
      icon = Icons.badge_outlined;
      label = "Danh bạ";
    } else {
      icon = Icons.description_outlined;
      label = "Văn bản";
    }

    String relativeTime = "Vừa xong";
    try {
      final date = DateTime.parse(item.timestamp);
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

    final sourceText = item.isCreated ? "Đã tạo" : "Đã quét";

    return Container(
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: AppColors.cardStroke),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(20),
          onTap: () => _handleItemClick(context, item),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
            child: Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    color: item.isCreated
                        ? AppColors.mint.withValues(alpha: 0.12)
                        : AppColors.iconBg,
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: Icon(
                    icon,
                    color: item.isCreated ? AppColors.mint : AppColors.mint,
                    size: 20,
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              item.content,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                fontSize: 14,
                                fontWeight: FontWeight.w600,
                                color: AppColors.foreground,
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          // Badge phân biệt Đã tạo / Đã quét
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                            decoration: BoxDecoration(
                              color: item.isCreated
                                  ? AppColors.mint.withValues(alpha: 0.15)
                                  : Colors.white.withValues(alpha: 0.06),
                              borderRadius: BorderRadius.circular(6),
                              border: Border.all(
                                color: item.isCreated
                                    ? AppColors.mint.withValues(alpha: 0.4)
                                    : Colors.white.withValues(alpha: 0.1),
                              ),
                            ),
                            child: Text(
                              sourceText,
                              style: TextStyle(
                                fontSize: 10,
                                fontWeight: FontWeight.bold,
                                color: item.isCreated ? AppColors.mint : AppColors.mutedForeground,
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text(
                        "$label · $relativeTime",
                        style: const TextStyle(fontSize: 12, color: AppColors.mutedForeground),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                // Individual delete button
                InkWell(
                  onTap: () {
                    if (item.id != null) {
                      HistoryNotifier.instance.deleteItem(item.id!, isCreated: item.isCreated);
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                          backgroundColor: AppColors.card,
                          content: Text("Đã xóa mục lịch sử", style: TextStyle(color: AppColors.foreground)),
                          duration: Duration(milliseconds: 1200),
                          behavior: SnackBarBehavior.floating,
                        ),
                      );
                    }
                  },
                  borderRadius: BorderRadius.circular(12),
                  child: Container(
                    width: 36,
                    height: 36,
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(color: AppColors.cardStroke),
                    ),
                    child: const Icon(Icons.delete_outline, color: AppColors.mutedForeground, size: 18),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildEmptyState() {
    String title = "Chưa có lịch sử";
    String subtitle = "Các mã QR bạn quét hoặc tạo sẽ xuất hiện tại đây.";

    if (_selectedFilter == 1) {
      title = "Chưa có mã đã quét";
      subtitle = "Các mã QR bạn quét qua Camera sẽ xuất hiện tại đây.";
    } else if (_selectedFilter == 2) {
      title = "Chưa có mã đã tạo";
      subtitle = "Các mã QR bạn tự tạo sẽ xuất hiện tại đây.";
    }

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(vertical: 60, horizontal: 24),
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: AppColors.cardStroke),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 58,
            height: 58,
            decoration: BoxDecoration(
              color: AppColors.iconBg,
              borderRadius: BorderRadius.circular(18),
            ),
            child: Icon(
              _selectedFilter == 2 ? Icons.qr_code : Icons.qr_code_scanner,
              color: AppColors.mutedForeground,
              size: 28,
            ),
          ),
          const SizedBox(height: 16),
          Text(
            title,
            style: const TextStyle(
              fontSize: 15,
              fontWeight: FontWeight.bold,
              color: AppColors.foreground,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            subtitle,
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 12, color: AppColors.mutedForeground),
          ),
        ],
      ),
    );
  }
}
