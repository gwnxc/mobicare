package com.example.mobicare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView; // Added
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.database.DataSnapshot; // Added
import com.google.firebase.database.DatabaseError; // Added
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener; // Added

import java.text.SimpleDateFormat;
import java.util.ArrayList; // Added
import java.util.Calendar;
import java.util.List; // Added
import java.util.Locale;
import java.util.TimeZone;

public class RegisterChildFragment extends Fragment {

    private EditText etFirstName, etMiddleName, etLastName, etBirthdate, etPlaceOfBirth;
    private AutoCompleteTextView autoCompleteGuardianSearch; // Added
    private Spinner spinnerGender;
    private Button btnAddChild;

    private DatabaseReference mDatabase;
    private List<String> guardianNames = new ArrayList<>(); // Added
    private ArrayAdapter<String> guardianAdapter; // Added

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register_child, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // 1. Initialize Views
        etFirstName = view.findViewById(R.id.etChildFirstName);
        etMiddleName = view.findViewById(R.id.etChildMiddleName);
        etLastName = view.findViewById(R.id.etChildLastName);
        etBirthdate = view.findViewById(R.id.etChildBirthdate);
        etPlaceOfBirth = view.findViewById(R.id.etPlaceOfBirth);
        spinnerGender = view.findViewById(R.id.spinnerGender);

        btnAddChild = view.findViewById(R.id.btnAddChild);

        // Initialize Guardian Search field
        autoCompleteGuardianSearch = view.findViewById(R.id.autoCompleteGuardianSearch);

        // 2. Setup Gender Spinner
        String[] genders = {"Male", "Female"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, genders);
        spinnerGender.setAdapter(adapter);

        // 3. Setup Guardian Search Adapter
        guardianAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, guardianNames);
        autoCompleteGuardianSearch.setAdapter(guardianAdapter);
        autoCompleteGuardianSearch.setThreshold(1);

        // Load the names from Firebase
        loadGuardiansFromFirebase();

        etBirthdate.setOnClickListener(v -> showDatePicker());

        // 4. Save Button
        btnAddChild.setOnClickListener(v -> {
            processNewChildRegistration();
        });
    }

    private void loadGuardiansFromFirebase() {
        mDatabase.child("Patients_Guardians").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                guardianNames.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String name = ds.child("fullName").getValue(String.class);
                    if (name != null) guardianNames.add(name);
                }

                // CRITICAL: This refreshes the search bar list immediately
                if (guardianAdapter != null) {
                    guardianAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void processNewChildRegistration() {
        String fName = etFirstName.getText().toString().trim();
        String mName = etMiddleName.getText().toString().trim();
        String lName = etLastName.getText().toString().trim();
        String bday = etBirthdate.getText().toString().trim();
        String gender = (spinnerGender.getSelectedItem() != null) ? spinnerGender.getSelectedItem().toString() : "Male";
        String place = etPlaceOfBirth.getText().toString().trim();
        String guardianName = autoCompleteGuardianSearch.getText().toString().trim();

        if (fName.isEmpty() || lName.isEmpty() || bday.isEmpty() || guardianName.isEmpty()) {
            Toast.makeText(getContext(), "All fields required!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Using the Child class again, but making sure we pass 'guardianName'
        Child child = new Child(fName, mName, lName, bday, gender, place, guardianName);

        mDatabase.child("Patients_Children").push().setValue(child)
                .addOnSuccessListener(aVoid -> {
                    // If you see this, the build finally updated!
                    Toast.makeText(getContext(), "FORCE UPDATE SUCCESS", Toast.LENGTH_LONG).show();
                    Navigation.findNavController(requireView()).navigateUp();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Child's Birthdate")
                .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(selection);
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            etBirthdate.setText(format.format(calendar.getTime()));
        });
        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }
}