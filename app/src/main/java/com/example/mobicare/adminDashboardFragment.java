package com.example.mobicare;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
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

public class adminDashboardFragment extends Fragment {

    private boolean hasNotifiedThisSession = false;

    private DatabaseReference mDatabaseMothers;
    private DatabaseReference mDatabaseConsultations;

    // Use a single unified list to prevent async syncing issues
    private List<ActivityEntry> masterActivityList = new ArrayList<>();
    private LinearLayout llActivityLogList;
    private TextView tvRecentActivityLabel;

    public adminDashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- Custom Bottom Navigation Logic ---
        LinearLayout navHome = view.findViewById(R.id.nav_home);
        LinearLayout navInventory = view.findViewById(R.id.nav_inventory);
        LinearLayout navAlerts = view.findViewById(R.id.nav_alerts);
        LinearLayout navProfile = view.findViewById(R.id.nav_profile);

        if (navHome != null) navHome.setOnClickListener(v -> {});

        // FIXED ID: Matches your nav_graph.xml line 85
        if (navInventory != null) navInventory.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_adminDashboardFragment_to_inventoryFragment));

        if (navAlerts != null) navAlerts.setOnClickListener(v -> {
            if (getActivity() != null) {
                SharedPreferences prefs = requireActivity().getSharedPreferences("MobicarePrefs", Context.MODE_PRIVATE);
                prefs.edit().putBoolean("alertsViewed", true).apply();
            }
            TextView tvBottomAlertBadge = view.findViewById(R.id.tvBottomAlertBadge);
            if (tvBottomAlertBadge != null) tvBottomAlertBadge.setVisibility(View.GONE);

            // FIXED ID: Matches your nav_graph.xml line 87
            Navigation.findNavController(view).navigate(R.id.action_adminDashboardFragment_to_alertsFragment);
        });

        // FIXED ID: Matches your nav_graph.xml line 89
        if (navProfile != null) navProfile.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_adminDashboardFragment_to_profileFragment));

        // --- Quick Actions Logic ---
        MaterialCardView cvHealthWorkers = view.findViewById(R.id.cvHealthWorkers);
        if (cvHealthWorkers != null) cvHealthWorkers.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_adminDashboardFragment_to_healthWorkerManagementFragment)); // Matches line 77

        MaterialCardView cvRegisterPatient = view.findViewById(R.id.cvRegisterPatient);
        if (cvRegisterPatient != null) cvRegisterPatient.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_adminDashboardFragment_to_registerPatientFragment)); // Matches line 79

        // Inside onViewCreated in adminDashboardFragment.java
        MaterialCardView cvViewMothers = view.findViewById(R.id.cvViewMothers);
        if (cvViewMothers != null) {
            cvViewMothers.setOnClickListener(v ->
                    Navigation.findNavController(view).navigate(R.id.action_adminDashboard_to_viewRecords));
        } // FIXED BRACE HERE

        MaterialCardView cvConsultations = view.findViewById(R.id.cvConsultations);
        if (cvConsultations != null) cvConsultations.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_adminDashboardFragment_to_consultationsFragment)); // Matches line 81

        MaterialCardView cvInventoryCard = view.findViewById(R.id.cvInventory);
        if (cvInventoryCard != null) cvInventoryCard.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putBoolean("showBottomNav", false);
            // FIXED ID: Matches line 85
            Navigation.findNavController(view).navigate(R.id.action_adminDashboardFragment_to_inventoryFragment, bundle);
        });

        // --- Setup Dynamic Top Badges ---
        setupTopSummaryBadges(view);

        // --- Recent Activity Logic ---
        mDatabaseMothers = FirebaseDatabase.getInstance().getReference("Mothers");
        mDatabaseConsultations = FirebaseDatabase.getInstance().getReference("Consultations");
        llActivityLogList = view.findViewById(R.id.llActivityItems);
        tvRecentActivityLabel = view.findViewById(R.id.tvRecentActivityLabel);

        // Fetch Activities sequentially
        fetchRecentActivities();
    }

    private void fetchRecentActivities() {
        masterActivityList.clear();

        // 1. Fetch Mothers First
        mDatabaseMothers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                for (DataSnapshot motherSnap : snapshot.getChildren()) {
                    String name = motherSnap.child("fullName").getValue(String.class);
                    Long regDate = motherSnap.child("registrationDate").getValue(Long.class);
                    if (name != null && regDate != null) {
                        String dateStr = dateFormat.format(new Date(regDate));
                        masterActivityList.add(new ActivityEntry(name, "Profile added", dateStr, "added", regDate));
                    }
                }
                // 2. ONLY AFTER MOTHERS LOAD, Fetch Consultations
                fetchConsultationsForActivity();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                fetchConsultationsForActivity();
            }
        });
    }

    private void fetchConsultationsForActivity() {
        mDatabaseConsultations.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long currentTime = System.currentTimeMillis();
                for (DataSnapshot consultSnap : snapshot.getChildren()) {
                    String patientName = consultSnap.child("patientName").getValue(String.class);
                    String purpose = consultSnap.child("purpose").getValue(String.class);
                    String date = consultSnap.child("date").getValue(String.class);
                    String time = consultSnap.child("time").getValue(String.class);
                    Long consultTimestamp = consultSnap.child("timestamp").getValue(Long.class);

                    if (patientName != null && consultTimestamp != null) {
                        String status = consultTimestamp > currentTime ? "scheduled" : "completed";
                        String formattedTimeText = date + " at " + time;
                        masterActivityList.add(new ActivityEntry(patientName, purpose, formattedTimeText, status, consultTimestamp));
                    }
                }

                // 3. BOTH ARE LOADED. Draw the UI!
                updateUnifiedLogUI();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                updateUnifiedLogUI();
            }
        });
    }

    // --- DYNAMIC TOP SUMMARY & ALERT LOGIC ---
    private void setupTopSummaryBadges(View view) {
        TextView tvChildrenCount = view.findViewById(R.id.tvChildrenCount);
        TextView tvUpcomingCount = view.findViewById(R.id.tvUpcomingCount);
        TextView tvWorkersCount = view.findViewById(R.id.tvWorkersCount);
        TextView tvBottomAlertBadge = view.findViewById(R.id.tvBottomAlertBadge);

        // 1. Children Count
        if (tvChildrenCount != null) {
            FirebaseDatabase.getInstance().getReference("Patients_Children").addValueEventListener(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                    tvChildrenCount.setText(String.valueOf(snapshot.getChildrenCount()));
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        // 2. Upcoming Consultations Count
        if (tvUpcomingCount != null) {
            FirebaseDatabase.getInstance().getReference("Consultations").addValueEventListener(new ValueEventListener() {
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

        // 3. Health Workers Count
        if (tvWorkersCount != null) {
            FirebaseDatabase.getInstance().getReference("HealthWorkers").addValueEventListener(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                    tvWorkersCount.setText(String.valueOf(snapshot.getChildrenCount()));
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        // 4. Background Alert Scanner
        FirebaseDatabase.getInstance().getReference("Inventory").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalAlerts = 0;
                long currentTime = System.currentTimeMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

                for (DataSnapshot itemSnap : snapshot.getChildren()) {
                    String qtyStr = itemSnap.child("quantity").getValue(String.class);
                    String expiryStr = itemSnap.child("expiryDate").getValue(String.class);
                    boolean hasAlert = false;

                    try { if (qtyStr != null && Integer.parseInt(qtyStr) <= 20) hasAlert = true; } catch (Exception e) {}
                    try {
                        if (!hasAlert && expiryStr != null && !expiryStr.equals("No Expiry")) {
                            Date expiryDate = sdf.parse(expiryStr);
                            if (expiryDate != null) {
                                long daysRemaining = (expiryDate.getTime() - currentTime) / (1000 * 60 * 60 * 24);
                                if (daysRemaining <= 30) hasAlert = true;
                            }
                        }
                    } catch (Exception e) {}

                    if (hasAlert) totalAlerts++;
                }

                if (getActivity() != null) {
                    SharedPreferences prefs = requireActivity().getSharedPreferences("MobicarePrefs", Context.MODE_PRIVATE);
                    int savedAlertCount = prefs.getInt("savedAlertCount", 0);
                    boolean alertsViewed = prefs.getBoolean("alertsViewed", false);

                    if (totalAlerts != savedAlertCount) {
                        alertsViewed = false;
                        prefs.edit()
                                .putInt("savedAlertCount", totalAlerts)
                                .putBoolean("alertsViewed", false)
                                .apply();
                    }

                    if (tvBottomAlertBadge != null) {
                        if (totalAlerts > 0 && !alertsViewed) {
                            tvBottomAlertBadge.setVisibility(View.VISIBLE);
                            tvBottomAlertBadge.setText(String.valueOf(totalAlerts));
                        } else {
                            tvBottomAlertBadge.setVisibility(View.GONE);
                        }
                    }

                    if (totalAlerts > 0 && !alertsViewed) {
                        showPushNotification(totalAlerts);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showPushNotification(int totalAlerts) {
        if (hasNotifiedThisSession || totalAlerts == 0) return;
        Context context = getContext();
        if (context == null) return;

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "mobicare_alerts";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Inventory Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Mobicare Inventory Alert")
                .setContentText("Warning: " + totalAlerts + " items are low on stock or expiring soon!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
        hasNotifiedThisSession = true;
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
            case "added":
                tvStatusBadge.setBackgroundResource(R.drawable.badge_gray_light);
                tvStatusBadge.setTextColor(Color.DKGRAY);
                ivActivityIcon.setImageResource(android.R.drawable.ic_menu_myplaces);
                ivActivityIcon.setColorFilter(tvStatusBadge.getCurrentTextColor());
                break;
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