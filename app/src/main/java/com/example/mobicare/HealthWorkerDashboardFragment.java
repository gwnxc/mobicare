package com.example.mobicare;

import android.graphics.Color;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HealthWorkerDashboardFragment extends Fragment {

    private TextView tvChildrenCount, tvMotherCount, tvUpcomingCount;
    private DatabaseReference mDatabase;
    private String currentUid = "";
    private ImageView btnLogout;
    private View slot1, slot2, slot3;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_health_worker_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("MobiCarePrefs", android.content.Context.MODE_PRIVATE);
        currentUid = prefs.getString("loggedUserKey", "");

        // 1. Initialize Views
        tvChildrenCount = view.findViewById(R.id.tvChildrenCountDashboard);
        tvMotherCount = view.findViewById(R.id.tvMotherCountDashboard);
        tvUpcomingCount = view.findViewById(R.id.tvUpcomingCountDashboard);

        btnLogout = view.findViewById(R.id.btnLogout);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // 2. Initialize Activity Slots
        slot1 = view.findViewById(R.id.activity1);
        slot2 = view.findViewById(R.id.activity2);
        slot3 = view.findViewById(R.id.activity3);

        // Hide slots by default until data arrives
        slot1.setVisibility(View.GONE);
        slot2.setVisibility(View.GONE);
        slot3.setVisibility(View.GONE);

        // Profile navigation
        ImageView ivProfile = view.findViewById(R.id.ivProfile);
        ivProfile.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.profileFragment));

        // --- QUICK ACTION CARD LISTENERS ---
        view.findViewById(R.id.cvRegisterPatient).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.registrationHubFragment));

        view.findViewById(R.id.cvAddRecord).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.addConsultationFragment));

        view.findViewById(R.id.cvConsultations).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.consultationsFragment));

        view.findViewById(R.id.cvViewRecords).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.viewRecordsFragment));

        view.findViewById(R.id.cvNotifications).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.alertsFragment));

        btnLogout.setOnClickListener(v -> showLogoutConfirmation());

        // 3. Initialize Data Loaders
        fetchDashboardStats();
        fetchHealthWorkerProfile();
        listenForNotifications();
        listenForRecentActivities();
    }

    private void listenForRecentActivities() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        String uid = "";

        if (user != null) {
            uid = user.getUid();
        } else if (currentUid != null && !currentUid.isEmpty()) {
            uid = currentUid;
        }
        mDatabase.child("Recent_Activities").child(uid)
                .orderByChild("timestamp")
                .limitToLast(3)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;

                        if (!snapshot.exists()) {
                            slot1.setVisibility(View.GONE);
                            return;
                        }

                        java.util.List<DataSnapshot> activities = new java.util.ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            activities.add(ds);
                        }
                        java.util.Collections.reverse(activities);

                        // Reset visibility
                        slot1.setVisibility(View.GONE);
                        slot2.setVisibility(View.GONE);
                        slot3.setVisibility(View.GONE);

                        for (int i = 0; i < activities.size(); i++) {
                            DataSnapshot ds = activities.get(i);
                            String type = ds.child("type").getValue(String.class);
                            String desc = ds.child("description").getValue(String.class);
                            Long timestamp = ds.child("timestamp").getValue(Long.class);

                            String timeStr = "Just now";
                            if (timestamp != null) {
                                timeStr = DateUtils.getRelativeTimeSpanString(timestamp,
                                        System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString();
                            }

                            if (i == 0) {
                                slot1.setVisibility(View.VISIBLE);
                                updateActivitySlot(slot1, type, desc, timeStr);
                            } else if (i == 1) {
                                slot2.setVisibility(View.VISIBLE);
                                updateActivitySlot(slot2, type, desc, timeStr);
                            } else if (i == 2) {
                                slot3.setVisibility(View.VISIBLE);
                                updateActivitySlot(slot3, type, desc, timeStr);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updateActivitySlot(View row, String type, String details, String time) {
        if (row == null || type == null) return;
        TextView title = row.findViewById(R.id.tvActivityTitle);
        TextView subtitle = row.findViewById(R.id.tvActivitySubtitle);
        TextView timeStamp = row.findViewById(R.id.tvActivityTime);
        ImageView icon = row.findViewById(R.id.ivActivityIcon);

        title.setText(type);
        subtitle.setText(details);
        timeStamp.setText(time);

        switch (type) {
            case "Patient Registered":
                icon.setImageResource(R.drawable.ic_profile);
                icon.setBackgroundResource(R.drawable.circle_light_blue);
                icon.setColorFilter(Color.parseColor("#155A91"));
                break;
            case "Immunization":
            case "Postnatal Care":
                icon.setImageResource(R.drawable.ic_add_record);
                icon.setBackgroundResource(R.drawable.circle_light_green);
                icon.setColorFilter(Color.parseColor("#4CAF50"));
                break;
            case "Consultation":
                icon.setImageResource(R.drawable.ic_consultation);
                icon.setBackgroundResource(R.drawable.circle_light_blue);
                icon.setColorFilter(Color.parseColor("#155A91"));
                break;
        }
    }

    private void showLogoutConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_logout, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnCancelLogout).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnConfirmLogout).setOnClickListener(v -> {
            dialog.dismiss();
            FirebaseAuth.getInstance().signOut();
            Navigation.findNavController(requireView()).navigate(R.id.loginFragment);
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        });
        dialog.show();
    }
    private void fetchHealthWorkerProfile() {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("MobiCarePrefs", android.content.Context.MODE_PRIVATE);
        String userKey = prefs.getString("loggedUserKey", "");

        if (!userKey.isEmpty()) {
            mDatabase.child("HealthWorkers").child(userKey).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && isAdded()) {
                        String name = snapshot.child("fullName").getValue(String.class);
                        TextView tvUserName = getView().findViewById(R.id.tvUserName);
                        if (tvUserName != null && name != null) {
                            tvUserName.setText(name); // Reflects Maria Santos
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void fetchDashboardStats() {
        mDatabase.child("Patients_Guardians").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded() && tvMotherCount != null) {
                    tvMotherCount.setText(String.valueOf(snapshot.getChildrenCount()));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        mDatabase.child("Patients_Children").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded() && tvChildrenCount != null) {
                    tvChildrenCount.setText(String.valueOf(snapshot.getChildrenCount()));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        mDatabase.child("Consultations")
                .orderByChild("status")
                .equalTo("scheduled")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isAdded() && tvUpcomingCount != null) {
                            tvUpcomingCount.setText(String.valueOf(snapshot.getChildrenCount()));
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void listenForNotifications() {
        // 1. Get the current user safely
        // Correct way to get the user
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        String uid = "";

        if (user != null) {
            uid = user.getUid();
        } else if (currentUid != null && !currentUid.isEmpty()) {
            // Fallback to the SharedPreferences UID if Firebase Auth isn't ready
            uid = currentUid;
        }

        // 2. Only run the listener if we actually have a UID
        if (!uid.isEmpty()) {
            mDatabase.child("Notifications")
                    .orderByChild("receiverUid")
                    .equalTo(uid)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            // Safety check to ensure fragment is still active
                            if (!isAdded()) return;

                            int unreadCount = 0;
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                Boolean isRead = ds.child("isRead").getValue(Boolean.class);
                                if (isRead != null && !isRead) unreadCount++;
                            }
                            updateDashboardBadge(unreadCount);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }
    }

    private void updateDashboardBadge(int count) {
        if (getView() == null) return;
        TextView tvCardBadge = getView().findViewById(R.id.tvBadge);
        if (tvCardBadge != null) {
            if (count > 0) {
                tvCardBadge.setVisibility(View.VISIBLE);
                tvCardBadge.setText(String.valueOf(count));
            } else {
                tvCardBadge.setVisibility(View.GONE);
            }
        }
    }
}