import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import 'qr_record.dart';

class DatabaseHelper {
  static const _databaseName = "qr_database.db";
  static const _databaseVersion = 2;

  static const tableQrHistory = 'qr_history';
  static const tableQrCreated = 'qr_created';

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
      onUpgrade: _onUpgrade,
      onOpen: (db) async {
        // Đảm bảo bảng qr_created luôn tồn tại
        await db.execute('''
          CREATE TABLE IF NOT EXISTS $tableQrCreated (
            $columnId INTEGER PRIMARY KEY AUTOINCREMENT,
            $columnContent TEXT,
            $columnType TEXT,
            $columnTimestamp TEXT
          )
        ''');
      },
    );
  }

  Future _onCreate(Database db, int version) async {
    await db.execute('''
      CREATE TABLE IF NOT EXISTS $tableQrHistory (
        $columnId INTEGER PRIMARY KEY AUTOINCREMENT,
        $columnContent TEXT,
        $columnType TEXT,
        $columnTimestamp TEXT
      )
    ''');

    await db.execute('''
      CREATE TABLE IF NOT EXISTS $tableQrCreated (
        $columnId INTEGER PRIMARY KEY AUTOINCREMENT,
        $columnContent TEXT,
        $columnType TEXT,
        $columnTimestamp TEXT
      )
    ''');
  }

  Future _onUpgrade(Database db, int oldVersion, int newVersion) async {
    if (oldVersion < 2) {
      await db.execute('''
        CREATE TABLE IF NOT EXISTS $tableQrCreated (
          $columnId INTEGER PRIMARY KEY AUTOINCREMENT,
          $columnContent TEXT,
          $columnType TEXT,
          $columnTimestamp TEXT
        )
      ''');
    }
  }

  // --- QUẢN LÝ MÃ ĐÃ QUÉT ---

  Future<int> addRecord(QrRecord record) async {
    Database db = await instance.database;
    return await db.insert(
      tableQrHistory,
      record.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<List<QrRecord>> getAllRecords() async {
    Database db = await instance.database;
    final List<Map<String, dynamic>> maps = await db.query(
      tableQrHistory,
      orderBy: '$columnId DESC',
    );

    return List.generate(maps.length, (i) {
      return QrRecord.fromMap(maps[i]);
    });
  }

  Future<int> getScannedCount() async {
    Database db = await instance.database;
    final result = await db.rawQuery('SELECT COUNT(*) as count FROM $tableQrHistory');
    return Sqflite.firstIntValue(result) ?? 0;
  }

  Future<int> deleteAll() async {
    Database db = await instance.database;
    return await db.delete(tableQrHistory);
  }

  Future<int> deleteRecord(int id) async {
    Database db = await instance.database;
    return await db.delete(
      tableQrHistory,
      where: '$columnId = ?',
      whereArgs: [id],
    );
  }

  // --- QUẢN LÝ MÃ ĐÃ TẠO ---

  Future<int> addCreatedRecord(QrRecord record) async {
    Database db = await instance.database;
    return await db.insert(
      tableQrCreated,
      record.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<List<QrRecord>> getAllCreatedRecords() async {
    Database db = await instance.database;
    final List<Map<String, dynamic>> maps = await db.query(
      tableQrCreated,
      orderBy: '$columnId DESC',
    );

    return List.generate(maps.length, (i) {
      return QrRecord.fromMap(maps[i]);
    });
  }

  Future<int> getCreatedCount() async {
    Database db = await instance.database;
    final result = await db.rawQuery('SELECT COUNT(*) as count FROM $tableQrCreated');
    return Sqflite.firstIntValue(result) ?? 0;
  }

  Future<int> deleteAllCreated() async {
    Database db = await instance.database;
    return await db.delete(tableQrCreated);
  }

  Future<int> deleteCreatedRecord(int id) async {
    Database db = await instance.database;
    return await db.delete(
      tableQrCreated,
      where: '$columnId = ?',
      whereArgs: [id],
    );
  }
}
