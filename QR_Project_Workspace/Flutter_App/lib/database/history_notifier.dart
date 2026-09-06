import 'package:flutter/foundation.dart';
import 'db_helper.dart';
import 'qr_record.dart';

class HistoryNotifier extends ChangeNotifier {
  static final HistoryNotifier instance = HistoryNotifier._internal();
  HistoryNotifier._internal();

  List<QrRecord> _records = [];
  bool _isLoading = true;

  List<QrRecord> get records => List.unmodifiable(_records);
  int get count => _records.length;
  bool get isLoading => _isLoading;

  Future<void> init() async {
    await loadRecords();
  }

  Future<void> loadRecords() async {
    _isLoading = true;
    notifyListeners();

    var list = await DatabaseHelper.instance.getAllRecords();
    if (list.isEmpty) {
      final now = DateTime.now();
      // Khởi tạo 7 mục lịch sử mẫu ban đầu chuẩn giao diện qr-scanner-interface
      final initialSeeds = [
        QrRecord(
          content: "vercel.com/dashboard",
          type: "url",
          timestamp: now.subtract(const Duration(seconds: 15)).toIso8601String(),
        ),
        QrRecord(
          content: "Coffee_House_5G",
          type: "wifi",
          timestamp: now.subtract(const Duration(minutes: 2)).toIso8601String(),
        ),
        QrRecord(
          content: "240.000đ",
          type: "payment",
          timestamp: now.subtract(const Duration(minutes: 18)).toIso8601String(),
        ),
        QrRecord(
          content: "Hoài Bo",
          type: "contact",
          timestamp: now.subtract(const Duration(days: 1)).toIso8601String(),
        ),
        QrRecord(
          content: "github.com/vercel/next.js",
          type: "url",
          timestamp: now.subtract(const Duration(days: 1, hours: 2)).toIso8601String(),
        ),
        QrRecord(
          content: "Home_Network_2.4G",
          type: "wifi",
          timestamp: now.subtract(const Duration(days: 2)).toIso8601String(),
        ),
        QrRecord(
          content: "89.000đ",
          type: "payment",
          timestamp: now.subtract(const Duration(days: 3)).toIso8601String(),
        ),
      ];

      for (var seed in initialSeeds) {
        await DatabaseHelper.instance.addRecord(seed);
      }
      list = await DatabaseHelper.instance.getAllRecords();
    }

    _records = list;
    _isLoading = false;
    notifyListeners();
  }

  Future<void> deleteItem(int id) async {
    await DatabaseHelper.instance.deleteRecord(id);
    _records.removeWhere((r) => r.id == id);
    notifyListeners();
  }

  Future<void> clearAll() async {
    await DatabaseHelper.instance.deleteAll();
    _records.clear();
    notifyListeners();
  }

  Future<void> addRecord(QrRecord record) async {
    await DatabaseHelper.instance.addRecord(record);
    _records = await DatabaseHelper.instance.getAllRecords();
    notifyListeners();
  }

  List<QrRecord> getRecent(int limit) {
    if (_records.length <= limit) return _records;
    return _records.sublist(0, limit);
  }
}
