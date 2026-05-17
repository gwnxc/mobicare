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

public class alert extends Fragment {

    private DatabaseReference mDatabaseMothers;
    private DatabaseReference mDatabaseConsultations;
    private DatabaseReference mDatabaseHealthWorkers;
    private DatabaseReference mDatabaseInventory;

    private LinearLayout llNewNotifications;
    private LinearLayout llEarlierNotifications;
    private TextView tvUnreadCount;
    private TextView tvNewHeader;
    private TextView tvEarlierHeader;

    private List<NotificationItem> allNotifications = new ArrayList<>();
    private final long TWENTY_FOUR_HOURS = 24 * 60 * 60 * 1000L;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alert, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // UI Hooks
        ImageView btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        llNewNotifications = view.findViewById(R.id.llNewNotifications);
        llEarlierNotifications = view.findViewById(R.id.llEarlierNotifications);
        tvUnreadCount = view.findViewById(R.id.tvUnreadCount);
        tvNewHeader = view.findViewById(R.id.tvNewHeader);
        tvEarlierHeader = view.findViewById(R.id.tvEarlierHeader);

        setupBottomNavigation(view);

        // Init Firebase References
        mDatabaseMothers = FirebaseDatabase.getInstance().getReference("Mothers");
        mDatabaseConsultations = FirebaseDatabase.getInstance().getReference("Consultations");
        mDatabaseHealthWorkers = FirebaseDatabase.getInstance().getReference("HealthWorkers");
        mDatabaseInventory = FirebaseDatabase.getInstance().getReference("Inventory");

        // Start the fetching chain!
        fetchNotifications();
    }

    // --- 1. Fetch Mothers ---
    private void fetchNotifications() {
        allNotifications.clear();
        mDatabaseMothers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String name = snap.child("fullName").getValue(String.class);
                    Long timestamp = snap.child("registrationDate").getValue(Long.class);
                    if (name != null && timestamp != null) {
                        allNotifications.add(new NotificationItem("New Patient", name + " registered.", timestamp, "patient"));
                    }
                }
                fetchConsultations(); // Chain next
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { fetchConsultations(); }
        });
    }

    // --- 2. Fetch Consultations ---
    private void fetchConsultations() {
        mDatabaseConsultations.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long currentTime = System.currentTimeMillis();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String name = snap.child("patientName").getValue(String.class);
                    String purpose = snap.child("purpose").getValue(String.class);
                    String date = snap.child("date").getValue(String.class);
                    Long timestamp = snap.child("timestamp").getValue(Long.class);

                    if (name != null && timestamp != null) {
                        String title = timestamp > currentTime ? "Upcoming Checkup" : "Consultation Added";
                        String type = purpose != null && purpose.toLowerCase().contains("vaccin") ? "vaccine" : "checkup";
                        allNotifications.add(new NotificationItem(title, name + " (" + purpose + ") on " + date, timestamp, type));
                    }
                }
                fetchHealthWorkers(); // Chain next
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { fetchHealthWorkers(); }
        });
    }

    // --- 3. Fetch Health Workers ---
    private void fetchHealthWorkers() {
        mDatabaseHealthWorkers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String name = snap.child("fullName").getValue(String.class);
                    Long timestamp = snap.child("registrationDate").getValue(Long.class);
                    if (name != null && timestamp != null) {
                        allNotifications.add(new NotificationItem("New Health Worker", name + " was added to the team.", timestamp, "worker"));
                    }
                }
                fetchInventoryAlerts(); // Chain next
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { fetchInventoryAlerts(); }
        });
    }

    // --- 4. Fetch Inventory Warnings ---
    private void fetchInventoryAlerts() {
        mDatabaseInventory.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long currentTime = System.currentTimeMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

                for (DataSnapshot snap : snapshot.getChildren()) {
                    String name = snap.child("itemName").getValue(String.class);
                    String qtyStr = snap.child("quantity").getValue(String.class);
                    String expiryStr = snap.child("expiryDate").getValue(String.class);
                    if (name == null) continue;

                    // Check Quantity
                    try {
                        if (qtyStr != null && Integer.parseInt(qtyStr) <= 20) {
                            // Give it the current time so it shows up at the very top of "New"
                            allNotifications.add(new NotificationItem("Low Stock", name + " is low (" + qtyStr + " left).", currentTime, "inv_low"));
                        }
                    } catch (Exception e) {}

                    // Check Expiry
                    try {
                        if (expiryStr != null && !expiryStr.equals("No Expiry")) {
                            Date expDate = sdf.parse(expiryStr);
                            if (expDate != null) {
                                long days = (expDate.getTime() - currentTime) / (1000 * 60 * 60 * 24);
                                if (days < 0) {
                                    allNotifications.add(new NotificationItem("Expired", name + " has expired!", currentTime, "inv_expired"));
                                } else if (days <= 30) {
                                    allNotifications.add(new NotificationItem("Expiring Soon", name + " expires in " + days + " days.", currentTime, "inv_expiry"));
                                }
                            }
                        }
                    } catch (Exception e) {}
                }
                populateUI(); // Finally, draw everything!
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { populateUI(); }
        });
    }

    // --- 5. Draw UI ---
    private void populateUI() {
        if (llNewNotifications == null || llEarlierNotifications == null) return;

        llNewNotifications.removeAllViews();
        llEarlierNotifications.removeAllViews();

        // Sort descending (newest first)
        Collections.sort(allNotifications, (a, b) -> Long.compare(b.timestamp, a.timestamp));

        long currentTime = System.currentTimeMillis();
        int newCount = 0;
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

        for (NotificationItem item : allNotifications) {
            View card = getLayoutInflater().inflate(R.layout.item_notification_card, null);

            TextView tvTitle = card.findViewById(R.id.tvNotifTitle);
            TextView tvDesc = card.findViewById(R.id.tvNotifDesc);
            TextView tvDate = card.findViewById(R.id.tvNotifDate);
            ImageView ivIcon = card.findViewById(R.id.ivNotifIcon);
            View unreadDot = card.findViewById(R.id.ivUnreadDot);

            tvTitle.setText(item.title);
            tvDesc.setText(item.description);
            tvDate.setText(dateFormat.format(new Date(item.timestamp)));

            // Color coding based on the event type
            switch (item.type) {
                case "vaccine":
                    ivIcon.setImageResource(android.R.drawable.ic_menu_myplaces);
                    ivIcon.setColorFilter(Color.parseColor("#4CAF50")); // Green
                    break;
                case "checkup":
                    ivIcon.setImageResource(android.R.drawable.ic_menu_today);
                    ivIcon.setColorFilter(Color.parseColor("#2196F3")); // Blue
                    break;
                case "patient":
                    ivIcon.setImageResource(android.R.drawable.ic_input_add);
                    ivIcon.setColorFilter(Color.parseColor("#9C27B0")); // Purple
                    break;
                case "worker":
                    ivIcon.setImageResource(android.R.drawable.ic_menu_agenda);
                    ivIcon.setColorFilter(Color.parseColor("#00BCD4")); // Cyan
                    break;
                case "inv_low":
                case "inv_expiry":
                    ivIcon.setImageResource(android.R.drawable.ic_dialog_alert);
                    ivIcon.setColorFilter(Color.parseColor("#FF9800")); // Orange
                    tvTitle.setTextColor(Color.parseColor("#FF9800"));
                    break;
                case "inv_expired":
                    ivIcon.setImageResource(android.R.drawable.ic_dialog_alert);
                    ivIcon.setColorFilter(Color.parseColor("#F44336")); // Red
                    tvTitle.setTextColor(Color.parseColor("#F44336"));
                    break;
            }

            // Decide "New" or "Earlier" category
            if ((currentTime - item.timestamp) < TWENTY_FOUR_HOURS || item.timestamp > currentTime || item.type.startsWith("inv")) {
                newCount++;
                unreadDot.setVisibility(View.VISIBLE);
                llNewNotifications.addView(card);
            } else {
                unreadDot.setVisibility(View.GONE);
                llEarlierNotifications.addView(card);
            }
        }

        tvUnreadCount.setText(newCount + " new alerts & events");
        tvNewHeader.setVisibility(newCount > 0 ? View.VISIBLE : View.GONE);
        tvEarlierHeader.setVisibility(llEarlierNotifications.getChildCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private void setupBottomNavigation(View view) {
        LinearLayout navHome = view.findViewById(R.id.nav_home);
        LinearLayout navInventory = view.findViewById(R.id.nav_inventory);
        LinearLayout navAlerts = view.findViewById(R.id.nav_alerts);
        LinearLayout navProfile = view.findViewById(R.id.nav_profile);

        // --- Highlight Logic ---
        int activeColor = Color.parseColor("#155A91"); // Blue
        int inactiveColor = Color.parseColor("#8E8E8E"); // Grey

        if (navHome != null) {
            ((ImageView) navHome.getChildAt(0)).setColorFilter(inactiveColor);
            ((TextView) navHome.getChildAt(1)).setTextColor(inactiveColor);
            // FIXED: Uses the Alert Fragment's specific action to go Home
            navHome.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_adminAlertsFragment_to_adminDashboardFragment));
        }

        if (navInventory != null) {
            ((ImageView) navInventory.getChildAt(0)).setColorFilter(inactiveColor);
            ((TextView) navInventory.getChildAt(1)).setTextColor(inactiveColor);
            // FIXED: Uses the Alert Fragment's specific action to go to Inventory
            navInventory.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_adminAlertsFragment_to_inventoryFragment));
        }

        if (navAlerts != null) {
            View alertsIcon = ((android.widget.FrameLayout) navAlerts.getChildAt(0)).getChildAt(0);
            if (alertsIcon instanceof ImageView) ((ImageView) alertsIcon).setColorFilter(activeColor);
            ((TextView) navAlerts.getChildAt(1)).setTextColor(activeColor);
            // Already on Alerts
            navAlerts.setOnClickListener(v -> {});
        }

        if (navProfile != null) {
            ((ImageView) navProfile.getChildAt(0)).setColorFilter(inactiveColor);
            ((TextView) navProfile.getChildAt(1)).setTextColor(inactiveColor);
            // FIXED: Uses the Alert Fragment's specific action to go to Profile
            navProfile.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_adminAlertsFragment_to_adminProfileFragment));
        }
    }

    class NotificationItem {
        String title, description, type;
        long timestamp;

        NotificationItem(String title, String description, long timestamp, String type) {
            this.title = title;
            this.description = description;
            this.timestamp = timestamp;
            this.type = type;
        }
    }
}