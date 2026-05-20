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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class patientAlertsFragment extends Fragment {

    private String loggedInUserId;
    private LinearLayout llNewNotifs, llEarlierNotifs;
    private TextView tvNewLabel, tvEarlierLabel, tvUnreadCountLabel, tvEmptyState;

    private List<NotifEntry> upcomingList = new ArrayList<>();
    private List<NotifEntry> pastList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_alerts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireActivity().getSharedPreferences("MobicarePrefs", Context.MODE_PRIVATE);
        loggedInUserId = prefs.getString("loggedInUser", null);

        ImageView btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        llNewNotifs = view.findViewById(R.id.llNewNotifs);
        llEarlierNotifs = view.findViewById(R.id.llEarlierNotifs);
        tvNewLabel = view.findViewById(R.id.tvNewLabel);
        tvEarlierLabel = view.findViewById(R.id.tvEarlierLabel);
        tvUnreadCountLabel = view.findViewById(R.id.tvUnreadCountLabel);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        // Update labels to fit the medical context
        if (tvNewLabel != null) tvNewLabel.setText("Upcoming Appointments");
        if (tvEarlierLabel != null) tvEarlierLabel.setText("Past Records");

        // --- Bottom Navigation Setup ---
        setupBottomNavigation(view);

        if (loggedInUserId != null) {
            // Now fetches from your actual records instead of the empty notifications folder
            fetchMedicalRecords();
        }
    }

    private void setupBottomNavigation(View view) {
        LinearLayout navHome = view.findViewById(R.id.nav_home);
        LinearLayout navAlerts = view.findViewById(R.id.nav_alerts);
        LinearLayout navProfile = view.findViewById(R.id.nav_profile);

        int activeColor = Color.parseColor("#155A91");
        int inactiveColor = Color.parseColor("#8E8E8E");

        if (navHome != null) {
            ((ImageView) navHome.getChildAt(0)).setColorFilter(inactiveColor);
            ((TextView) navHome.getChildAt(1)).setTextColor(inactiveColor);
            navHome.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.patientDashboardFragment));
        }

        if (navAlerts != null) {
            View alertsIconContainer = navAlerts.getChildAt(0);
            if(alertsIconContainer instanceof android.widget.FrameLayout) {
                View icon = ((android.widget.FrameLayout) alertsIconContainer).getChildAt(0);
                if(icon instanceof ImageView) ((ImageView) icon).setColorFilter(activeColor);
            } else if (alertsIconContainer instanceof ImageView) {
                ((ImageView) alertsIconContainer).setColorFilter(activeColor);
            }
            ((TextView) navAlerts.getChildAt(1)).setTextColor(activeColor);
            navAlerts.setOnClickListener(v -> {});
        }

        if (navProfile != null) {
            ((ImageView) navProfile.getChildAt(0)).setColorFilter(inactiveColor);
            ((TextView) navProfile.getChildAt(1)).setTextColor(inactiveColor);
            navProfile.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.patientProfileFragment));
        }
    }

    private void fetchMedicalRecords() {
        // Look directly at the Consultations database folder
        DatabaseReference recordsRef = FirebaseDatabase.getInstance().getReference("Consultations");

        recordsRef.orderByChild("patientUid").equalTo(loggedInUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                upcomingList.clear();
                pastList.clear();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    String id = snap.getKey();

                    // Pull the actual medical details
                    String type = snap.child("type").getValue(String.class); // e.g., Vaccine, Checkup
                    String date = snap.child("date").getValue(String.class);
                    String status = snap.child("status").getValue(String.class);
                    Long timestamp = snap.child("timestamp").getValue(Long.class);

                    if (type == null) type = "Medical Record";
                    if (date == null) date = "Date TBD";
                    if (timestamp == null) timestamp = System.currentTimeMillis();
                    if (status == null) status = "completed";

                    // Check if the record is upcoming or completed
                    boolean isUpcoming = "scheduled".equalsIgnoreCase(status);

                    String title = type;
                    String message = isUpcoming ? "Scheduled for: " + date : "Completed on: " + date;

                    NotifEntry entry = new NotifEntry(id, title, message, timestamp, !isUpcoming);

                    if (isUpcoming) {
                        upcomingList.add(entry);
                    } else {
                        pastList.add(entry);
                    }
                }

                // Sort lists
                Collections.sort(upcomingList, (a, b) -> Long.compare(b.timestamp, a.timestamp));
                Collections.sort(pastList, (a, b) -> Long.compare(b.timestamp, a.timestamp));

                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load records", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateUI() {
        if (!isAdded() || llNewNotifs == null || llEarlierNotifs == null) return;

        llNewNotifs.removeAllViews();
        llEarlierNotifs.removeAllViews();

        tvUnreadCountLabel.setText(upcomingList.size() + (upcomingList.size() == 1 ? " upcoming appointment" : " upcoming appointments"));

        if (upcomingList.isEmpty() && pastList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvNewLabel.setVisibility(View.GONE);
            tvEarlierLabel.setVisibility(View.GONE);
            return;
        }

        tvEmptyState.setVisibility(View.GONE);

        // Render Upcoming
        if (upcomingList.isEmpty()) {
            tvNewLabel.setVisibility(View.GONE);
        } else {
            tvNewLabel.setVisibility(View.VISIBLE);
            for (NotifEntry entry : upcomingList) {
                addCardToUI(llNewNotifs, entry);
            }
        }

        // Render Past Records
        if (pastList.isEmpty()) {
            tvEarlierLabel.setVisibility(View.GONE);
        } else {
            tvEarlierLabel.setVisibility(View.VISIBLE);
            for (NotifEntry entry : pastList) {
                addCardToUI(llEarlierNotifs, entry);
            }
        }
    }

    private void addCardToUI(LinearLayout targetLayout, NotifEntry entry) {
        if (getContext() == null) return;
        View card = getLayoutInflater().inflate(R.layout.item_patient_notification, null);

        ImageView ivIcon = card.findViewById(R.id.ivNotifIcon);
        TextView tvTitle = card.findViewById(R.id.tvNotifTitle);
        TextView tvMessage = card.findViewById(R.id.tvNotifMessage);
        TextView tvDate = card.findViewById(R.id.tvNotifDate);
        TextView tvUnreadDot = card.findViewById(R.id.tvUnreadDot);

        tvTitle.setText(entry.title != null ? entry.title : "Record");
        tvMessage.setText(entry.message != null ? entry.message : "");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        tvDate.setText(sdf.format(new Date(entry.timestamp)));

        // If it is an UPCOMING appointment (isRead is false here), highlight it in blue
        if (!entry.isRead) {
            tvUnreadDot.setVisibility(View.VISIBLE);
            ivIcon.setBackgroundResource(R.drawable.circle_light_blue);
            ivIcon.setColorFilter(Color.parseColor("#1976D2"));
        } else {
            // If it is a PAST record, show it in green
            tvUnreadDot.setVisibility(View.GONE);
            ivIcon.setBackgroundResource(R.drawable.badge_green_light);
            ivIcon.setColorFilter(Color.parseColor("#81C784"));
            tvTitle.setTextColor(Color.parseColor("#475569"));
        }

        // Change icon based on keywords
        if (entry.title != null) {
            String t = entry.title.toLowerCase();
            if (t.contains("appointment") || t.contains("checkup") || t.contains("consultation")) {
                ivIcon.setImageResource(android.R.drawable.ic_menu_today);
            } else if (t.contains("vaccine") || t.contains("immunization")) {
                ivIcon.setImageResource(android.R.drawable.ic_menu_edit);
            }
        }

        targetLayout.addView(card);
    }

    class NotifEntry {
        String id, title, message;
        long timestamp;
        boolean isRead; // We are reusing this flag to mean "is this record in the past?"

        NotifEntry(String id, String title, String message, long timestamp, boolean isRead) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.timestamp = timestamp;
            this.isRead = isRead;
        }
    }
}