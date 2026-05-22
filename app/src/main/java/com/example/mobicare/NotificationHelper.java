package com.example.mobicare;

import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class NotificationHelper {
    public static void sendPatientNotification(String patientUid, String title, String message, String type) {
        String notifId = FirebaseDatabase.getInstance().getReference("Notifications").push().getKey();
        Map<String, Object> data = new HashMap<>();
        data.put("id", notifId);
        data.put("title", title);
        data.put("message", message);
        data.put("type", type);
        data.put("receiverUid", patientUid);
        data.put("isRead", false);
        data.put("timestamp", System.currentTimeMillis());

        if (notifId != null) {
            FirebaseDatabase.getInstance().getReference("Notifications").child(notifId).setValue(data);
        }
    }
}