package com.demo.qr;

public class QrRecord {
    private int id;
    private String content;
    private String type;
    private String timestamp;
    private boolean isCreated = false;

    public QrRecord() {}

    public QrRecord(int id, String content, String type, String timestamp) {
        this(id, content, type, timestamp, false);
    }

    public QrRecord(int id, String content, String type, String timestamp, boolean isCreated) {
        this.id = id;
        this.content = content;
        this.type = type;
        this.timestamp = timestamp;
        this.isCreated = isCreated;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isCreated() {
        return isCreated;
    }

    public void setCreated(boolean created) {
        isCreated = created;
    }
}
