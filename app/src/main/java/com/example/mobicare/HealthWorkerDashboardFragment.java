package com.example.mobicare;

import android.graphics.Color;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class HealthWorkerDashboardFragment extends Fragment {

    private TextView tvChildrenCount, tvMotherCount, tvUpcomingCount;
    private DatabaseReference mDatabase;
    private String currentUid = "";
    private ImageView btnLogout;
    private View slot1, slot2, slot3;

    private DatabaseReference mDatabase;
    private LinearLayout llWorkerList;
    private TextView tvCount;

    public HealthWorkerDashboardFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // NOTE: Make sure this matches your actual XML layout file name for this screen!
        // If your layout is named fragment_health_worker_dashboard, change it here:
        return inflater.inflate(R.layout.fragment_health_worker_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Point to the Users node in Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        // Link to UI Elements
        llWorkerList = view.findViewById(R.id.llWorkerList);
        tvCount = view.findViewById(R.id.tvCount);

        // 1. Back Button Navigation
        ImageView btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());
        }

        // 2. Add Worker Dialog Button
        MaterialButton btnAddWorker = view.findViewById(R.id.btnAddWorker);
        if (btnAddWorker != null) {
            btnAddWorker.setOnClickListener(v -> showAddWorkerDialog());
        }

        // 3. Start fetching data from Firebase
        fetchHealthWorkers();
    }

    private void showAddWorkerDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_health_worker, null);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Buttons
        MaterialButton btnAdd = dialogView.findViewById(R.id.btnDialogAdd);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnDialogCancel);
        ImageView btnClose = dialogView.findViewById(R.id.btnDialogClose);

        // Input Fields
        EditText etName = dialogView.findViewById(R.id.etDialogFullName);
        EditText etUser = dialogView.findViewById(R.id.etDialogUsername);
        EditText etPass = dialogView.findViewById(R.id.etDialogPassword);
        EditText etPhone = dialogView.findViewById(R.id.etDialogPhone);
        EditText etEmail = dialogView.findViewById(R.id.etDialogEmail);

        // Setup the Spinner for Specialization (FIXED ERROR: No longer uses EditText)
        Spinner spinnerSpec = dialogView.findViewById(R.id.spinnerSpecialization);

        String[] specializations = {
                "General Practice",
                "Midwifery",
                "Barangay Health Worker (BHW)",
                "Pediatrics",
                "Nutritionist",
                "Obstetrics"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                specializations
        );
        spinnerSpec.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String user = etUser.getText().toString().trim();
            String pass = etPass.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String email = etEmail.getText().toString().trim();

            // Get the selected item from the dropdown instead of an EditText
            String spec = spinnerSpec.getSelectedItem().toString();

            if (!name.isEmpty() && !user.isEmpty() && !pass.isEmpty()) {

                HashMap<String, Object> map = new HashMap<>();
                map.put("fullName", name);
                map.put("username", user);
                map.put("password", pass);
                map.put("phone", phone);
                map.put("email", email);
                map.put("specialization", spec);
                map.put("role", "Health Worker");
                map.put("registrationDate", System.currentTimeMillis()); // Added for the Activity Feed

                // Generate a unique, random ID
                String uniqueId = mDatabase.push().getKey();

                if (uniqueId != null) {
                    mDatabase.child(uniqueId).setValue(map).addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            Toast.makeText(getContext(), "Health Worker Added!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(getContext(), "Failed to add worker", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            } else {
                Toast.makeText(getContext(), "Name, Username, and Password are required", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void fetchHealthWorkers() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (llWorkerList != null) {
                    llWorkerList.removeAllViews();
                }
                int count = 0;

                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    // Check if the user is actually a Health Worker before showing them
                    String role = userSnap.child("role").getValue(String.class);

                    if (role != null && role.equals("Health Worker")) {
                        count++;
                        String name = userSnap.child("fullName").getValue(String.class);
                        String spec = userSnap.child("specialization").getValue(String.class);
                        String phone = userSnap.child("phone").getValue(String.class);
                        String email = userSnap.child("email").getValue(String.class);
                        Long regDate = userSnap.child("registrationDate").getValue(Long.class);
                        String uniqueId = userSnap.getKey();

                        addWorkerCardToUI(name, spec, uniqueId, phone, email, regDate);
                    }
                }

                if (tvCount != null) {
                    tvCount.setText(count + " registered health workers");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load workers.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // FIXED ERROR: Handles the new XML layout with Phone, Email, and Date (No tvUser)
    private void addWorkerCardToUI(String name, String spec, String uniqueId, String phone, String email, Long regDate) {
        View card = getLayoutInflater().inflate(R.layout.item_worker_card, null);

        TextView tvName = card.findViewById(R.id.tvName);
        TextView tvSpec = card.findViewById(R.id.tvSpec);
        TextView tvPhone = card.findViewById(R.id.tvPhone);
        TextView tvEmail = card.findViewById(R.id.tvEmail);
        TextView tvDate = card.findViewById(R.id.tvDate);

        tvName.setText(name != null ? name : "Unknown Worker");
        tvSpec.setText(spec != null ? spec : "General Practice");
        tvPhone.setText(phone != null && !phone.isEmpty() ? "📞 " + phone : "📞 No phone provided");
        tvEmail.setText(email != null && !email.isEmpty() ? "✉️ " + email : "✉️ No email provided");

        // Format the timestamp into a readable date
        if (regDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String dateString = sdf.format(new Date(regDate));
            tvDate.setText("Added on " + dateString);
        } else {
            tvDate.setText("Added date unknown");
        }

        // Setup the Edit Button
        card.findViewById(R.id.btnEdit).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Edit feature coming soon for " + name, Toast.LENGTH_SHORT).show();
        });

        // Setup the Delete Button
        card.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            mDatabase.child(uniqueId).removeValue();
            Toast.makeText(getContext(), "Deleted " + name, Toast.LENGTH_SHORT).show();
        });

        if (llWorkerList != null) {
            llWorkerList.addView(card);
        }
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