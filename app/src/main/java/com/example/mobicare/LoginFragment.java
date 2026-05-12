package com.example.mobicare;

import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.firebase.database.ValueEventListener;

public class LoginFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private TextInputEditText etUsername, etPassword;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        etUsername = view.findViewById(R.id.etUsername);
        etPassword = view.findViewById(R.id.etPassword);
        MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.toggleGroup);
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                // Clear the text from the input fields
                if (etUsername != null) etUsername.setText("");
                if (etPassword != null) etPassword.setText("");

                // Optional: Remove error icons if any were showing
                view.findViewById(R.id.tilUsername).setActivated(false);
                view.findViewById(R.id.tilPassword).setActivated(false);

                //Toast.makeText(getContext(), "Role changed: Credentials cleared", Toast.LENGTH_SHORT).show();
            }
        });
        MaterialButton btnLogin = view.findViewById(R.id.btnLogin);

        view.findViewById(R.id.tvSignUp).setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_registerFragment);
        });

        btnLogin.setOnClickListener(v -> {
            String user = etUsername.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            int checkedId = toggleGroup.getCheckedButtonId();

            if (TextUtils.isEmpty(user) || TextUtils.isEmpty(pass) || checkedId == -1) {
                Toast.makeText(getContext(), "Please fill all fields and select a role", Toast.LENGTH_SHORT).show();
                return;
            }

            loginUser(user, pass, checkedId, view);
        });
    }

    private void loginUser(String usernameInput, String pass, int checkedId, View view) {
        String node = (checkedId == R.id.btnHealthWorker) ? "HealthWorkers" : "Users";

        mDatabase.child(node).orderByChild("username").equalTo(usernameInput)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            DataSnapshot userSnap = snapshot.getChildren().iterator().next();

                            // Get the password directly from the database string
                            String dbPassword = userSnap.child("password").getValue(String.class);

                            if (dbPassword != null && dbPassword.equals(pass)) {
                                // 1. Get the Unique ID from the database
                                String userKey = userSnap.getKey();

                                // 2. PASTE THIS HERE: Save it so other fragments can find Maria
                                android.content.SharedPreferences prefs = requireContext().getSharedPreferences("MobiCarePrefs", android.content.Context.MODE_PRIVATE);
                                prefs.edit().putString("loggedUserKey", userKey).apply();
                                // SUCCESS: Password matches the database string
                                if (checkedId == R.id.btnHealthWorker) {
                                    Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_healthWorkerDashboardFragment);
                                } else {
                                    // For parents, we still use your checkUserRole logic
                                    checkUserRole(userSnap.getKey(), checkedId, view);
                                }
                            } else {
                                Toast.makeText(getContext(), "Password incorrect", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "Username not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void checkUserRole(String uid, int checkedId, View view) {
        // FIX 1: Add "User_" prefix to match your RegisterFragment's organizedKey
        mDatabase.child("Users").child("User_" + uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String roleInDb = snapshot.child("role").getValue(String.class);

                    String selectedRole = "";
                    if (checkedId == R.id.btnHealthWorker) selectedRole = "HealthWorker";
                    else if (checkedId == R.id.btnMother) selectedRole = "Parent/Guardian"; // Matches RegisterFragment
                    else if (checkedId == R.id.btnAdmin) selectedRole = "Admin";

                    if (roleInDb != null && roleInDb.equals(selectedRole)) {
                        navigateToDashboard(selectedRole, view);
                    } else {
                        mAuth.signOut();
                        Toast.makeText(getContext(), "Incorrect role selected for this account", Toast.LENGTH_LONG).show();
                    }
                } else {
                    mAuth.signOut();
                    Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void navigateToDashboard(String role, View view) {
        if (role.equals("HealthWorker")) {
            Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_healthWorkerDashboardFragment);
        } else if (role.equals("Parent/Guardian")) { // FIX 2: Check for "Parent/Guardian" instead of "Patient"
            Toast.makeText(getContext(), "Welcome back, Parent!", Toast.LENGTH_SHORT).show();
            // Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_motherDashboardFragment);
        } else {
            Toast.makeText(getContext(), "Admin Dashboard coming soon!", Toast.LENGTH_SHORT).show();
        }
    }
}