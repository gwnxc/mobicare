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

    private List<NotifEntry> unreadList = new ArrayList<>();
    private List<NotifEntry> readList = new ArrayList<>();

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

        if (loggedInUserId != null) {
            fetchNotifications();
        }
    }

    private void fetchNotifications() {
        DatabaseReference notifsRef = FirebaseDatabase.getInstance().getReference("Notifications");

        // Fetch notifications where receiverUid matches the logged-in patient
        notifsRef.orderByChild("receiverUid").equalTo(loggedInUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                unreadList.clear();
                readList.clear();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    String title = snap.child("title").getValue(String.class);
                    String message = snap.child("message").getValue(String.class);
                    Long timestamp = snap.child("timestamp").getValue(Long.class);
                    Boolean isRead = snap.child("isRead").getValue(Boolean.class);
                    String id = snap.getKey();

                    if (timestamp == null) timestamp = 0L;
                    if (isRead == null) isRead = false;

                    NotifEntry entry = new NotifEntry(id, title, message, timestamp, isRead);

                    if (isRead) {
                        readList.add(entry);
                    } else {
                        unreadList.add(entry);
                    }
                }

                // Sort both lists with newest at the top
                Collections.sort(unreadList, (a, b) -> Long.compare(b.timestamp, a.timestamp));
                Collections.sort(readList, (a, b) -> Long.compare(b.timestamp, a.timestamp));

                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        llNewNotifs.removeAllViews();
        llEarlierNotifs.removeAllViews();

        tvUnreadCountLabel.setText(unreadList.size() + (unreadList.size() == 1 ? " unread notification" : " unread notifications"));

        if (unreadList.isEmpty() && readList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvNewLabel.setVisibility(View.GONE);
            tvEarlierLabel.setVisibility(View.GONE);
            return;
        }

        tvEmptyState.setVisibility(View.GONE);

        // Handle Unread (New)
        if (unreadList.isEmpty()) {
            tvNewLabel.setVisibility(View.GONE);
        } else {
            tvNewLabel.setVisibility(View.VISIBLE);
            for (NotifEntry entry : unreadList) {
                addCardToUI(llNewNotifs, entry);
            }
        }

        // Handle Read (Earlier)
        if (readList.isEmpty()) {
            tvEarlierLabel.setVisibility(View.GONE);
        } else {
            tvEarlierLabel.setVisibility(View.VISIBLE);
            for (NotifEntry entry : readList) {
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

        tvTitle.setText(entry.title != null ? entry.title : "Alert");
        tvMessage.setText(entry.message != null ? entry.message : "");

        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        tvDate.setText(sdf.format(new Date(entry.timestamp)));

        if (!entry.isRead) {
            tvUnreadDot.setVisibility(View.VISIBLE);
            ivIcon.setBackgroundResource(R.drawable.circle_light_blue);
            ivIcon.setColorFilter(Color.parseColor("#1976D2"));

            // Optional: Set up click listener to mark as read in Firebase when clicked
            card.setOnClickListener(v -> {
                FirebaseDatabase.getInstance().getReference("Notifications")
                        .child(entry.id).child("isRead").setValue(true);
            });

        } else {
            tvUnreadDot.setVisibility(View.GONE);
            ivIcon.setBackgroundResource(R.drawable.badge_green_light); // Assuming you have this light green background
            ivIcon.setColorFilter(Color.parseColor("#81C784"));
            tvTitle.setTextColor(Color.parseColor("#475569")); // Slightly dimmer text for read items
        }

        // Change icon based on keywords in title
        if (entry.title != null) {
            String t = entry.title.toLowerCase();
            if (t.contains("appointment") || t.contains("checkup") || t.contains("consultation")) {
                ivIcon.setImageResource(android.R.drawable.ic_menu_today);
            } else if (t.contains("vaccine") || t.contains("immunization")) {
                ivIcon.setImageResource(android.R.drawable.ic_menu_edit); // Substitute for a syringe if you don't have one
            }
        }

        targetLayout.addView(card);
    }

    class NotifEntry {
        String id, title, message;
        long timestamp;
        boolean isRead;

        NotifEntry(String id, String title, String message, long timestamp, boolean isRead) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.timestamp = timestamp;
            this.isRead = isRead;
        }
    }
}