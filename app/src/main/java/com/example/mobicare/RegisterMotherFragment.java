package com.example.mobicare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class RegisterMotherFragment extends Fragment {

    private EditText etFullName, etBirthdate, etAge, etAddress, etPhone,
            etEmail, etEmergencyContact, etMedicalHistory;
    private Spinner spinnerCivilStatus;
    //private Button btnNext;
    private Button btnAddMother;
    private DatabaseReference mDatabase;

    public RegisterMotherFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Initialize Database inside onCreateView or onViewCreated
        mDatabase = FirebaseDatabase.getInstance().getReference();
        return inflater.inflate(R.layout.fragment_register_mother, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize all Views
        etFullName = view.findViewById(R.id.etFullName);
        etBirthdate = view.findViewById(R.id.etBirthdate);
        etAge = view.findViewById(R.id.etAge);
        etAddress = view.findViewById(R.id.etAddress);
        etPhone = view.findViewById(R.id.etPhone);
        etEmail = view.findViewById(R.id.etEmail);
        etEmergencyContact = view.findViewById(R.id.etEmergencyContact);
        etMedicalHistory = view.findViewById(R.id.etMedicalHistory);
        spinnerCivilStatus = view.findViewById(R.id.spinnerCivilStatus);
        btnAddMother = view.findViewById(R.id.btnAddMother);

        // 3. Setup Civil Status Spinner
        String[] statusOptions = {"Single", "Married", "Widowed", "Separated"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                statusOptions
        );
        spinnerCivilStatus.setAdapter(adapter);

        // 4. Date Picker for Birthdate
        etBirthdate.setOnClickListener(v -> showDatePicker());


        btnAddMother.setOnClickListener(v -> {

            android.util.Log.d("HUB_DEBUG", "CLICK REACHED MOTHER FRAGMENT");
            // 1. Use your full validation method
            if (!validateInputs()) {
                return; // Stops here if any required field is empty
            }

            // 2. Get data from fields (now that we know they aren't empty)
            String name = etFullName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String bday = etBirthdate.getText().toString().trim();
            String age = etAge.getText().toString().trim();
            String addr = etAddress.getText().toString().trim();

            // Safety check for Spinner to prevent null errors
            String status = (spinnerCivilStatus.getSelectedItem() != null)
                    ? spinnerCivilStatus.getSelectedItem().toString()
                    : "Select";

            String email = etEmail.getText().toString().trim();
            String emergency = etEmergencyContact.getText().toString().trim();
            String history = etMedicalHistory.getText().toString().trim();

            // 3. Create Mother Object
            Mother mother = new Mother(name, bday, age, addr, phone, status, email, emergency, history, "");

            // 4. Save to Firebase
            mDatabase.child("Patients_Guardians").child(phone).setValue(mother)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), "Guardian Registered Successfully!", Toast.LENGTH_SHORT).show();

                        // This will take you back to the Dashboard Hub
                        Navigation.findNavController(v).navigateUp();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Mother's Birthdate")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(selection);
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            etBirthdate.setText(format.format(calendar.getTime()));

            calculateAge(calendar);
        });

        datePicker.show(getChildFragmentManager(), "DATE_PICKER");
    }

    private void calculateAge(Calendar birthDate) {
        Calendar today = Calendar.getInstance(); // Local time

        Calendar localBirthDate = Calendar.getInstance();
        localBirthDate.setTimeInMillis(birthDate.getTimeInMillis());

        int age = today.get(Calendar.YEAR) - localBirthDate.get(Calendar.YEAR);

        // Check if the birthday hasn't happened yet this year
        if (today.get(Calendar.MONTH) < localBirthDate.get(Calendar.MONTH) ||
                (today.get(Calendar.MONTH) == localBirthDate.get(Calendar.MONTH) &&
                        today.get(Calendar.DAY_OF_MONTH) < localBirthDate.get(Calendar.DAY_OF_MONTH))) {
            age--;
        }

        etAge.setText(String.valueOf(age));
    }

    private boolean validateInputs() {
        if (etFullName.getText().toString().trim().isEmpty()) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return false;
        }
        if (etBirthdate.getText().toString().trim().isEmpty()) {
            etBirthdate.setError("Birthdate is required");
            etBirthdate.requestFocus();
            return false;
        }
        if (etAddress.getText().toString().trim().isEmpty()) {
            etAddress.setError("Address is required");
            etAddress.requestFocus();
            return false;
        }
        if (etPhone.getText().toString().trim().isEmpty()) {
            etPhone.setError("Phone number is required");
            etPhone.requestFocus();
            return false;
        }
        return true;
    }
}