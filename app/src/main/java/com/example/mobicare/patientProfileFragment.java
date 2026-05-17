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

        // ---> ROBUST LOGIN CHECK <---
        SharedPreferences prefs = requireActivity().getSharedPreferences("MobiCarePrefs", Context.MODE_PRIVATE);
        loggedInUserId = prefs.getString("loggedUserKey", null);

        if (loggedInUserId == null || loggedInUserId.isEmpty()) {
            SharedPreferences altPrefs = requireActivity().getSharedPreferences("MobicarePrefs", Context.MODE_PRIVATE);
            loggedInUserId = altPrefs.getString("loggedInUser", null);
        }

        if (loggedInUserId == null || loggedInUserId.isEmpty()) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                loggedInUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            }
        }

        // Initialize UI Elements
        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        tvProfileName = view.findViewById(R.id.tvPatientName);
        tvProfileRole = view.findViewById(R.id.tvPatientID);
        tvProfilePhone = view.findViewById(R.id.tvPatientGuardian);
        tvProfileEmail = view.findViewById(R.id.tvPatientDOB);
        tvProfileAddress = view.findViewById(R.id.tvPatientAddress);

        View btnEditProfile = view.findViewById(R.id.btnEditProfile);
        View btnChangePassword = view.findViewById(R.id.btnChangePassword);
        View btnLogout = view.findViewById(R.id.btnLogout);

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
                requireActivity().getSharedPreferences("MobiCarePrefs", Context.MODE_PRIVATE).edit().clear().apply();
                Navigation.findNavController(view).navigate(R.id.loginFragment);
            });
        }

        // --- Bottom Navigation Setup ---
        setupBottomNavigation(view);

        if (loggedInUserId != null && !loggedInUserId.isEmpty()) {
            fetchPatientProfile();
        } else {
            Toast.makeText(getContext(), "Session error. Please log in again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupBottomNavigation(View view) {
        LinearLayout navHome = view.findViewById(R.id.nav_home);
        LinearLayout navAlerts = view.findViewById(R.id.nav_alerts);
        LinearLayout navProfile = view.findViewById(R.id.nav_profile);

        // Define colors matching your XML
        int activeColor = Color.parseColor("#155A91"); // trust_blue
        int inactiveColor = Color.parseColor("#8E8E8E"); // cool_grey

        if (navHome != null) {
            // Set Home to inactive
            ((ImageView) navHome.getChildAt(0)).setColorFilter(inactiveColor);
            ((TextView) navHome.getChildAt(1)).setTextColor(inactiveColor);

            navHome.setOnClickListener(v ->
                    Navigation.findNavController(view).navigate(R.id.patientDashboardFragment)
            );
        }

        if (navAlerts != null) {
            // Set Alerts to inactive
            View alertsIconContainer = navAlerts.getChildAt(0);
            if(alertsIconContainer instanceof android.widget.FrameLayout) {
                View icon = ((android.widget.FrameLayout) alertsIconContainer).getChildAt(0);
                if(icon instanceof ImageView) {
                    ((ImageView) icon).setColorFilter(inactiveColor);
                }
            } else if (alertsIconContainer instanceof ImageView) {
                ((ImageView) alertsIconContainer).setColorFilter(inactiveColor);
            }
            ((TextView) navAlerts.getChildAt(1)).setTextColor(inactiveColor);

            navAlerts.setOnClickListener(v ->
                    Navigation.findNavController(view).navigate(R.id.patientAlertsFragment)
            );
        }

        if (navProfile != null) {
            // Set Profile to ACTIVE (Current Screen)
            ((ImageView) navProfile.getChildAt(0)).setColorFilter(activeColor);
            ((TextView) navProfile.getChildAt(1)).setTextColor(activeColor);

            // Do nothing on click since we are already here
            navProfile.setOnClickListener(v -> {});
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

                    if (tvProfileName != null) tvProfileName.setText(currentName != null ? currentName : "Unknown");
                    if (tvProfileRole != null) tvProfileRole.setText(role != null ? role : "Parent/Guardian");
                    if (tvProfilePhone != null) tvProfilePhone.setText(currentPhone != null && !currentPhone.isEmpty() ? currentPhone : "Not provided");
                    if (tvProfileEmail != null) tvProfileEmail.setText(email != null && !email.isEmpty() ? email : "Not provided");
                    if (tvProfileAddress != null) tvProfileAddress.setText(currentAddress != null && !currentAddress.isEmpty() ? currentAddress : "Not provided");
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

        View btnCancel = dialogView.findViewById(R.id.btnCancel);
        View btnSave = dialogView.findViewById(R.id.btnSave);

        if(etName != null) etName.setText(currentName);
        if(etPhone != null) etPhone.setText(currentPhone);
        if(etAddress != null) etAddress.setText(currentAddress);

        if(btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        if(btnSave != null) {
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
                        fetchPatientProfile();
                    } else {
                        Toast.makeText(getContext(), "Update failed.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }
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

        View btnCancel = dialogView.findViewById(R.id.btnCancel);
        View btnUpdate = dialogView.findViewById(R.id.btnUpdate);

        if(btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        if(btnUpdate != null) {
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
        }
        dialog.show();
    }
}