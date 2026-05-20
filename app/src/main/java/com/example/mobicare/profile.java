package com.example.mobicare;

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
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

// Import for MaterialCardView
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class profile extends Fragment {

    private DatabaseReference adminRef;
    private String loggedInAdminId;

    private TextView tvAdminName, tvAdminEmail, tvAdminPhone, tvAdminAddress;

    // Variables to hold current data for pre-filling the dialog
    private String currentName = "System Administrator";
    private String currentEmail = "Not set";
    private String currentPhone = "Not set";
    private String currentAddress = "Not set";

    public profile() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Read SharedPreferences to see who logged in
        SharedPreferences prefs = requireActivity().getSharedPreferences("MobicarePrefs", Context.MODE_PRIVATE);
        loggedInAdminId = prefs.getString("loggedInUser", null);

        // If no one is logged in, kick them back to the login screen
        if (loggedInAdminId == null) {
            Toast.makeText(getContext(), "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigate(R.id.loginFragment);
            return;
        }

        // Now points exactly to the "Admin" node instead of "Users"
        adminRef = FirebaseDatabase.getInstance().getReference("Admin").child(loggedInAdminId);

        // UI Setup
        ImageView btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        tvAdminName = view.findViewById(R.id.tvAdminName);
        tvAdminEmail = view.findViewById(R.id.tvAdminEmail);
        tvAdminPhone = view.findViewById(R.id.tvAdminPhone);
        tvAdminAddress = view.findViewById(R.id.tvAdminAddress);

        MaterialCardView btnEditProfile = view.findViewById(R.id.btnEditProfile);
        MaterialCardView btnChangePassword = view.findViewById(R.id.btnChangePassword);
        ImageView btnLogout = view.findViewById(R.id.btnLogout);

        setupBottomNavigation(view);

        // Load Data
        loadAdminProfile();

        // Button Clicks
        if (btnEditProfile != null) btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        if (btnChangePassword != null) btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // ---> FIXED: Now this actually calls your popup method! <---
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        }
    }

    private void loadAdminProfile() {
        adminRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {

                    currentName = snapshot.child("fullName").getValue(String.class);
                    currentEmail = snapshot.child("email").getValue(String.class);
                    currentAddress = snapshot.child("address").getValue(String.class);

                    // Safely grab the phone number whether it is a Long or a String
                    Object phoneObj = snapshot.child("phone").getValue();
                    if (phoneObj != null) {
                        currentPhone = String.valueOf(phoneObj);
                    } else {
                        currentPhone = "Not set";
                    }

                    if (currentName != null) tvAdminName.setText(currentName);
                    if (currentEmail != null) tvAdminEmail.setText(currentEmail);
                    if (currentPhone != null) tvAdminPhone.setText(currentPhone);
                    if (currentAddress != null) tvAdminAddress.setText(currentAddress);
                } else {
                    tvAdminName.setText("Admin not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load profile data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditProfileDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        EditText etEditName = dialogView.findViewById(R.id.etEditName);
        EditText etEditPhone = dialogView.findViewById(R.id.etEditPhone);
        EditText etEditAddress = dialogView.findViewById(R.id.etEditAddress);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSaveEdit);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelEdit);

        if (currentName != null && !currentName.equals("System Administrator")) etEditName.setText(currentName);
        if (currentPhone != null && !currentPhone.equals("Not set")) etEditPhone.setText(currentPhone);
        if (currentAddress != null && !currentAddress.equals("Not set")) etEditAddress.setText(currentAddress);

        btnSave.setOnClickListener(v -> {
            String newName = etEditName.getText().toString().trim();
            String newPhone = etEditPhone.getText().toString().trim();
            String newAddress = etEditAddress.getText().toString().trim();

            if (!newName.isEmpty()) {
                HashMap<String, Object> updates = new HashMap<>();
                updates.put("fullName", newName);
                updates.put("phone", newPhone.isEmpty() ? "Not set" : newPhone);
                updates.put("address", newAddress.isEmpty() ? "Not set" : newAddress);

                adminRef.updateChildren(updates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(getContext(), "Update failed.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showChangePasswordDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSavePassword);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelPassword);

        btnSave.setOnClickListener(v -> {
            String newPass = etNewPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            if (newPass.length() < 6) {
                Toast.makeText(getContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPass.equals(confirmPass)) {
                adminRef.child("password").setValue(newPass).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Password updated successfully!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(getContext(), "Failed to update password.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setupBottomNavigation(View view) {
        LinearLayout navHome = view.findViewById(R.id.nav_home);
        LinearLayout navInventory = view.findViewById(R.id.nav_inventory);
        LinearLayout navAlerts = view.findViewById(R.id.nav_alerts);
        LinearLayout navProfile = view.findViewById(R.id.nav_profile);

        int activeColor = Color.parseColor("#155A91"); // Blue
        int inactiveColor = Color.parseColor("#8E8E8E"); // Grey

        if (navHome != null) {
            ((ImageView) navHome.getChildAt(0)).setColorFilter(inactiveColor);
            ((TextView) navHome.getChildAt(1)).setTextColor(inactiveColor);
            navHome.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_adminProfileFragment_to_adminDashboardFragment));
        }

        if (navInventory != null) {
            ((ImageView) navInventory.getChildAt(0)).setColorFilter(inactiveColor);
            ((TextView) navInventory.getChildAt(1)).setTextColor(inactiveColor);
            navInventory.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_adminProfileFragment_to_inventoryFragment));
        }

        if (navAlerts != null) {
            View alertsIcon = ((android.widget.FrameLayout) navAlerts.getChildAt(0)).getChildAt(0);
            if (alertsIcon instanceof ImageView) ((ImageView) alertsIcon).setColorFilter(inactiveColor);
            ((TextView) navAlerts.getChildAt(1)).setTextColor(inactiveColor);
            navAlerts.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_adminProfileFragment_to_adminAlertsFragment));
        }

        if (navProfile != null) {
            ((ImageView) navProfile.getChildAt(0)).setColorFilter(activeColor);
            ((TextView) navProfile.getChildAt(1)).setTextColor(activeColor);
            navProfile.setOnClickListener(v -> {}); // Already on Profile
        }
    }

    private void showLogoutConfirmation() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_logout, null);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnCancelLogout).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnConfirmLogout).setOnClickListener(v -> {
            dialog.dismiss();

            // 1. Sign out of Firebase (Important for Mothers who use Firebase Auth)
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();

            // 2. CRITICAL: Clear the SharedPreferences using the exact lowercase 'c'
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("MobicarePrefs", android.content.Context.MODE_PRIVATE);
            prefs.edit().clear().apply();

            // 3. Navigate back to Login
            androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.loginFragment);
            android.widget.Toast.makeText(getContext(), "Logged out successfully", android.widget.Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }
}