import 'package:flutter/foundation.dart';
import 'db_helper.dart';
import 'qr_record.dart';

class HistoryNotifier extends ChangeNotifier {
  static final HistoryNotifier instance = HistoryNotifier._internal();
  HistoryNotifier._internal();

  List<QrRecord> _scannedRecords = [];
  List<QrRecord> _createdRecords = [];
  bool _isLoading = true;

  List<QrRecord> get scannedRecords => List.unmodifiable(_scannedRecords);
  List<QrRecord> get createdRecords => List.unmodifiable(_createdRecords);

  List<QrRecord> get allRecords {
    final combined = <QrRecord>[..._scannedRecords, ..._createdRecords];
    combined.sort((a, b) {
      try {
        final dateA = DateTime.parse(a.timestamp);
        final dateB = DateTime.parse(b.timestamp);
        return dateB.compareTo(dateA);
      } catch (_) {
        return 0;
      }
    });
    return List.unmodifiable(combined);
  }

  List<QrRecord> get records => allRecords;
  int get count => _scannedRecords.length;
  int get scannedCount => _scannedRecords.length;
  int get createdCount => _createdRecords.length;
  int get totalCount => _scannedRecords.length + _createdRecords.length;
  bool get isLoading => _isLoading;

  Future<void> init() async {
    await loadRecords();
  }

  Future<void> loadRecords() async {
    _isLoading = true;
    notifyListeners();

    _scannedRecords = await DatabaseHelper.instance.getAllRecords();
    _createdRecords = await DatabaseHelper.instance.getAllCreatedRecords();

    _isLoading = false;
    notifyListeners();
  }

  Future<void> deleteItem(int id, {bool isCreated = false}) async {
    if (isCreated) {
      await DatabaseHelper.instance.deleteCreatedRecord(id);
      _createdRecords.removeWhere((r) => r.id == id);
    } else {
      await DatabaseHelper.instance.deleteRecord(id);
      _scannedRecords.removeWhere((r) => r.id == id);
    }
    notifyListeners();
  }

  Future<void> clearAllScanned() async {
    await DatabaseHelper.instance.deleteAll();
    _scannedRecords.clear();
    notifyListeners();
  }

  Future<void> clearAllCreated() async {
    await DatabaseHelper.instance.deleteAllCreated();
    _createdRecords.clear();
    notifyListeners();
  }

  Future<void> clearAll() async {
    await DatabaseHelper.instance.deleteAll();
    await DatabaseHelper.instance.deleteAllCreated();
    _scannedRecords.clear();
    _createdRecords.clear();
    notifyListeners();
  }

  Future<void> addRecord(QrRecord record) async {
    await DatabaseHelper.instance.addRecord(record);
    _scannedRecords = await DatabaseHelper.instance.getAllRecords();
    notifyListeners();
  }

  Future<void> addCreatedRecord(QrRecord record) async {
    await DatabaseHelper.instance.addCreatedRecord(record);
    _createdRecords = await DatabaseHelper.instance.getAllCreatedRecords();
    notifyListeners();
  }

  List<QrRecord> getRecent(int limit) {
    final all = allRecords;
    if (all.length <= limit) return all;
    return all.sublist(0, limit);
  }
}
