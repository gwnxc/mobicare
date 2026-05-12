package com.example.mobicare;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class LoginFragment extends Fragment {

    private TextInputEditText etUsername, etPassword;
    private FirebaseAuth mAuth; // ADDED: Firebase Authentication

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Views
        etUsername = view.findViewById(R.id.etUsername);
        etPassword = view.findViewById(R.id.etPassword);
        MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.toggleGroup);
        MaterialButton btnLogin = view.findViewById(R.id.btnLogin);
        TextView tvSignUp = view.findViewById(R.id.tvSignUp);

        // Sign Up Link
        tvSignUp.setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_registerFragment);
        });

        // Sign In Button
        btnLogin.setOnClickListener(v -> {
            String identifier = etUsername.getText().toString().trim(); // Might be an email OR a username
            String password = etPassword.getText().toString().trim();
            int checkedId = toggleGroup.getCheckedButtonId();

            // Validation
            if (identifier.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Please enter details and password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (checkedId == -1) {
                Toast.makeText(getContext(), "Please select a role", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get selected role string
            String selectedRole = "";
            if (checkedId == R.id.btnHealthWorker) selectedRole = "Health Worker";
            else if (checkedId == R.id.btnMother) selectedRole = "Mother";
            else if (checkedId == R.id.btnAdmin) selectedRole = "Admin";

            loginUser(identifier, password, selectedRole, view);
        });
    }

    private void loginUser(String identifier, String password, String selectedRole, View view) {

        // ---> IF MOTHER: Use Firebase Authentication (Email/Password) <---
        if (selectedRole.equals("Mother")) {

            // Check if what they typed looks like an email address
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
                Toast.makeText(getContext(), "Please enter a valid email address for Mother login", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(identifier, password)
                    .addOnCompleteListener(requireActivity(), task -> {
                        if (task.isSuccessful()) {
                            // Login Success! Get the unique ID from FirebaseAuth
                            String userId = mAuth.getCurrentUser().getUid();
                            navigateToDashboard(selectedRole, userId, view);
                        } else {
                            // Login Failed
                            Toast.makeText(getContext(), "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });

        }
        // ---> IF ADMIN OR HEALTH WORKER: Use Custom Realtime DB Query (Username/Password) <---
        else {
            String nodeName = selectedRole.equals("Admin") ? "Admin" : "HealthWorkers";
            DatabaseReference targetDatabase = FirebaseDatabase.getInstance().getReference(nodeName);

            // Search INSIDE the folders for the matching username field
            Query query = targetDatabase.orderByChild("username").equalTo(identifier);

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (DataSnapshot userSnap : snapshot.getChildren()) {

                            String dbPassword = userSnap.child("password").getValue(String.class);
                            String dbRole = userSnap.child("role").getValue(String.class);

                            // We need the ACTUAL folder ID for the Profile screen to work
                            String actualNodeId = userSnap.getKey();

                            // Verify Password
                            if (dbPassword != null && dbPassword.equals(password)) {

                                // Verify Role
                                if (dbRole != null && dbRole.equalsIgnoreCase(selectedRole)) {
                                    navigateToDashboard(dbRole, actualNodeId, view);
                                } else {
                                    Toast.makeText(getContext(), "Incorrect role for this account", Toast.LENGTH_SHORT).show();
                                }

                            } else {
                                Toast.makeText(getContext(), "Wrong password", Toast.LENGTH_SHORT).show();
                            }
                            return; // Stop looping after we find the user
                        }
                    } else {
                        Toast.makeText(getContext(), "User does not exist in " + nodeName, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void navigateToDashboard(String role, String userId, View view) {
        // Save the logged-in user's ACTUAL folder ID (or Firebase Auth UID) to SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("MobicarePrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("loggedInUser", userId).apply();

        switch (role) {
            case "Health Worker":
                Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_healthWorkerDashboardFragment);
                break;
            case "Mother":
                // Navigate to the Patient Dashboard
                Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_patientDashboardFragment);
                break;
            case "Admin":
                Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_adminDashboardFragment);
                break;
        }
    }
}