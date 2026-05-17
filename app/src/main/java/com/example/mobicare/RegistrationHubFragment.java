package com.example.mobicare;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import android.widget.AutoCompleteTextView;

// ADDED: Missing Import for BottomNavigationView
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class RegistrationHubFragment extends Fragment {

    private View layoutGuardian, layoutChild;
    private MaterialCardView cardGuardian, cardChild;
    private TextView tvSubtitle;

    // Guardian Form Fields (Initialized from the include)
    private EditText etMotherName, etMotherBirthdate, etMotherAge, etMotherPhone;
    private Spinner spinnerCivilStatus;
    private AutoCompleteTextView autoCompleteAccount;
    private java.util.List<UserAccount> userAccountList = new java.util.ArrayList<>();
    private String linkedUid = "";
    private String currentUid = "";
    private AutoCompleteTextView autoCompleteGuardianSearch;
    private Button btnAddMother;

    // Child Form Fields
    private EditText etChildFirstName, etChildMiddleName, etChildLastName;
    private EditText etChildBirthdate, etChildBirthplace;
    private Spinner spinnerGender;
    private Button btnAddChild;
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        return inflater.inflate(R.layout.fragment_registration_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("MobiCarePrefs", android.content.Context.MODE_PRIVATE);
        currentUid = prefs.getString("loggedUserKey", "");

        // 1. Hub UI
        tvSubtitle = view.findViewById(R.id.tvSubtitle);
        cardGuardian = view.findViewById(R.id.cardToggleGuardian);
        cardChild = view.findViewById(R.id.cardToggleChild);
        layoutGuardian = view.findViewById(R.id.includeGuardianForm);
        layoutChild = view.findViewById(R.id.includeChildForm);

        // 2. INITIALIZE GUARDIAN VIEWS
        etMotherName = view.findViewById(R.id.etFullName);
        etMotherBirthdate = view.findViewById(R.id.etBirthdate);
        etMotherAge = view.findViewById(R.id.etAge);
        spinnerCivilStatus = view.findViewById(R.id.spinnerCivilStatus);
        etMotherPhone = view.findViewById(R.id.etPhone);
        btnAddMother = view.findViewById(R.id.btnAddMother);
        autoCompleteAccount = view.findViewById(R.id.autoCompleteAccount);
        autoCompleteGuardianSearch = view.findViewById(R.id.autoCompleteGuardianSearch);

        // Fetch user accounts
        fetchUserAccounts();

        autoCompleteGuardianSearch.setOnItemClickListener((parent, v, position, id) -> {
            UserAccount selected = userAccountList.get(position);
            this.linkedUid = selected.uid;
            Toast.makeText(requireContext(), "Linked to: " + selected.name, Toast.LENGTH_SHORT).show();
        });

        // 3. INITIALIZE CHILD VIEWS
        etChildFirstName = view.findViewById(R.id.etChildFirstName);
        etChildMiddleName = view.findViewById(R.id.etChildMiddleName);
        etChildLastName = view.findViewById(R.id.etChildLastName);
        etChildBirthdate = view.findViewById(R.id.etChildBirthdate);
        etChildBirthplace = view.findViewById(R.id.etPlaceOfBirth);
        spinnerGender = view.findViewById(R.id.spinnerGender);
        btnAddChild = view.findViewById(R.id.btnAddChild);

        // 4. SETUP SPINNERS
        setupSpinners();

        // 5. ATTACH ALL LISTENERS
        attachListeners();

        // ---> FIXED: BOTTOM NAVIGATION MOVED HERE <---
        BottomNavigationView bottomNav = view.findViewById(R.id.bottomNavHealthWorker);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();

                if (id == R.id.healthWorkerDashboardFragment) {
                    Navigation.findNavController(view).navigate(R.id.healthWorkerDashboardFragment);
                    return true;
                }
                else if (id == R.id.addConsultationFragment) {
                    Navigation.findNavController(view).navigate(R.id.addConsultationFragment);
                    return true;
                }
                else if (id == R.id.profileFragment) {
                    Navigation.findNavController(view).navigate(R.id.profileFragment);
                    return true;
                }

                return false;
            });
        }

        // 6. STARTUP STATE
        cardGuardian.performClick();
    }

    private void setupSpinners() {
        String[] statusOptions = {"Single", "Married", "Widowed", "Separated"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, statusOptions);
        spinnerCivilStatus.setAdapter(adapter);

        String[] genders = {"Male", "Female"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, genders);
        spinnerGender.setAdapter(genderAdapter);
    }

    private void attachListeners() {
        etMotherBirthdate.setOnClickListener(v -> showDatePicker(etMotherBirthdate, etMotherAge));
        etChildBirthdate.setOnClickListener(v -> showDatePicker(etChildBirthdate, null));

        btnAddMother.setOnClickListener(v -> saveGuardianData());
        btnAddChild.setOnClickListener(v -> saveChildData());

        getView().findViewById(R.id.btnBackRegistration).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        cardGuardian.setOnClickListener(v -> {
            layoutGuardian.setVisibility(View.VISIBLE);
            layoutChild.setVisibility(View.GONE);
            tvSubtitle.setText("Registering Parent or Guardian");
            updateCardStyles(cardGuardian, cardChild, "#1B75BC");
        });

        cardChild.setOnClickListener(v -> {
            layoutGuardian.setVisibility(View.GONE);
            layoutChild.setVisibility(View.VISIBLE);
            tvSubtitle.setText("Registering Child Patient");
            updateCardStyles(cardChild, cardGuardian, "#2E7D32");
        });
    }

    private void showDatePicker(EditText dateField, EditText ageField) {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Birthdate")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(selection);
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            dateField.setText(format.format(calendar.getTime()));

            if (ageField != null) {
                Calendar today = Calendar.getInstance();
                int age = today.get(Calendar.YEAR) - calendar.get(Calendar.YEAR);
                if (today.get(Calendar.DAY_OF_YEAR) < calendar.get(Calendar.DAY_OF_YEAR)) {
                    age--;
                }
                ageField.setText(String.valueOf(age));
            }
        });

        datePicker.show(getChildFragmentManager(), "DATE_PICKER");
    }

    private void saveGuardianData() {
        String name = etMotherName.getText().toString().trim();
        String phone = etMotherPhone.getText().toString().trim();
        String bday = etMotherBirthdate.getText().toString().trim();
        String age = etMotherAge.getText().toString().trim();
        String civilStatus = (spinnerCivilStatus.getSelectedItem() != null) ?
                spinnerCivilStatus.getSelectedItem().toString() : "Select Status";

        EditText etAddress = getView().findViewById(R.id.etAddress);
        EditText etEmail = getView().findViewById(R.id.etEmail);
        EditText etEmergency = getView().findViewById(R.id.etEmergencyContact);
        EditText etHistory = getView().findViewById(R.id.etMedicalHistory);

        String address = etAddress.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String emergency = etEmergency.getText().toString().trim();
        String history = etHistory.getText().toString().trim();

        if (name.isEmpty() || bday.isEmpty() || address.isEmpty() || phone.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (linkedUid.isEmpty()) {
            Toast.makeText(getContext(), "Please link an account from the search first!", Toast.LENGTH_LONG).show();
            autoCompleteAccount.requestFocus();
            return;
        }

        java.util.HashMap<String, Object> motherMap = new java.util.HashMap<>();
        motherMap.put("fullName", name);
        motherMap.put("linkedUid", linkedUid);
        motherMap.put("birthDate", bday);
        motherMap.put("age", age);
        motherMap.put("address", address);
        motherMap.put("phone", phone);
        motherMap.put("civilStatus", civilStatus);
        motherMap.put("email", email);
        motherMap.put("emergencyContact", emergency);
        motherMap.put("medicalHistory", history);

        mDatabase.child("Patients_Guardians").child(linkedUid).setValue(motherMap)
                .addOnSuccessListener(aVoid -> {
                    logRecentActivity("Patient Registered", "Registered Guardian: " + name);
                    Toast.makeText(getContext(), "Guardian Registered Successfully!", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void saveChildData() {
        if (linkedUid == null || linkedUid.isEmpty()) {
            Toast.makeText(getContext(), "Error: Please link an account first!", Toast.LENGTH_LONG).show();
            cardGuardian.performClick();
            return;
        }

        String fName = etChildFirstName.getText().toString().trim();
        String mName = etChildMiddleName.getText().toString().trim();
        String lName = etChildLastName.getText().toString().trim();
        String bday = etChildBirthdate.getText().toString().trim();
        String place = etChildBirthplace.getText().toString().trim();
        String gender = (spinnerGender.getSelectedItem() != null) ?
                spinnerGender.getSelectedItem().toString() : "";

        if (fName.isEmpty() || lName.isEmpty() || bday.isEmpty() || place.isEmpty() || gender.equals("Select Gender")) {
            Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Child child = new Child(fName, mName, lName, bday, gender, place, linkedUid);

        String childId = mDatabase.child("Patients_Children").push().getKey();
        if (childId != null) {
            mDatabase.child("Patients_Children").child(childId).setValue(child)
                    .addOnSuccessListener(aVoid -> {
                        logRecentActivity("Patient Registered", "Registered Child: " + fName + " " + lName);
                        Toast.makeText(getContext(), "Child Registered Successfully!", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).navigateUp();
                    });
        }
    }

    private void fetchUserAccounts() {
        mDatabase.child("Users").orderByChild("role").equalTo("Parent/Guardian")
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        userAccountList.clear();
                        java.util.List<String> namesAndUsernames = new java.util.ArrayList<>();

                        for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                            String name = ds.child("fullName").getValue(String.class);
                            String username = ds.child("username").getValue(String.class);
                            String uid = ds.child("uid").getValue(String.class);

                            userAccountList.add(new UserAccount(name, username, uid));
                            namesAndUsernames.add(name + " (@" + username + ")");
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                                android.R.layout.simple_dropdown_item_1line, namesAndUsernames);

                        autoCompleteAccount.setAdapter(adapter);
                        autoCompleteGuardianSearch.setAdapter(adapter);

                        autoCompleteAccount.setOnItemClickListener((parent, v, position, id) -> {
                            UserAccount selected = userAccountList.get(position);
                            etMotherName.setText(selected.name);
                            linkedUid = selected.uid;
                            Toast.makeText(getContext(), "Account Linked!", Toast.LENGTH_SHORT).show();
                        });
                    }
                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
                });
    }

    class UserAccount {
        String name, username, uid;
        UserAccount(String n, String u, String id) { this.name = n; this.username = u; this.uid = id; }
    }

    private void updateCardStyles(MaterialCardView selected, MaterialCardView unselected, String color) {
        selected.setStrokeWidth(6);
        selected.setStrokeColor(Color.parseColor(color));
        unselected.setStrokeWidth(2);
        unselected.setStrokeColor(Color.LTGRAY);
    }

    private void logRecentActivity(String type, String description) {
        String uid = currentUid;

        if (uid == null || uid.isEmpty()) {
            if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
                uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
            } else {
                return;
            }
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Recent_Activities").child(uid);
        String activityId = ref.push().getKey();

        java.util.HashMap<String, Object> activityData = new java.util.HashMap<>();
        activityData.put("type", type);
        activityData.put("description", description);
        activityData.put("timestamp", com.google.firebase.database.ServerValue.TIMESTAMP);

        if (activityId != null) {
            ref.child(activityId).setValue(activityData);
        }
    }
}