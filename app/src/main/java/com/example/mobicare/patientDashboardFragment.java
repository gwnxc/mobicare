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

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class patientDashboardFragment extends Fragment {

    private String loggedInUserId;
    private String motherFullName = "";

    private DatabaseReference mDatabaseConsultations;
    private List<ActivityEntry> masterActivityList = new ArrayList<>();
    private LinearLayout llActivityLogList;
    private TextView tvRecentActivityLabel;

    public patientDashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Check who is logged in!
        SharedPreferences prefs = requireActivity().getSharedPreferences("MobicarePrefs", Context.MODE_PRIVATE);
        loggedInUserId = prefs.getString("loggedInUser", null);

        if (loggedInUserId == null) {
            Toast.makeText(getContext(), "Session error. Please log in again.", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigate(R.id.loginFragment);
            return;
        }

        // --- Custom Bottom Navigation Logic for Patients ---
        LinearLayout navHome = view.findViewById(R.id.nav_home);
        LinearLayout navAlerts = view.findViewById(R.id.nav_alerts);
        LinearLayout navProfile = view.findViewById(R.id.nav_profile);

        if (navHome != null) navHome.setOnClickListener(v -> {}); // Already here

        if (navAlerts != null) {
            navAlerts.setOnClickListener(v -> {
                // Hide the badge if it was showing
                TextView tvBottomAlertBadge = view.findViewById(R.id.tvBottomAlertBadge);
                if (tvBottomAlertBadge != null) tvBottomAlertBadge.setVisibility(View.GONE);

                // Navigate to the new fragment
                Navigation.findNavController(view).navigate(R.id.action_patientDashboardFragment_to_patientAlertsFragment);
            });
        }

        // ---> FIXED: Profile navigation properly placed inside onViewCreated <---
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.action_patientDashboardFragment_to_patientProfileFragment);
            });
        }

        // --- Quick Action Cards ---
        MaterialCardView cvMyChildren = view.findViewById(R.id.cvMyChildren);
        if (cvMyChildren != null) cvMyChildren.setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.action_patientDashboardFragment_to_myChildrenFragment);
        });

        MaterialCardView cvConsultations = view.findViewById(R.id.cvConsultations);
        if (cvConsultations != null) {
            cvConsultations.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.action_patientDashboardFragment_to_patientConsultationsFragment);
            });
        }

        com.google.android.material.card.MaterialCardView cvMyRecord = view.findViewById(R.id.cvMyRecord);
        if (cvMyRecord != null) {
            cvMyRecord.setOnClickListener(v -> {
                // ADDED: Create a bundle parameter to safely forward identity metrics
                Bundle args = new Bundle();
                args.putString("selectedChildId", loggedInUserId);
                args.putString("selectedChildName", motherFullName);

                Navigation.findNavController(view).navigate(
                        R.id.action_patientDashboardFragment_to_myRecordFragment,
                        args
                );
            });
        }

        // --- UI Hooks ---
        llActivityLogList = view.findViewById(R.id.llActivityItems);
        tvRecentActivityLabel = view.findViewById(R.id.tvRecentActivityLabel);

        // Fetch User Data and update dashboard!
        fetchUserData(view);
        loadUpcomingVisits();
    }

    private void fetchUserData(View view) {
        DatabaseReference motherRef = FirebaseDatabase.getInstance().getReference("Patients_Guardians").child(loggedInUserId);

        motherRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Grab the fullName from the guardian node
                    motherFullName = snapshot.child("fullName").getValue(String.class);
                    TextView tvUserName = view.findViewById(R.id.tvUserName);
                    if (tvUserName != null && motherFullName != null) {
                        tvUserName.setText(motherFullName);
                    }

                    setupTopSummaryBadges(view);
                    fetchRecentActivities();
                } else {
                    Toast.makeText(getContext(), "User profile not found in database", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupTopSummaryBadges(View view) {
        TextView tvChildrenCount = view.findViewById(R.id.tvChildrenCount);
        TextView tvUpcomingCount = view.findViewById(R.id.tvUpcomingCount);
        TextView tvAlertBadgeCount = view.findViewById(R.id.tvAlertBadgeCount);

        // 1. Children Count
        // 1. Children Count
        if (tvChildrenCount != null) {
            FirebaseDatabase.getInstance().getReference("Patients_Children")
                    .orderByChild("parentUid").equalTo(loggedInUserId) // ---> FIXED KEY HERE <---
                    .addValueEventListener(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                            tvChildrenCount.setText(String.valueOf(snapshot.getChildrenCount()));
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }

        // 2. Upcoming Consultations Count
        if (tvUpcomingCount != null && !motherFullName.isEmpty()) {
            FirebaseDatabase.getInstance().getReference("Consultations")
                    .orderByChild("patientName").equalTo(motherFullName)
                    .addValueEventListener(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                            int upcoming = 0;
                            long currentTime = System.currentTimeMillis();
                            for (DataSnapshot snap : snapshot.getChildren()) {
                                Long ts = snap.child("timestamp").getValue(Long.class);
                                if (ts != null && ts > currentTime) upcoming++;
                            }
                            tvUpcomingCount.setText(String.valueOf(upcoming));
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }
    }

    private void fetchRecentActivities() {
        masterActivityList.clear();
        mDatabaseConsultations = FirebaseDatabase.getInstance().getReference("Consultations");

        mDatabaseConsultations.orderByChild("patientName").equalTo(motherFullName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long currentTime = System.currentTimeMillis();
                        for (DataSnapshot consultSnap : snapshot.getChildren()) {
                            String purpose = consultSnap.child("purpose").getValue(String.class);
                            String date = consultSnap.child("date").getValue(String.class);
                            String time = consultSnap.child("time").getValue(String.class);
                            Long consultTimestamp = consultSnap.child("timestamp").getValue(Long.class);

                            if (consultTimestamp != null) {
                                String status = consultTimestamp > currentTime ? "scheduled" : "completed";
                                String formattedTimeText = date + " at " + time;
                                masterActivityList.add(new ActivityEntry(motherFullName, purpose, formattedTimeText, status, consultTimestamp));
                            }
                        }
                        updateUnifiedLogUI();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        updateUnifiedLogUI();
                    }
                });
    }

    private void updateUnifiedLogUI() {
        if (llActivityLogList == null) return;
        llActivityLogList.removeAllViews();

        if (masterActivityList.isEmpty()) {
            if (tvRecentActivityLabel != null) {
                tvRecentActivityLabel.setText("Recent Activity (No recent activity)");
                tvRecentActivityLabel.setTextSize(16);
                tvRecentActivityLabel.setTextColor(Color.GRAY);
            }
            return;
        }

        if (tvRecentActivityLabel != null) {
            tvRecentActivityLabel.setText("Recent Activity");
            tvRecentActivityLabel.setTextSize(20);
            tvRecentActivityLabel.setTextColor(getResources().getColor(R.color.slate_navy, null));
        }

        Collections.sort(masterActivityList, (a, b) -> Long.compare(b.timestamp, a.timestamp));

        int maxItemsToShow = Math.min(3, masterActivityList.size());
        List<ActivityEntry> recentThree = masterActivityList.subList(0, maxItemsToShow);

        for (ActivityEntry entry : recentThree) {
            View card = getLayoutInflater().inflate(R.layout.item_activity_log_card, null);
            populateActivityCard(card, entry);
            llActivityLogList.addView(card);
        }
    }

    private void populateActivityCard(View card, ActivityEntry entry) {
        TextView tvPatientName = card.findViewById(R.id.tvPatientName);
        TextView tvActivityDesc = card.findViewById(R.id.tvActivityDesc);
        TextView tvActivityTime = card.findViewById(R.id.tvActivityTime);
        TextView tvStatusBadge = card.findViewById(R.id.tvStatusBadge);
        ImageView ivActivityIcon = card.findViewById(R.id.ivActivityIcon);

        tvPatientName.setText(entry.name);
        tvActivityDesc.setText(entry.description);
        tvActivityTime.setText(entry.formattedTime);
        tvStatusBadge.setText(entry.status.toUpperCase());

        switch (entry.status) {
            case "scheduled":
                tvStatusBadge.setBackgroundResource(R.drawable.badge_blue_light);
                tvStatusBadge.setTextColor(Color.rgb(0x00, 0x4D, 0xAA));
                ivActivityIcon.setImageResource(android.R.drawable.ic_menu_today);
                ivActivityIcon.setColorFilter(tvStatusBadge.getCurrentTextColor());
                break;
            case "completed":
                tvStatusBadge.setBackgroundResource(R.drawable.badge_green_light);
                tvStatusBadge.setTextColor(Color.rgb(0x1B, 0x5E, 0x20));
                ivActivityIcon.setImageResource(android.R.drawable.ic_menu_today);
                ivActivityIcon.setColorFilter(tvStatusBadge.getCurrentTextColor());
                break;
        }
    }
    private void loadUpcomingVisits() {
        // 1. Use a Query with a limit or specific index if possible
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Consultations");

        ref.orderByChild("patientUid").equalTo(loggedInUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Run the list building on the background-intent if the snapshot is huge,
                        // but for now, just keep it clean:
                        List<String> visitDates = new ArrayList<>();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            String date = data.child("date").getValue(String.class);
                            if (date != null) visitDates.add(date);
                        }

                        // Use 'post' to ensure UI updates happen after the current cycle
                        View view = getView();
                        if (view != null) {
                            view.post(() -> setupCalendarListener(visitDates));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupCalendarListener(List<String> visitDates) {
        android.widget.CalendarView calendarView = getView().findViewById(R.id.dashboardCalendar);

        if (calendarView != null) {
            // Use a simple guard to prevent multiple attachments
            calendarView.setOnDateChangeListener(null);

            calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
                // Use local variables to avoid heavy object creation
                String selectedDate = String.format("%02d/%02d/%d", dayOfMonth, (month + 1), year);

                if (visitDates.contains(selectedDate)) {
                    Toast.makeText(getContext(), "Scheduled visit on " + selectedDate, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "No scheduled visits.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    class ActivityEntry {
        String name, description, formattedTime, status;
        long timestamp;

        ActivityEntry(String name, String description, String formattedTime, String status, long timestamp) {
            this.name = name;
            this.description = description;
            this.formattedTime = formattedTime;
            this.status = status;
            this.timestamp = timestamp;
        }
    }
}