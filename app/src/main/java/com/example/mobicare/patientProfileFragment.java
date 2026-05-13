package com.example.mobicare;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class patientProfileFragment extends Fragment {

    private String loggedInUserId;
    private TextView tvProfileName, tvProfileRole, tvProfilePhone, tvProfileEmail, tvProfileAddress;

    // Store these so we can pre-fill the edit dialog
    private String currentName = "";
    private String currentPhone = "";
    private String currentAddress = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireActivity().getSharedPreferences("MobicarePrefs", Context.MODE_PRIVATE);
        loggedInUserId = prefs.getString("loggedInUser", null);

        ImageView btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        // Initialize TextViews
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileRole = view.findViewById(R.id.tvProfileRole);
        tvProfilePhone = view.findViewById(R.id.tvProfilePhone);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfileAddress = view.findViewById(R.id.tvProfileAddress);

        // Action Buttons
        MaterialButton btnEditProfile = view.findViewById(R.id.btnEditProfile);
        MaterialButton btnChangePassword = view.findViewById(R.id.btnChangePassword);
        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);

        // ---> LAUNCH POP-UPS <---
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        }

        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                requireActivity().getSharedPreferences("MobicarePrefs", Context.MODE_PRIVATE).edit().clear().apply();
                Navigation.findNavController(view).navigate(R.id.loginFragment);
            });
        }

        // Setup Bottom Navigation
        LinearLayout navHome = view.findViewById(R.id.nav_home);
        LinearLayout navAlerts = view.findViewById(R.id.nav_alerts);
        LinearLayout navProfile = view.findViewById(R.id.nav_profile);

        if (navHome != null) navHome.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.patientDashboardFragment));
        if (navAlerts != null) navAlerts.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.patientAlertsFragment));
        if (navProfile != null) navProfile.setOnClickListener(v -> {}); // Already here

        if (loggedInUserId != null) {
            fetchPatientProfile();
        }
    }

    private void fetchPatientProfile() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Patients_Guardians").child(loggedInUserId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentName = snapshot.child("fullName").getValue(String.class);
                    currentPhone = snapshot.child("phone").getValue(String.class);
                    currentAddress = snapshot.child("address").getValue(String.class);

                    String role = snapshot.child("role").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);

                    tvProfileName.setText(currentName != null ? currentName : "Unknown");
                    tvProfileRole.setText(role != null ? role : "Parent/Guardian");
                    tvProfilePhone.setText(currentPhone != null && !currentPhone.isEmpty() ? currentPhone : "Not provided");
                    tvProfileEmail.setText(email != null && !email.isEmpty() ? email : "Not provided");
                    tvProfileAddress.setText(currentAddress != null && !currentAddress.isEmpty() ? currentAddress : "Not provided");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- POP-UP LOGIC: EDIT PROFILE ---
    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_patient_edit_profile, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();


        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText etName = dialogView.findViewById(R.id.etEditName);
        EditText etPhone = dialogView.findViewById(R.id.etEditPhone);
        EditText etAddress = dialogView.findViewById(R.id.etEditAddress);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);

        // Pre-fill current data
        etName.setText(currentName);
        etPhone.setText(currentPhone);
        etAddress.setText(currentAddress);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String newPhone = etPhone.getText().toString().trim();
            String newAddress = etAddress.getText().toString().trim();

            if (newName.isEmpty()) {
                etName.setError("Name is required");
                return;
            }

            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Patients_Guardians").child(loggedInUserId);
            Map<String, Object> updates = new HashMap<>();
            updates.put("fullName", newName);
            updates.put("phone", newPhone);
            updates.put("address", newAddress);

            userRef.updateChildren(updates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    fetchPatientProfile(); // Refresh the screen with new data!
                } else {
                    Toast.makeText(getContext(), "Update failed.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    // --- POP-UP LOGIC: CHANGE PASSWORD ---
    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_patient_change_password, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnUpdate = dialogView.findViewById(R.id.btnUpdate);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnUpdate.setOnClickListener(v -> {
            String newPass = etNewPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            if (newPass.length() < 6) {
                etNewPassword.setError("Minimum 6 characters");
                return;
            }
            if (!newPass.equals(confirmPass)) {
                etConfirmPassword.setError("Passwords do not match");
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                user.updatePassword(newPass).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Password updated successfully!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(getContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(getContext(), "Not authenticated. Please log in again.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}