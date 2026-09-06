import 'package:flutter/material.dart';
import '../database/history_notifier.dart';
import '../database/qr_record.dart';
import '../theme/app_colors.dart';

class HistoryScreen extends StatelessWidget {
  final VoidCallback onBack;

  const HistoryScreen({
    super.key,
    required this.onBack,
  });

  void _confirmDeleteAll(BuildContext context) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.card,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
        title: const Text("Xóa toàn bộ lịch sử?", style: TextStyle(color: AppColors.foreground, fontWeight: FontWeight.bold)),
        content: const Text(
          "Tất cả các mã đã quét sẽ bị xóa vĩnh viễn.",
          style: TextStyle(color: AppColors.mutedForeground, fontSize: 13),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text("Hủy", style: TextStyle(color: AppColors.mutedForeground)),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(ctx);
              HistoryNotifier.instance.clearAll();
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  backgroundColor: AppColors.card,
                  content: Text("Đã xóa toàn bộ lịch sử", style: TextStyle(color: AppColors.foreground)),
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
              "Các mã đã quét gần đây",
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

  Widget _buildSubHeader(BuildContext context) {
    return AnimatedBuilder(
      animation: HistoryNotifier.instance,
      builder: (context, _) {
        final count = HistoryNotifier.instance.count;
        return Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Row(
              children: [
                InkWell(
                  onTap: onBack,
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
                      "Lịch sử quét",
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        color: AppColors.foreground,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      "$count mã đã quét",
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

  Widget _buildHistoryList() {
    return AnimatedBuilder(
      animation: HistoryNotifier.instance,
      builder: (context, _) {
        final records = HistoryNotifier.instance.records;

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
    final c = item.content.toLowerCase();

    if (t == 'url' || c.startsWith('http') || c.contains('.com')) {
      icon = Icons.language;
      label = "Liên kết";
    } else if (t == 'wifi' || c.startsWith('wifi:')) {
      icon = Icons.wifi;
      label = "Wi-Fi";
    } else if (t == 'payment' || c.contains('đ') || c.contains('vnd')) {
      icon = Icons.credit_card;
      label = "Thanh toán";
    } else if (t == 'contact' || c.contains('begin:vcard')) {
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

    return Container(
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
                  item.content,
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
                  "$label · $relativeTime",
                  style: const TextStyle(fontSize: 12, color: AppColors.mutedForeground),
                ),
              ],
            ),
          ),
          const SizedBox(width: 6),
          // Individual delete button
          InkWell(
            onTap: () {
              if (item.id != null) {
                HistoryNotifier.instance.deleteItem(item.id!);
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
    );
  }

  Widget _buildEmptyState() {
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
            child: const Icon(Icons.qr_code_2, color: AppColors.mutedForeground, size: 28),
          ),
          const SizedBox(height: 16),
          const Text(
            "Chưa có lịch sử",
            style: TextStyle(
              fontSize: 15,
              fontWeight: FontWeight.bold,
              color: AppColors.foreground,
            ),
          ),
          const SizedBox(height: 6),
          const Text(
            "Các mã QR bạn quét sẽ xuất hiện tại đây.",
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 12, color: AppColors.mutedForeground),
          ),
        ],
      ),
    );
  }
}
