package com.example.mobicare;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class patientConsultationsFragment extends Fragment {

    private String loggedInUserId;
    private LinearLayout llUpcoming, llCompleted;
    private TextView tvUpcomingHeader, tvCompletedHeader;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Loads your newly named layout file
        return inflater.inflate(R.layout.fragment_patient_consultations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireActivity().getSharedPreferences("MobicarePrefs", Context.MODE_PRIVATE);
        loggedInUserId = prefs.getString("loggedInUser", null);

        ImageView btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        llUpcoming = view.findViewById(R.id.llUpcoming);
        llCompleted = view.findViewById(R.id.llCompleted);
        tvUpcomingHeader = view.findViewById(R.id.tvUpcomingHeader);
        tvCompletedHeader = view.findViewById(R.id.tvCompletedHeader);

        if (loggedInUserId != null) {
            fetchConsultations();
        }
    }

    private void fetchConsultations() {
        DatabaseReference consultationsRef = FirebaseDatabase.getInstance().getReference("Consultations");

        consultationsRef.orderByChild("patientUid").equalTo(loggedInUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Safety check to prevent crashes if the user leaves the screen quickly
                if (!isAdded()) return;

                if (llUpcoming != null) llUpcoming.removeAllViews();
                if (llCompleted != null) llCompleted.removeAllViews();

                int upcomingCount = 0;
                int completedCount = 0;

                for (DataSnapshot consultSnap : snapshot.getChildren()) {
                    String status = consultSnap.child("status").getValue(String.class);
                    String patientName = consultSnap.child("patientName").getValue(String.class);
                    String reason = consultSnap.child("reason").getValue(String.class);
                    String date = consultSnap.child("date").getValue(String.class);
                    String time = consultSnap.child("time").getValue(String.class);
                    String healthWorker = consultSnap.child("healthWorker").getValue(String.class);

                    if (status != null && status.equalsIgnoreCase("scheduled")) {
                        upcomingCount++;
                        // For upcoming, we show Time instead of Health Worker
                        addCardToUI(llUpcoming, patientName, reason, date, time, status, true);
                    } else if (status != null && status.equalsIgnoreCase("completed")) {
                        completedCount++;
                        // For completed, we show the Health Worker instead of Time
                        addCardToUI(llCompleted, patientName, reason, date, healthWorker, status, false);
                    }
                }

                if (tvUpcomingHeader != null) tvUpcomingHeader.setText("Upcoming (" + upcomingCount + ")");
                if (tvCompletedHeader != null) tvCompletedHeader.setText("Completed (" + completedCount + ")");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load consultations", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void addCardToUI(LinearLayout targetLayout, String name, String reason, String date, String secondaryInfo, String status, boolean isUpcoming) {
        if (getContext() == null || targetLayout == null) return;

        // ---> CRITICAL FIX: Passed targetLayout and false to keep your 24dp margin! <---
        View card = getLayoutInflater().inflate(R.layout.item_patient_consultation_card, targetLayout, false);

        ImageView ivIcon = card.findViewById(R.id.ivIcon);
        TextView tvName = card.findViewById(R.id.tvPatientName);
        TextView tvReason = card.findViewById(R.id.tvReason);
        TextView tvBadge = card.findViewById(R.id.tvStatusBadge);
        TextView tvDate = card.findViewById(R.id.tvDate);
        ImageView ivSecIcon = card.findViewById(R.id.ivSecondaryIcon);
        TextView tvSecText = card.findViewById(R.id.tvSecondaryText);

        if (tvName != null) tvName.setText(name != null ? name : "Unknown Patient");
        if (tvReason != null) tvReason.setText(reason != null ? reason : "Consultation");
        if (tvDate != null) tvDate.setText(date != null ? date : "--");
        if (tvBadge != null) tvBadge.setText(status != null ? status : "");

        if (isUpcoming) {
            // Setup Blue Theme
            if (ivIcon != null) {
                ivIcon.setImageResource(android.R.drawable.ic_menu_today);
                ivIcon.setBackgroundResource(R.drawable.circle_light_blue);
                ivIcon.setColorFilter(Color.parseColor("#1976D2"));
            }

            if (tvBadge != null) {
                tvBadge.setBackgroundResource(R.drawable.badge_blue_light);
                tvBadge.setTextColor(Color.parseColor("#1976D2"));
            }

            if (ivSecIcon != null) ivSecIcon.setImageResource(android.R.drawable.ic_menu_recent_history);
            if (tvSecText != null) tvSecText.setText(secondaryInfo != null && !secondaryInfo.isEmpty() ? secondaryInfo : "Time TBD");
        } else {
            // Setup Green Theme
            if (ivIcon != null) {
                ivIcon.setImageResource(android.R.drawable.ic_menu_edit);
                ivIcon.setBackgroundResource(R.drawable.badge_green_light);
                ivIcon.setColorFilter(Color.parseColor("#388E3C"));
            }

            if (tvBadge != null) {
                tvBadge.setBackgroundResource(R.drawable.badge_green_light);
                tvBadge.setTextColor(Color.parseColor("#388E3C"));
            }

            if (ivSecIcon != null) ivSecIcon.setImageResource(android.R.drawable.ic_menu_info_details);
            if (tvSecText != null) tvSecText.setText(secondaryInfo != null && !secondaryInfo.isEmpty() ? secondaryInfo : "Health Worker");
        }

        targetLayout.addView(card);
    }
}