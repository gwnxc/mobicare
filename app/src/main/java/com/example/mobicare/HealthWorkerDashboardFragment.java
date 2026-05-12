package com.example.mobicare;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
}