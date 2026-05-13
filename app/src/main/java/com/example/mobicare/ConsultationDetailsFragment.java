package com.example.mobicare;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class ConsultationDetailsFragment extends Fragment {

    private String consultationId;
    private String patientUid;
    private DatabaseReference mDatabase;

    private TextView tvName, tvDate, tvTime, tvWorker, tvPurpose, tvNotes, tvStatusPill, tvStatusHeader;
    private View headerBackground;
    private Button btnComplete, btnCancel, btnReschedule;
    private LinearLayout layoutButtons;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_consultation_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            consultationId = getArguments().getString("consultationId");
        }

        // 1. Initialize Views
        tvName = view.findViewById(R.id.tvDetailName);
        tvDate = view.findViewById(R.id.tvDetailDate);
        tvTime = view.findViewById(R.id.tvDetailTime);
        tvWorker = view.findViewById(R.id.tvDetailWorker);
        tvPurpose = view.findViewById(R.id.tvDetailPurpose);
        tvNotes = view.findViewById(R.id.tvDetailNotes);
        tvStatusPill = view.findViewById(R.id.tvStatusPill);
        tvStatusHeader = view.findViewById(R.id.tvStatusHeader);
        headerBackground = view.findViewById(R.id.headerBackground);

        btnComplete = view.findViewById(R.id.btnComplete);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnReschedule = view.findViewById(R.id.btnReschedule);
        layoutButtons = view.findViewById(R.id.layoutButtons);

        // 2. Set Up Click Listeners
        btnReschedule.setOnClickListener(v -> showReschedulePickers());
        btnComplete.setOnClickListener(v -> markAsCompleted());
        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().onBackPressed());

        btnCancel.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(getContext())
                    .setTitle("Cancel Appointment")
                    .setMessage("Are you sure you want to cancel this consultation for " + tvName.getText().toString() + "?")
                    .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                        cancelConsultation(); // Only runs if they click Yes
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        // 3. Database Initialization
        if (consultationId != null) {
            mDatabase = FirebaseDatabase.getInstance().getReference().child("Consultations").child(consultationId);
            loadDetails();
            String userId = "";
            if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
                userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
            } else {
                android.content.SharedPreferences prefs = requireContext().getSharedPreferences("MobiCarePrefs", android.content.Context.MODE_PRIVATE);
                userId = prefs.getString("loggedUserKey", "");
            }

            if (!userId.isEmpty()) {
                FirebaseDatabase.getInstance().getReference("HealthWorkers").child(userId).get()
                        .addOnSuccessListener(snapshot -> {
                            if (snapshot.exists()) {
                                String realName = snapshot.child("fullName").getValue(String.class);
                                // This updates the "emma" label to "Maria Santos"
                                tvWorker.setText(realName);
                            }
                        });
            }

            checkUserRoleAndSetupButtons();
        }
    }

    private void loadDetails() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Consultation consult = snapshot.getValue(Consultation.class);
                if (consult != null) {
                    patientUid = consult.patientUid;
                    tvName.setText(consult.patientName);
                    tvDate.setText(consult.date);
                    tvTime.setText(consult.time);
                    tvPurpose.setText(consult.reason);
                    tvWorker.setText(consult.healthWorker);
                    tvNotes.setText(consult.notes);

                    updateUIBasedOnStatus(consult.status);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- BUTTON FUNCTIONS ---

    private void markAsCompleted() {
        mDatabase.child("status").setValue("completed").addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Marked as Completed", Toast.LENGTH_SHORT).show();
            // CHANGE THIS LINE:
            sendDualNotifications("completed", "", "");
        });
    }

    private void cancelConsultation() {
        mDatabase.child("status").setValue("cancelled").addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Consultation Cancelled", Toast.LENGTH_SHORT).show();
            sendAccountNotification("Appointment Cancelled", "Your appointment for " + tvName.getText().toString() + " has been cancelled.");
            requireActivity().onBackPressed();
        });
    }

    private void showReschedulePickers() {
        final Calendar c = Calendar.getInstance();
        DatePickerDialog dpd = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            String newDate = dayOfMonth + "/" + (month + 1) + "/" + year;
            showTimePicker(newDate);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dpd.show();
    }

    private void showTimePicker(String newDate) {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog tpd = new TimePickerDialog(getContext(), (view, hourOfDay, minute1) -> {
            // Convert 24h to 12h format
            String amPm = (hourOfDay >= 12) ? "PM" : "AM";
            int displayHour = (hourOfDay > 12) ? hourOfDay - 12 : hourOfDay;
            if (displayHour == 0) displayHour = 12; // Handle Midnight

            String formattedTime = String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute1, amPm);

            updateAppointment(newDate, formattedTime);
        }, hour, minute, false); // 'false' here sets it to 12-hour mode picker
        tpd.show();
    }
    private void updateAppointment(String date, String time) {
        HashMap<String, Object> update = new HashMap<>();
        update.put("date", date);
        update.put("time", time);
        update.put("status", "scheduled");

        mDatabase.updateChildren(update).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Rescheduled!", Toast.LENGTH_SHORT).show();
            // CHANGE THIS LINE:
            sendDualNotifications("rescheduled", date, time);
        });
    }
    // --- UTILITY METHODS ---

    private void sendDualNotifications(String actionType, String date, String time) {
        // FIX: Get the ID using your new dual-logic
        String workerUid = "";
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            workerUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("MobiCarePrefs", android.content.Context.MODE_PRIVATE);
            workerUid = prefs.getString("loggedUserKey", "");
        }

        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("Notifications");

        // 1. Patient's Perspective
        HashMap<String, Object> patientNotif = new HashMap<>();
        patientNotif.put("receiverUid", patientUid);
        patientNotif.put("senderUid", workerUid);
        patientNotif.put("isRead", false);
        patientNotif.put("timestamp", ServerValue.TIMESTAMP);

        // 2. Maria's Perspective (Health Worker)
        HashMap<String, Object> workerNotif = new HashMap<>();
        workerNotif.put("receiverUid", workerUid); // This sends it to HER list
        workerNotif.put("senderUid", workerUid);
        workerNotif.put("isRead", false);
        workerNotif.put("timestamp", ServerValue.TIMESTAMP);

        if (actionType.equals("rescheduled")) {
            patientNotif.put("title", "Appointment Rescheduled");
            patientNotif.put("message", "Your appointment has been moved to " + date + " at " + time);

            workerNotif.put("title", "Rescheduled Successfully");
            workerNotif.put("message", "You moved " + tvName.getText().toString() + "'s appointment to " + date);
        } else if (actionType.equals("completed")) {
            patientNotif.put("title", "Visit Completed");
            patientNotif.put("message", "Your record for " + tvName.getText().toString() + " is ready.");

            workerNotif.put("title", "Task Finished");
            workerNotif.put("message", "You completed the checkup for " + tvName.getText().toString());
        }

        notifRef.push().setValue(patientNotif);
        notifRef.push().setValue(workerNotif);
    }

    private void checkUserRoleAndSetupButtons() {
        String userId = "";
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("MobiCarePrefs", android.content.Context.MODE_PRIVATE);
            userId = prefs.getString("loggedUserKey", "");
        }

        if (userId.isEmpty()) return;

        FirebaseDatabase.getInstance().getReference("HealthWorkers").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            btnComplete.setVisibility(View.VISIBLE);
                            btnReschedule.setVisibility(View.VISIBLE);
                            btnCancel.setVisibility(View.VISIBLE);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
    private void updateUIBasedOnStatus(String status) {
        if ("completed".equalsIgnoreCase(status)) {
            headerBackground.setBackgroundResource(R.color.healing_green);
            tvStatusPill.setText("COMPLETED");
            tvStatusPill.setBackgroundResource(R.drawable.badge_green);
            tvStatusPill.setTextColor(Color.parseColor("#2E7D32"));
            tvStatusHeader.setText("Completed Visit");
            layoutButtons.setVisibility(View.GONE);
        } else if ("cancelled".equalsIgnoreCase(status)) {
            // New logic for Cancelled status
            headerBackground.setBackgroundResource(R.color.cool_grey); // Or a light red
            tvStatusPill.setText("CANCELLED");
            tvStatusPill.setBackgroundResource(R.drawable.badge_grey);
            tvStatusPill.setTextColor(Color.GRAY);
            tvStatusHeader.setText("Cancelled Appointment");
            layoutButtons.setVisibility(View.GONE); // Hide buttons so they can't interact
        } else {
            headerBackground.setBackgroundResource(R.color.trust_blue);
            tvStatusPill.setText("SCHEDULED");
            tvStatusPill.setBackgroundResource(R.drawable.badge_blue);
            tvStatusHeader.setText("Upcoming appointment");
            tvStatusPill.setTextColor(Color.parseColor("#1B75BC"));
            layoutButtons.setVisibility(View.VISIBLE);
        }
    }
    private void sendAccountNotification(String title, String message) {
        if (patientUid == null) return;

        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("Notifications").push();

        HashMap<String, Object> notifData = new HashMap<>();
        notifData.put("title", title);
        notifData.put("message", message);
        notifData.put("receiverUid", patientUid);
        notifData.put("isRead", false);
        notifData.put("timestamp", ServerValue.TIMESTAMP);

        notifRef.setValue(notifData).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to send notification", Toast.LENGTH_SHORT).show();
        });
    }
}