package com.example.mobicare;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Context; // Needed for Context//
import com.google.firebase.auth.FirebaseAuth; // Added
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query; // Added
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.text.SimpleDateFormat; // Needed for SimpleDateFormat
import java.util.Date; // Needed for Date
import java.util.Locale; // Needed for Locale
import java.util.concurrent.TimeUnit; // Needed for TimeUnit

public class AlertsFragment extends Fragment {

    private RecyclerView rvNotifications;
    private TextView tvSubtitle;
    private DatabaseReference mDatabase;
    private List<Notification> notificationList = new ArrayList<>();
    private NotificationAdapter adapter;
    private String currentUid; // Added to store current user ID

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alerts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String userId = "";

        // 1. Check if a real Firebase Auth user exists (For Patients/Guardians)
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        // 2. If not, check SharedPreferences (For Health Workers like Maria)
        else {
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("MobiCarePrefs", android.content.Context.MODE_PRIVATE);
            userId = prefs.getString("loggedUserKey", "");
        }

        // 3. SET THE GLOBAL VARIABLE
        this.currentUid = userId;

        // !!! DELETE OR COMMENT OUT THIS LINE BELOW !!!
        // currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        rvNotifications = view.findViewById(R.id.rvNotifications);
        tvSubtitle = view.findViewById(R.id.tvSubtitle);
        ImageView btnBack = view.findViewById(R.id.btnBackAlerts);

        mDatabase = FirebaseDatabase.getInstance().getReference("Notifications");

        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(adapter);

        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Only load if we actually have a valid ID
        // Inside onViewCreated, under loadNotifications()
        if (currentUid != null && !currentUid.isEmpty()) {
            loadNotifications();
            checkConsultationAlerts(); // Add this line!
        }
        // ---> FIXED: INITIALIZE NAVIGATION SYSTEM FOR ALERTS CONTEXT SCREEN RIGHT HERE <---
        setupHealthWorkerNavigation(view, "alerts");
    }

    private void loadNotifications() {
        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("Notifications");

        // 1. Get role to decide the "Target"
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("MobiCarePrefs", Context.MODE_PRIVATE);
        String userRole = prefs.getString("userRole", "Patient");

        // 2. Define the target receiver
        String targetReceiver = "Patient".equalsIgnoreCase(userRole) ? currentUid : "HealthWorker";

        // 3. Query based on the target
        Query query = notifRef.orderByChild("receiverUid").equalTo(targetReceiver);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                notificationList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Notification notification = ds.getValue(Notification.class);
                    if (notification != null) {
                        notification.id = ds.getKey();
                        notificationList.add(notification);
                    }
                }
                // Sort by timestamp if needed
                Collections.reverse(notificationList);
                adapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateSubtitle(int count) {
        if (tvSubtitle != null) {
            tvSubtitle.setText(count + " unread notifications");
        }
    }

    // Optional: If you want everything marked as read when they EXIT the screen
    @Override
    public void onPause() {
        super.onPause();
        // markAllAsRead(); // Uncomment if you want this behavior
    }

    private void markAllAsRead() {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("MobiCarePrefs", Context.MODE_PRIVATE);
        String userRole = prefs.getString("userRole", "Patient");

        Query query = "Patient".equalsIgnoreCase(userRole)
                ? mDatabase.orderByChild("receiverUid").equalTo(currentUid)
                : mDatabase.orderByChild("receiverUid").equalTo("Admin");

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (Boolean.FALSE.equals(ds.child("isRead").getValue(Boolean.class))) {
                        ds.getRef().child("isRead").setValue(true);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    private void setupHealthWorkerNavigation(View view, String activeTab) {
        View navBarContainer = view.findViewById(R.id.healthWorkerNavBar);
        if (navBarContainer == null) return;

        LinearLayout customNavHome = navBarContainer.findViewById(R.id.nav_home);
        LinearLayout customNavConsultations = navBarContainer.findViewById(R.id.nav_consultations_tab);
        LinearLayout customNavAddRecord = navBarContainer.findViewById(R.id.nav_add_record_tab);
        LinearLayout customNavNotifications = navBarContainer.findViewById(R.id.nav_notifications_tab);
        LinearLayout customNavProfile = navBarContainer.findViewById(R.id.nav_profile_tab);

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

        if (customNavHome != null) {
            customNavHome.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.healthWorkerDashboardFragment));
        }

        if (customNavConsultations != null) {
            customNavConsultations.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.consultationsFragment));
        }

        if (customNavAddRecord != null) {
            customNavAddRecord.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.addConsultationFragment));
        }

        if (customNavNotifications != null) {
            customNavNotifications.setOnClickListener(v -> {
                if (!activeTab.equals("alerts")) {
                    Navigation.findNavController(view).navigate(R.id.alertsFragment);
                }
            });
        }

        if (customNavProfile != null) {
            customNavProfile.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.profileFragment));
        }
    }
    private void checkConsultationAlerts() {
        DatabaseReference consultRef = FirebaseDatabase.getInstance().getReference("Consultations");
        consultRef.orderByChild("patientUid").equalTo(currentUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String date = ds.child("date").getValue(String.class);
                            if (date != null && isDateNear(date)) {
                                // TRIGGER UI ALERT OR NOTIFICATION
                                showNotification("Consultation Alert", "You have a consultation on " + date);
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
    private boolean isDateNear(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date targetDate = sdf.parse(dateStr);
            Date today = new Date();

            // Calculate difference in days
            long diffInMillies = Math.abs(targetDate.getTime() - today.getTime());
            long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

            // Return true if it's within 3 days (or any threshold you prefer)
            return diffInDays <= 3 && targetDate.after(today);
        } catch (Exception e) {
            return false;
        }
    }
    private void showNotification(String title, String message) {
        // This is a simple Toast for now to demonstrate the alert
        android.widget.Toast.makeText(getContext(), title + ": " + message, android.widget.Toast.LENGTH_LONG).show();
    }
    private void highlightActiveTab(View parentView, int iconResId, int textResId) {
        ImageView icon = parentView.findViewById(iconResId);
        TextView text = parentView.findViewById(textResId);
        if (icon != null) icon.setColorFilter(Color.parseColor("#2D79D1"));
        if (text != null) {
            text.setTextColor(Color.parseColor("#2D79D1"));
            text.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }
}