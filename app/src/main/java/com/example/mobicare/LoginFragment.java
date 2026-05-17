package com.example.mobicare;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private TextInputEditText etUsername, etPassword;
    private MaterialButtonToggleGroup toggleGroup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize Views
        etUsername = view.findViewById(R.id.etUsername);
        etPassword = view.findViewById(R.id.etPassword);
        toggleGroup = view.findViewById(R.id.toggleGroup);
        MaterialButton btnLogin = view.findViewById(R.id.btnLogin);

        // Clear fields when role changes
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (etUsername != null) etUsername.setText("");
                if (etPassword != null) etPassword.setText("");
            }
        });

        // Sign Up Link
        view.findViewById(R.id.tvSignUp).setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_registerFragment);
        });

        // Main Login Logic
        btnLogin.setOnClickListener(v -> {
            String identifier = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            int checkedId = toggleGroup.getCheckedButtonId();

            if (identifier.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (checkedId == -1) {
                Toast.makeText(getContext(), "Please select a role", Toast.LENGTH_SHORT).show();
                return;
            }

            // Determine role string based on the button selected
            String selectedRole = "";
            if (checkedId == R.id.btnHealthWorker) selectedRole = "Health Worker";
            else if (checkedId == R.id.btnMother) selectedRole = "Mother";
            else if (checkedId == R.id.btnAdmin) selectedRole = "Admin";

            performLogin(identifier, password, selectedRole, view);
        });
    }

    private void performLogin(String identifier, String password, String selectedRole, View view) {
        // MOTHERS: Use Firebase Auth (Email login)
        if (selectedRole.equals("Mother")) {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
                Toast.makeText(getContext(), "Please enter a valid email for Parent login", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(identifier, password)
                    .addOnCompleteListener(requireActivity(), task -> {
                        if (task.isSuccessful()) {
                            String userId = mAuth.getCurrentUser().getUid();
                            saveUserKeyAndNavigate(selectedRole, userId, view);
                        } else {
                            Toast.makeText(getContext(), "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
        // ADMIN & HEALTH WORKERS: Use Realtime DB (Username login)
        else {
            String node = selectedRole.equals("Admin") ? "Admin" : "HealthWorkers";

            mDatabase.child(node).orderByChild("username").equalTo(identifier)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                DataSnapshot userSnap = snapshot.getChildren().iterator().next();
                                String dbPassword = userSnap.child("password").getValue(String.class);

                                if (dbPassword != null && dbPassword.equals(password)) {
                                    saveUserKeyAndNavigate(selectedRole, userSnap.getKey(), view);
                                } else {
                                    Toast.makeText(getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(getContext(), "User not found in " + node, Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }
    }

    private void saveUserKeyAndNavigate(String role, String userKey, View view) {
        // FIX 1: Change "MobiCarePrefs" to "MobicarePrefs" (lowercase 'c') to match patientDashboardFragment
        SharedPreferences prefs = requireActivity().getSharedPreferences("MobicarePrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // FIX 2: Store BOTH key names so both your Dashboard and your History layout sections can read the ID
        editor.putString("loggedUserKey", userKey);  // Used by healthRecordsFragment & MyRecordFragment
        editor.putString("loggedInUser", userKey);   // Used by patientDashboardFragment

        // Map UI roles neatly into authorization strings
        if ("Mother".equals(role)) {
            editor.putString("userRole", "Patient"); // Record fragments check for "Patient"
        } else {
            editor.putString("userRole", role); // Saves exactly "Health Worker" or "Admin"
        }
        editor.apply();

        // Single switch to handle all navigations
        switch (role) {
            case "Health Worker":
                Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_healthWorkerDashboardFragment);
                break;
            case "Mother":
                Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_patientDashboardFragment);
                break;
            case "Admin":
                Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_adminDashboardFragment);
                break;
        }
    }
}