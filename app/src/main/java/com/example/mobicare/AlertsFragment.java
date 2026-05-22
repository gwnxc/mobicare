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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
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

public class AlertsFragment extends Fragment {

    private RecyclerView rvNotifications;
    private TextView tvSubtitle;
    private List<Notification> notificationList = new ArrayList<>();
    private NotificationAdapter adapter;
    private String currentUid;

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
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        // 2. If not, check SharedPreferences (Fixed spelling to match your other files!)
        else {
            SharedPreferences prefs = requireContext().getSharedPreferences("MobicarePrefs", Context.MODE_PRIVATE);
            userId = prefs.getString("loggedInUser", "");
        }

        this.currentUid = userId;

        rvNotifications = view.findViewById(R.id.rvNotifications);
        tvSubtitle = view.findViewById(R.id.tvSubtitle);
        ImageView btnBack = view.findViewById(R.id.btnBackAlerts);

        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(adapter);

        if (btnBack != null) btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Kick off the Master Activity Feed chain!
        // We now start it regardless of whether the UID is empty, because Health Workers
        // always need to see the global Consultations and Inventory!
        if (currentUid != null && !currentUid.isEmpty()) {
            loadNotifications();
        } else {
            // Skip direct notifications, jump straight to the global feed
            fetchConsultationFeed();
        }

        setupHealthWorkerNavigation(view, "alerts");
    }

    // --- STEP 1: Fetch Direct Notifications (Just in case) ---
    private void loadNotifications() {
        notificationList.clear();
        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("Notifications");

        notifRef.orderByChild("receiverUid").equalTo(currentUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Notification notification = ds.getValue(Notification.class);
                            if (notification != null) {
                                notification.id = ds.getKey();
                                notificationList.add(notification);
                            }
                        }

                        // Once done, fetch the clinical events
                        fetchConsultationFeed();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        if (isAdded()) fetchConsultationFeed();
                    }
                });
    }

    // --- STEP 2: Fetch Consultations (Appointments, Checkups, Cancellations) ---
    private void fetchConsultationFeed() {
        DatabaseReference consultRef = FirebaseDatabase.getInstance().getReference("Consultations");

        consultRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                long currentTime = System.currentTimeMillis();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String status = ds.child("status").getValue(String.class);
                    String patientName = ds.child("patientName").getValue(String.class);
                    String purpose = ds.child("purpose").getValue(String.class);
                    if (purpose == null) purpose = ds.child("type").getValue(String.class); // fallback
                    String date = ds.child("date").getValue(String.class);
                    Long timestamp = ds.child("timestamp").getValue(Long.class);

                    if (patientName != null && timestamp != null) {
                        Notification n = new Notification();
                        n.id = ds.getKey();
                        n.timestamp = timestamp;

                        // Build the text dynamically based on the status
                        if ("cancelled".equalsIgnoreCase(status)) {
                            n.title = "Cancelled: " + purpose;
                            n.message = patientName + "'s appointment was cancelled.";
                        } else if ("scheduled".equalsIgnoreCase(status) || timestamp > currentTime) {
                            n.title = "Upcoming: " + purpose;
                            n.message = patientName + " is scheduled on " + date;
                        } else {
                            n.title = "Record Added";
                            n.message = purpose + " completed for " + patientName;
                        }

                        notificationList.add(n);
                    }
                }

                // Once done, fetch the inventory warnings
                fetchInventoryFeed();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) fetchInventoryFeed();
            }
        });
    }

    // --- STEP 3: Fetch Inventory Warnings (Low stock, expiring) ---
    private void fetchInventoryFeed() {
        DatabaseReference invRef = FirebaseDatabase.getInstance().getReference("Inventory");

        invRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                long currentTime = System.currentTimeMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String name = ds.child("itemName").getValue(String.class);
                    String qtyStr = ds.child("quantity").getValue(String.class);
                    String expiryStr = ds.child("expiryDate").getValue(String.class);

                    if (name == null) continue;

                    // Low Quantity Check
                    try {
                        if (qtyStr != null && Integer.parseInt(qtyStr) <= 20) {
                            Notification n = new Notification();
                            n.id = ds.getKey() + "_qty";
                            n.title = "Low Stock Alert";
                            n.message = name + " only has " + qtyStr + " left in inventory.";
                            n.timestamp = currentTime;
                            notificationList.add(n);
                        }
                    } catch (Exception ignored) {}

                    // Expiry Check
                    try {
                        if (expiryStr != null && !expiryStr.equals("No Expiry") && !expiryStr.isEmpty()) {
                            Date expDate = sdf.parse(expiryStr);
                            if (expDate != null) {
                                long days = (expDate.getTime() - currentTime) / (1000 * 60 * 60 * 24);
                                if (days < 0) {
                                    Notification n = new Notification();
                                    n.id = ds.getKey() + "_exp1";
                                    n.title = "Expired Item";
                                    n.message = name + " has expired!";
                                    n.timestamp = currentTime;
                                    notificationList.add(n);
                                } else if (days <= 30) {
                                    Notification n = new Notification();
                                    n.id = ds.getKey() + "_exp2";
                                    n.title = "Expiring Soon";
                                    n.message = name + " expires in " + days + " days.";
                                    n.timestamp = currentTime;
                                    notificationList.add(n);
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }

                // FINALLY, sort the combined list and draw it to the screen!
                Collections.sort(notificationList, (n1, n2) -> Long.compare(n2.timestamp, n1.timestamp));
                adapter.notifyDataSetChanged();
                updateSubtitle(notificationList.size());

                // DIAGNOSTIC TOAST: If you see "Found 0 items", your Firebase tables might be empty.
                // If you see "Found X items" but the screen is blank, there is an issue with your XML ID names.
                Toast.makeText(getContext(), "Loaded " + notificationList.size() + " feed items", Toast.LENGTH_SHORT).show();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateSubtitle(int count) {
        if (tvSubtitle != null) {
            tvSubtitle.setText(count + " alerts and events");
        }
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