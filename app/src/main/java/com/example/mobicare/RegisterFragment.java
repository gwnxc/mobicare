package com.example.mobicare;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private TextInputEditText etFullName, etUsername, etPassword, etConfirmPassword;
    private Button btnRegister;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize Views
        etFullName = view.findViewById(R.id.etFullName);
        etUsername = view.findViewById(R.id.etUsernameReg);
        etPassword = view.findViewById(R.id.etPasswordReg);
        etConfirmPassword = view.findViewById(R.id.etConfirmPasswordReg);
        btnRegister = view.findViewById(R.id.btnRegister);

        // Back to Login
        view.findViewById(R.id.tvBackToLogin).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp()
        );

        btnRegister.setOnClickListener(v -> registerUser(view));
    }

    private void registerUser(View view) {
        String name = etFullName.getText().toString().trim();
        String user = etUsername.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        // 1. Validation Checks
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(user) || TextUtils.isEmpty(pass)) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pass.equals(confirmPass)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        // 2. Create Firebase Account
        // Note: Firebase uses Email. If your username isn't an email,
        // you can append "@mobicare.com" for internal logic.
        String dummyEmail = user + "@mobicare.com";

        mAuth.createUserWithEmailAndPassword(dummyEmail, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // 3. Unique ID Generation
                        String userId = mAuth.getCurrentUser().getUid();
                        // Create an organized key (e.g., Patient_ABC123)
                        String organizedKey = "User_" + userId;
                        // 4. Save to Database with Patient Role
                        HashMap<String, Object> userMap = new HashMap<>();
                        userMap.put("fullName", name);
                        userMap.put("username", user);
                        userMap.put("role", "Parent/Guardian"); // Locked role
                        userMap.put("uid", userId);

                        mDatabase.child("Users").child(organizedKey).setValue(userMap)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Account Created!", Toast.LENGTH_SHORT).show();
                                    Navigation.findNavController(view).navigateUp();
                                });
                    } else {
                        // 5. Duplicate Account Prevention
                        // Firebase automatically fails if the "email" (username) already exists.
                        Toast.makeText(getContext(), "Registration failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}