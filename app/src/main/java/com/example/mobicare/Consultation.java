package com.example.mobicare;

public class Consultation {
    // Added patientType to match your Firebase query logic
    public String id, patientUid;
    public String patientName, reason, date, time, healthWorker, notes, status, patientType;

    public Consultation() {} // Required for Firebase

    public Consultation(String patientName, String reason, String date, String time,
                        String healthWorker, String notes, String status,
                        String patientType, String patientUid) {
        this.patientName = patientName;
        this.reason = reason;
        this.date = date;
        this.time = time;
        this.healthWorker = healthWorker;
        this.notes = notes;
        this.status = status;
        this.patientType = patientType; // Corrected field name
        this.patientUid = patientUid;
    }
}