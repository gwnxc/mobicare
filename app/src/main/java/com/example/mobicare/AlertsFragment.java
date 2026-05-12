package com.example.mobicare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
        if (currentUid != null && !currentUid.isEmpty()) {
            loadNotifications();
            //markAllAsRead();
        }
    }

    private void loadNotifications() {
        // 1. Point to the Notifications node
        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("Notifications");

        // 2. APPLY THE FILTER: This is the "Perspective Lock"
        // It tells Firebase: "Only give me notifications where I am the receiver"
        com.google.firebase.database.Query query = notifRef.orderByChild("receiverUid").equalTo(currentUid);

        query.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                if (!isAdded()) return;

                notificationList.clear();
                int unreadCount = 0;

                for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                    Notification notification = ds.getValue(Notification.class);
                    if (notification != null) {
                        notification.id = ds.getKey();
                        notificationList.add(notification);

                        // Logic for the unread count in the green header
                        if (Boolean.FALSE.equals(notification.isRead)) {
                            unreadCount++;
                        }
                    }
                }

                updateSubtitle(unreadCount);
                // Put the most recent alerts at the top
                java.util.Collections.reverse(notificationList);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                android.util.Log.e("FirebaseError", error.getMessage());
            }
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
        boolean isHealthWorker = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null;

        com.google.firebase.database.Query query;
        if (isHealthWorker) {
            query = mDatabase; // Or filter by receiverUid = "all"
        } else {
            query = mDatabase.orderByChild("receiverUid").equalTo(currentUid);
        }

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    // Only update if it's currently unread
                    Boolean isRead = ds.child("isRead").getValue(Boolean.class);
                    if (Boolean.FALSE.equals(isRead)) {
                        ds.getRef().child("isRead").setValue(true);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}