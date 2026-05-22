package com.example.mobicare;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Notification {
    public String id;
    public String title;
    public String message;
    public String date;
    public String type;
    public String receiverUid;
    public Boolean isRead;
    public long timestamp;

    public Notification() {}

    // Add a getter to ensure the UI always gets a string
    public String getDisplayDate() {
        if (date != null && !date.isEmpty()) return date;
        if (timestamp > 0) {
            return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(timestamp));
        }
        return "";
    }
}