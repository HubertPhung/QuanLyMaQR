import 'package:flutter/material.dart';
import '../database/history_notifier.dart';
import '../database/qr_record.dart';
import '../theme/app_colors.dart';

class HomeScreen extends StatelessWidget {
  final Function(int) onNavigate;

  const HomeScreen({
    super.key,
    required this.onNavigate,
  });

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
              _buildGreeting(),
              const SizedBox(height: 20),
              _buildStatCard(),
              const SizedBox(height: 18),
              _buildQuickActions(),
              const SizedBox(height: 26),
              _buildRecentSection(),
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
        Row(
          children: [
            ClipRRect(
              borderRadius: BorderRadius.circular(12),
              child: Image.asset("assets/app_logo.png", width: 42, height: 42, fit: BoxFit.cover),
            ),
            const SizedBox(width: 12),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: const [
                Text(
                  "QR Scanner",
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: AppColors.foreground,
                  ),
                ),
                SizedBox(height: 2),
                Text(
                  "Quét & tạo mã nhanh chóng",
                  style: TextStyle(fontSize: 12, color: AppColors.mutedForeground),
                ),
              ],
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

  Widget _buildGreeting() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: const [
        Text(
          "Chào buổi sáng,",
          style: TextStyle(fontSize: 13, color: AppColors.mutedForeground),
        ),
        SizedBox(height: 2),
        Text(
          "Hoài Bo",
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.bold,
            color: AppColors.foreground,
          ),
        ),
      ],
    );
  }

  Widget _buildStatCard() {
    return AnimatedBuilder(
      animation: HistoryNotifier.instance,
      builder: (context, _) {
        final count = HistoryNotifier.instance.count;
        return Container(
          width: double.infinity,
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            gradient: const LinearGradient(
              colors: [Color(0xFF142228), Color(0xFF151C26)],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            borderRadius: BorderRadius.circular(26),
            border: Border.all(color: AppColors.mint.withValues(alpha: 0.2)),
            boxShadow: [
              BoxShadow(
                color: AppColors.mint.withValues(alpha: 0.08),
                blurRadius: 24,
                spreadRadius: 2,
              ),
            ],
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: const [
                      Icon(Icons.trending_up, color: AppColors.mint, size: 16),
                      SizedBox(width: 6),
                      Text(
                        "Tuần này",
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: AppColors.mint,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    "$count",
                    style: const TextStyle(
                      fontSize: 32,
                      fontWeight: FontWeight.bold,
                      color: AppColors.foreground,
                    ),
                  ),
                  const SizedBox(height: 2),
                  const Text(
                    "mã đã quét",
                    style: TextStyle(fontSize: 12, color: AppColors.mutedForeground),
                  ),
                ],
              ),
              Container(
                width: 58,
                height: 58,
                decoration: BoxDecoration(
                  color: AppColors.mint,
                  borderRadius: BorderRadius.circular(20),
                  boxShadow: [
                    BoxShadow(
                      color: AppColors.mint.withValues(alpha: 0.4),
                      blurRadius: 16,
                      offset: const Offset(0, 4),
                    ),
                  ],
                ),
                child: const Icon(Icons.qr_code_2, color: Color(0xFF0B0E11), size: 30),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildQuickActions() {
    return Row(
      children: [
        Expanded(
          child: _buildActionCard(
            title: "Quét mã QR",
            subtitle: "Mở camera quét ngay",
            icon: Icons.qr_code_scanner,
            isPrimary: true,
            onTap: () => onNavigate(1),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _buildActionCard(
            title: "Tạo mã QR",
            subtitle: "Tạo mã của riêng bạn",
            icon: Icons.qr_code,
            isPrimary: false,
            onTap: () => onNavigate(2),
          ),
        ),
      ],
    );
  }

  Widget _buildActionCard({
    required String title,
    required String subtitle,
    required IconData icon,
    required bool isPrimary,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(22),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppColors.card,
          borderRadius: BorderRadius.circular(22),
          border: Border.all(color: AppColors.cardStroke),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                color: isPrimary ? AppColors.mint : AppColors.iconBg,
                borderRadius: BorderRadius.circular(14),
              ),
              child: Icon(
                icon,
                color: isPrimary ? const Color(0xFF0B0E11) : AppColors.mint,
                size: 22,
              ),
            ),
            const SizedBox(height: 14),
            Text(
              title,
              style: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.bold,
                color: AppColors.foreground,
              ),
            ),
            const SizedBox(height: 3),
            Text(
              subtitle,
              style: const TextStyle(fontSize: 11, color: AppColors.mutedForeground),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildRecentSection() {
    return AnimatedBuilder(
      animation: HistoryNotifier.instance,
      builder: (context, _) {
        final recentItems = HistoryNotifier.instance.getRecent(3);
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  "Quét gần đây",
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.bold,
                    color: AppColors.foreground,
                  ),
                ),
                InkWell(
                  onTap: () => onNavigate(3),
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
                  return _buildRecentCard(recentItems[index]);
                },
              ),
          ],
        );
      },
    );
  }

  Widget _buildRecentCard(QrRecord item) {
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
                  label,
                  style: const TextStyle(fontSize: 12, color: AppColors.mutedForeground),
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          const Icon(Icons.chevron_right, color: AppColors.mutedForeground, size: 18),
        ],
      ),
    );
  }
}
