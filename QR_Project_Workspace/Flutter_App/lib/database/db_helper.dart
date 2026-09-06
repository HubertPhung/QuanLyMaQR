import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import 'qr_record.dart';

class DatabaseHelper {
  static const _databaseName = "qr_database.db";
  static const _databaseVersion = 1;

  static const tableQrHistory = 'qr_history';
  static const columnId = 'id';
  static const columnContent = 'content';
  static const columnType = 'type';
  static const columnTimestamp = 'timestamp';

  // Khởi tạo Singleton
  DatabaseHelper._privateConstructor();
  static final DatabaseHelper instance = DatabaseHelper._privateConstructor();

  static Database? _database;

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDatabase();
    return _database!;
  }

  Future<Database> _initDatabase() async {
    String path = join(await getDatabasesPath(), _databaseName);
    return await openDatabase(
      path,
      version: _databaseVersion,
      onCreate: _onCreate,
    );
  }

  Future _onCreate(Database db, int version) async {
    await db.execute('''
      CREATE TABLE $tableQrHistory (
        $columnId INTEGER PRIMARY KEY AUTOINCREMENT,
        $columnContent TEXT,
        $columnType TEXT,
        $columnTimestamp TEXT
      )
    ''');
  }

  // 1. Thêm lịch sử (addRecord)
  Future<int> addRecord(QrRecord record) async {
    Database db = await instance.database;
    return await db.insert(
      tableQrHistory,
      record.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  // 2. Lấy toàn bộ danh sách (getAllRecords)
  Future<List<QrRecord>> getAllRecords() async {
    Database db = await instance.database;
    // Lấy dữ liệu và sắp xếp theo ID giảm dần (mới nhất lên đầu)
    final List<Map<String, dynamic>> maps = await db.query(
      tableQrHistory,
      orderBy: '$columnId DESC',
    );

    // Chuyển List<Map<String, dynamic>> thành List<QrRecord>
    return List.generate(maps.length, (i) {
      return QrRecord.fromMap(maps[i]);
    });
  }

  // 3. Xóa toàn bộ dữ liệu (deleteAll)
  Future<int> deleteAll() async {
    Database db = await instance.database;
    return await db.delete(tableQrHistory);
  }

  // 4. Xóa từng bản ghi (deleteRecord)
  Future<int> deleteRecord(int id) async {
    Database db = await instance.database;
    return await db.delete(
      tableQrHistory,
      where: '$columnId = ?',
      whereArgs: [id],
    );
  }
}
