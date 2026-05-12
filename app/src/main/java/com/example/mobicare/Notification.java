package com.example.mobicare;

public class Notification {
    public String id;
    public String title;
    public String message;
    public String date;
    public String type;
    public String receiverUid;
    public Boolean isRead; // Change 'boolean' to 'Boolean'

    public Notification() {}

    public Notification(String title, String message, String date, String type, String receiverUid) {
        this.title = title;
        this.message = message;
        this.date = date;
        this.type = type;
        this.receiverUid = receiverUid;
        this.isRead = false;
    }
}