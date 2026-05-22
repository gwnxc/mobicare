package com.example.mobicare;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class health_worker_management extends Fragment {

    private DatabaseReference mDatabase;
    private LinearLayout llWorkerList;
    private TextView tvCount;
    private EditText etSearch;
    private Spinner spinnerFilter;

    // Master lists for searching and filtering
    private List<WorkerItem> masterList = new ArrayList<>();
    private List<WorkerItem> filteredList = new ArrayList<>();

    private String currentSearchQuery = "";
    private String currentFilterSpec = "All Specializations";

    public health_worker_management() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_health_worker_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDatabase = FirebaseDatabase.getInstance().getReference("HealthWorkers");

        // UI Hooks
        llWorkerList = view.findViewById(R.id.llWorkerList);
        tvCount = view.findViewById(R.id.tvCount);
        etSearch = view.findViewById(R.id.etSearch);
        spinnerFilter = view.findViewById(R.id.spinnerFilter);

        ImageView btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        MaterialButton btnAddWorker = view.findViewById(R.id.btnAddWorker);
        // ---> CHANGED: Pass null because we are Adding, not Editing <---
        if (btnAddWorker != null) btnAddWorker.setOnClickListener(v -> showWorkerDialog(null, null, null, null, null, null));

        setupInteractions();
        fetchHealthWorkers();
    }

    private void setupInteractions() {
        // 1. Setup Filter Dropdown Options
        String[] filterOptions = {
                "All Specializations",
                "General Practice",
                "Midwifery",
                "Barangay Health Worker (BHW)",
                "Pediatrics",
                "Nutritionist",
                "Obstetrics"
        };
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, filterOptions);
        if (spinnerFilter != null) {
            spinnerFilter.setAdapter(filterAdapter);
            spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    currentFilterSpec = filterOptions[position];
                    applyFilters();
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        // 2. Setup Search Bar Listener
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s.toString().toLowerCase().trim();
                    applyFilters();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void fetchHealthWorkers() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                masterList.clear();

                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String name = userSnap.child("fullName").getValue(String.class);
                    String spec = userSnap.child("specialization").getValue(String.class);
                    String phone = userSnap.child("phone").getValue(String.class);
                    String email = userSnap.child("email").getValue(String.class);
                    String username = userSnap.child("username").getValue(String.class);
                    Long regDate = userSnap.child("registrationDate").getValue(Long.class);
                    String uniqueId = userSnap.getKey();

                    if (name != null) {
                        masterList.add(new WorkerItem(uniqueId, name, spec, phone, email, username, regDate));
                    }
                }

                // Once data is downloaded, filter it and draw it
                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load workers.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        filteredList.clear();

        for (WorkerItem worker : masterList) {
            boolean matchesFilter = currentFilterSpec.equals("All Specializations") ||
                    (worker.spec != null && worker.spec.equals(currentFilterSpec));

            boolean matchesSearch = worker.name.toLowerCase().contains(currentSearchQuery);

            if (matchesFilter && matchesSearch) {
                filteredList.add(worker);
            }
        }

        updateUI();
    }

    private void updateUI() {
        if (llWorkerList == null) return;
        llWorkerList.removeAllViews();

        for (WorkerItem worker : filteredList) {
            addWorkerCardToUI(worker);
        }

        if (tvCount != null) {
            tvCount.setText(filteredList.size() + " registered health workers");
        }
    }

    private void addWorkerCardToUI(WorkerItem worker) {
        View card = getLayoutInflater().inflate(R.layout.item_worker_card, llWorkerList, false);


        TextView tvName = card.findViewById(R.id.tvName);
        TextView tvSpec = card.findViewById(R.id.tvSpec);
        TextView tvPhone = card.findViewById(R.id.tvPhone);
        TextView tvEmail = card.findViewById(R.id.tvEmail);
        TextView tvDate = card.findViewById(R.id.tvDate);

        tvName.setText(worker.name != null ? worker.name : "Unknown Worker");
        tvSpec.setText(worker.spec != null ? worker.spec : "General Practice");
        tvPhone.setText(worker.phone != null && !worker.phone.isEmpty() ? "📞 " + worker.phone : "📞 No phone provided");
        tvEmail.setText(worker.email != null && !worker.email.isEmpty() ? "✉️ " + worker.email : "✉️ No email provided");

        if (worker.regDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            tvDate.setText("Added on " + sdf.format(new Date(worker.regDate)));
        } else {
            tvDate.setText("Added date unknown");
        }

        // ---> CHANGED: Pass the worker's data to the dialog when Edit is clicked! <---
        card.findViewById(R.id.btnEdit).setOnClickListener(v -> {
            // Note: We don't fetch the password here for security reasons. The edit dialog will handle it.
            showWorkerDialog(worker.id, worker.name, worker.username, worker.phone, worker.email, worker.spec);
        });

        card.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            mDatabase.child(worker.id).removeValue();
            Toast.makeText(getContext(), "Deleted " + worker.name, Toast.LENGTH_SHORT).show();
        });

        if (llWorkerList != null) llWorkerList.addView(card);
    }

    // ---> CHANGED: This method now handles BOTH Adding and Editing! <---
    private void showWorkerDialog(@Nullable String workerId, @Nullable String currentName, @Nullable String currentUsername, @Nullable String currentPhone, @Nullable String currentEmail, @Nullable String currentSpec) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_health_worker, null);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle); // Assuming you have a title in the XML
        MaterialButton btnAdd = dialogView.findViewById(R.id.btnDialogAdd);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnDialogCancel);
        ImageView btnClose = dialogView.findViewById(R.id.btnDialogClose);

        EditText etName = dialogView.findViewById(R.id.etDialogFullName);
        EditText etUser = dialogView.findViewById(R.id.etDialogUsername);
        EditText etPass = dialogView.findViewById(R.id.etDialogPassword);
        EditText etPhone = dialogView.findViewById(R.id.etDialogPhone);
        EditText etEmail = dialogView.findViewById(R.id.etDialogEmail);
        Spinner spinnerSpec = dialogView.findViewById(R.id.spinnerSpecialization);

        String[] specializations = {
                "General Practice",
                "Midwifery",
                "Barangay Health Worker (BHW)",
                "Pediatrics",
                "Nutritionist",
                "Obstetrics"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, specializations);
        spinnerSpec.setAdapter(adapter);

        // If workerId is NOT null, we are EDITING! Pre-fill the fields.
        if (workerId != null) {
            if (tvDialogTitle != null) tvDialogTitle.setText("Edit Health Worker");
            btnAdd.setText("Save Changes");

            etName.setText(currentName);
            etUser.setText(currentUsername);
            etPhone.setText(currentPhone);
            etEmail.setText(currentEmail);
            etPass.setHint("Leave blank to keep current password"); // Don't show old password

            // Set the spinner to the correct specialization
            for (int i = 0; i < specializations.length; i++) {
                if (specializations[i].equals(currentSpec)) {
                    spinnerSpec.setSelection(i);
                    break;
                }
            }
        } else {
            if (tvDialogTitle != null) tvDialogTitle.setText("Add Health Worker");
        }

        btnAdd.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String user = etUser.getText().toString().trim();
            String pass = etPass.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String spec = spinnerSpec.getSelectedItem().toString();

            if (!name.isEmpty() && !user.isEmpty()) {

                // If adding new, password is required
                if (workerId == null && pass.isEmpty()) {
                    Toast.makeText(getContext(), "Password is required for new workers", Toast.LENGTH_SHORT).show();
                    return;
                }

                HashMap<String, Object> map = new HashMap<>();
                map.put("fullName", name);
                map.put("username", user);
                map.put("phone", phone);
                map.put("email", email);
                map.put("specialization", spec);

                // Only update password if they typed a new one
                if (!pass.isEmpty()) {
                    map.put("password", pass);
                }

                if (workerId == null) {
                    // ADDING NEW
                    map.put("role", "Health Worker");
                    map.put("registrationDate", System.currentTimeMillis());
                    String newUniqueId = mDatabase.push().getKey();
                    if (newUniqueId != null) {
                        mDatabase.child(newUniqueId).setValue(map).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(getContext(), "Health Worker Added!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                Toast.makeText(getContext(), "Failed to add worker", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    // EDITING EXISTING
                    mDatabase.child(workerId).updateChildren(map).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Profile Updated!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                Toast.makeText(getContext(), "Name and Username are required", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // Helper class to store data temporarily for filtering
    private class WorkerItem {
        String id, name, spec, phone, email, username;
        Long regDate;

        WorkerItem(String id, String name, String spec, String phone, String email, String username, Long regDate) {
            this.id = id;
            this.name = name;
            this.spec = spec;
            this.phone = phone;
            this.email = email;
            this.username = username;
            this.regDate = regDate;
        }
    }
}