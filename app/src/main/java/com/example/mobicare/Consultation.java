package com.example.mobicare;

public class Consultation {
    // Add patientUid and id to the list
    public String id, patientUid;
    public String patientName, reason, date, time, healthWorker, notes, status, type;

    public Consultation() {}

    public Consultation(String patientName, String reason, String date, String time, String healthWorker, String notes, String status, String type, String patientUid) {
        this.patientName = patientName;
        this.reason = reason;
        this.date = date;
        this.time = time;
        this.healthWorker = healthWorker;
        this.notes = notes;
        this.status = status;
        this.type = type;
        this.patientUid = patientUid; // Initialize the UID
    }
}