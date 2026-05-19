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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ConsultationsFragment extends Fragment {

    private LinearLayout llUpcoming, llCompleted;
    private TextView tvUpcomingCount, tvCompletedCount;
    private DatabaseReference mDatabase;
    private String currentUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_consultations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Acknowledging our Auth/Prefs Check for Maria/Health Workers
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            SharedPreferences prefs = requireContext().getSharedPreferences("MobiCarePrefs", Context.MODE_PRIVATE);
            currentUid = prefs.getString("loggedUserKey", "");
        }

        // 2. Link the UI Containers from your XML
        llUpcoming = view.findViewById(R.id.llUpcoming);
        llCompleted = view.findViewById(R.id.llCompleted);
        tvUpcomingCount = view.findViewById(R.id.tvUpcomingCount);
        tvCompletedCount = view.findViewById(R.id.tvCompletedCount);

        // 3. Back Button (Matches R.id.btnBack in your XML)
        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        }

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Consultations");
        fetchConsultations();

        setupHealthWorkerNavigation(view, "consultations");
    }

    private void fetchConsultations() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                llUpcoming.removeAllViews();
                llCompleted.removeAllViews();

                int upcoming = 0;
                int completed = 0;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Consultation c = ds.getValue(Consultation.class);
                    if (c != null) {
                        c.id = ds.getKey();

                        // Sort into the correct container based on status
                        if ("scheduled".equalsIgnoreCase(c.status) || "pending".equalsIgnoreCase(c.status)) {
                            upcoming++;
                            addConsultationCardToUI(llUpcoming, c);
                        } else {
                            completed++;
                            addConsultationCardToUI(llCompleted, c);
                        }
                    }
                }

                // Update the counters in your XML
                tvUpcomingCount.setText("Upcoming (" + upcoming + ")");
                tvCompletedCount.setText("Completed (" + completed + ")");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addConsultationCardToUI(LinearLayout container, Consultation consultation) {
        View card = getLayoutInflater().inflate(R.layout.item_consultation_card, container, false);

        // Bind existing fields
        TextView tvName = card.findViewById(R.id.tvPatientName);
        TextView tvType = card.findViewById(R.id.tvPatientType);
        TextView tvPurpose = card.findViewById(R.id.tvPurpose);
        TextView tvDateTime = card.findViewById(R.id.tvDateTime);
        TextView tvWorker = card.findViewById(R.id.tvWorker);

        // Bind the new Status Badge
        TextView tvStatusBadge = card.findViewById(R.id.tvStatusBadge);

        // Set Text
        if (tvName != null) tvName.setText(consultation.patientName);
        if (tvType != null) tvType.setText(consultation.patientType);
        if (tvPurpose != null) tvPurpose.setText("Purpose: " + consultation.reason);
        if (tvDateTime != null) tvDateTime.setText(consultation.date + " at " + consultation.time);
        if (tvWorker != null) tvWorker.setText("Assigned to: " + consultation.healthWorker);

        // Set Status Badge Logic
        if (tvStatusBadge != null) {
            tvStatusBadge.setText(consultation.status != null ? consultation.status.toUpperCase() : "PENDING");

            // Color coding
            if ("completed".equalsIgnoreCase(consultation.status)) {
                tvStatusBadge.setTextColor(Color.parseColor("#388E3C")); // Green
            } else if ("cancelled".equalsIgnoreCase(consultation.status)) {
                tvStatusBadge.setTextColor(Color.parseColor("#D32F2F")); // Red
            } else {
                tvStatusBadge.setTextColor(Color.parseColor("#1976D2")); // Blue
            }
        }

        card.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("consultationId", consultation.id);
            bundle.putString("status", consultation.status);
            Navigation.findNavController(requireView()).navigate(R.id.consultationDetailsFragment, bundle);
        });

        container.addView(card);
    }
    private void setupHealthWorkerNavigation(View view, String activeTab) {
        // 1. Find the master navigation container element first
        View navBarContainer = view.findViewById(R.id.healthWorkerNavBar);
        if (navBarContainer == null) return; // Prevent null pointer crashes

        // 2. Query elements INSIDE the parent container scope to resolve the symbol paths
        LinearLayout customNavHome = navBarContainer.findViewById(R.id.nav_home);
        LinearLayout customNavConsultations = navBarContainer.findViewById(R.id.nav_consultations_tab);
        LinearLayout customNavAddRecord = navBarContainer.findViewById(R.id.nav_add_record_tab);
        LinearLayout customNavNotifications = navBarContainer.findViewById(R.id.nav_notifications_tab);
        LinearLayout customNavProfile = navBarContainer.findViewById(R.id.nav_profile_tab);

        // 3. Apply active color adjustments directly to the layout reference scope
        if (activeTab.equals("home")) {
            highlightActiveTab(navBarContainer, R.id.iv_nav_home, R.id.tv_nav_home);
        } else if (activeTab.equals("consultations")) {
            highlightActiveTab(navBarContainer, R.id.iv_nav_consultations, R.id.tv_nav_consultations);
        } else if (activeTab.equals("add")) {
            highlightActiveTab(navBarContainer, R.id.iv_nav_add, R.id.tv_nav_add);
        } else if (activeTab.equals("alerts")) {
            highlightActiveTab(navBarContainer, R.id.iv_nav_alerts, R.id.tv_nav_alerts);
        } else if (activeTab.equals("profile")) {
            highlightActiveTab(navBarContainer, R.id.iv_nav_profile, R.id.tv_nav_profile);
        }

        // 4. Click routing rules using exact nav_graph destination targets
        if (customNavHome != null) {
            customNavHome.setOnClickListener(v -> {
                if (!activeTab.equals("home")) {
                    Navigation.findNavController(view).navigate(R.id.healthWorkerDashboardFragment);
                }
            });
        }

        if (customNavConsultations != null) {
            customNavConsultations.setOnClickListener(v -> {
                if (!activeTab.equals("consultations")) {
                    Navigation.findNavController(view).navigate(R.id.consultationsFragment);
                }
            });
        }

        if (customNavAddRecord != null) {
            customNavAddRecord.setOnClickListener(v -> {
                if (!activeTab.equals("add")) {
                    Navigation.findNavController(view).navigate(R.id.addConsultationFragment);
                }
            });
        }

        if (customNavNotifications != null) {
            customNavNotifications.setOnClickListener(v -> {
                if (!activeTab.equals("alerts")) {
                    Navigation.findNavController(view).navigate(R.id.alertsFragment);
                }
            });
        }

        if (customNavProfile != null) {
            customNavProfile.setOnClickListener(v -> {
                if (!activeTab.equals("profile")) {
                    Navigation.findNavController(view).navigate(R.id.profileFragment);
                }
            });
        }
    }

    private void highlightActiveTab(View parentView, int iconResId, int textResId) {
        ImageView icon = parentView.findViewById(iconResId);
        TextView text = parentView.findViewById(textResId);
        if (icon != null) icon.setColorFilter(Color.parseColor("#2D79D1")); // Colors it active blue
        if (text != null) {
            text.setTextColor(Color.parseColor("#2D79D1"));
            text.setTypeface(null, android.graphics.Typeface.BOLD); // Bolds the active screen label
        }
    }
}