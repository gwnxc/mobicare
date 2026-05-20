package com.example.mobicare;

public class Notification {
    public String id;
    public String title;
    public String message;
    public String date; // Keep this if you use it for UI
    public String type;
    public String receiverUid;
    public Boolean isRead;
    public long timestamp; // ADD THIS to match your Firebase data

    public Notification() {}

    public Notification(String title, String message, String date, String type, String receiverUid, long timestamp) {
        this.title = title;
        this.message = message;
        this.date = date;
        this.type = type;
        this.receiverUid = receiverUid;
        this.isRead = false;
        this.timestamp = timestamp;
    }
}