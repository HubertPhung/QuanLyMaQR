class QrRecord {
  final int? id;
  final String content;
  final String type;
  final String timestamp;
  final bool isCreated;

  QrRecord({
    this.id,
    required this.content,
    required this.type,
    required this.timestamp,
    this.isCreated = false,
  });

  // Chuyển đổi đối tượng QrRecord thành Map để lưu vào SQLite
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'content': content,
      'type': type,
      'timestamp': timestamp,
    };
  }

  // Tạo đối tượng QrRecord từ Map (khi đọc từ SQLite)
  factory QrRecord.fromMap(Map<String, dynamic> map, {bool isCreated = false}) {
    return QrRecord(
      id: map['id'] as int?,
      content: map['content'] as String,
      type: map['type'] as String,
      timestamp: map['timestamp'] as String,
      isCreated: isCreated,
    );
  }

  QrRecord copyWith({
    int? id,
    String? content,
    String? type,
    String? timestamp,
    bool? isCreated,
  }) {
    return QrRecord(
      id: id ?? this.id,
      content: content ?? this.content,
      type: type ?? this.type,
      timestamp: timestamp ?? this.timestamp,
      isCreated: isCreated ?? this.isCreated,
    );
  }
}
