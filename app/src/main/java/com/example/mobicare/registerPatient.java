package com.example.mobicare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class registerPatient extends Fragment {

    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register_patient, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Point directly to the "Mothers" node as shown in your screenshot
        mDatabase = FirebaseDatabase.getInstance().getReference("Mothers");

        // Setup Back Button
        ImageView btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        // Map all EditText fields
        EditText etFullName = view.findViewById(R.id.etFullName);
        EditText etPhone = view.findViewById(R.id.etPhone);
        EditText etAddress = view.findViewById(R.id.etAddress);
        EditText etAge = view.findViewById(R.id.etAge);
        EditText etBirthDate = view.findViewById(R.id.etBirthDate);
        EditText etCivilStatus = view.findViewById(R.id.etCivilStatus);
        EditText etEmail = view.findViewById(R.id.etEmail);
        EditText etEmergency = view.findViewById(R.id.etEmergency);
        EditText etMedical = view.findViewById(R.id.etMedical);

        MaterialButton btnSave = view.findViewById(R.id.btnSavePatient);

        btnSave.setOnClickListener(v -> {
            // Get text from fields
            String fullName = etFullName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String address = etAddress.getText().toString().trim();
            String age = etAge.getText().toString().trim();
            String birthDate = etBirthDate.getText().toString().trim();
            String civilStatus = etCivilStatus.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String emergency = etEmergency.getText().toString().trim();
            String medical = etMedical.getText().toString().trim();

            // The Phone Number is REQUIRED because it acts as the unique folder ID
            if (fullName.isEmpty() || phone.isEmpty()) {
                Toast.makeText(getContext(), "Full Name and Phone Number are required!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Structure the data exactly as shown in the Firebase JSON
            HashMap<String, Object> motherData = new HashMap<>();
            motherData.put("fullName", fullName);
            motherData.put("phone", phone);
            motherData.put("address", address);
            motherData.put("age", age);
            motherData.put("birthDate", birthDate);
            motherData.put("civilStatus", civilStatus);
            motherData.put("email", email);
            motherData.put("emergencyContact", emergency);
            motherData.put("medicalHistory", medical);

            // Create an empty dummy placeholder for the children node so it exists
            HashMap<String, Object> childrenData = new HashMap<>();
            motherData.put("children", childrenData);

            // Save to Firebase using the phone number as the root key: .child(phone)
            mDatabase.child(phone).setValue(motherData).addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    Toast.makeText(getContext(), "Mother Registered Successfully!", Toast.LENGTH_SHORT).show();
                    // Return to the dashboard automatically
                    Navigation.findNavController(view).navigateUp();
                } else {
                    Toast.makeText(getContext(), "Database Error: Failed to save.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}