package com.example.mobicare;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Removed duplicate UserAccount class from top and placed only at the bottom

public class AddConsultationFragment extends Fragment {

    // Common UI
    private LinearLayout layoutConsultation, layoutImmunization, layoutPrenatal, layoutMedicine;
    private com.google.android.material.card.MaterialCardView cardConsultation, cardImmunization, cardMedicine, cardPrenatal;
    private TextView tvReturnCheckupLabel;
    private AutoCompleteTextView autoCompletePatient;
    private MaterialButton btnSave;
    private Spinner spinnerPatientType;
    private LinearLayout layoutConsultationSearch, layoutCommonConsultation;

    // Consultation Fields
    private EditText etDate, etTime, etPurpose, etNotes;

    // Immunization Fields
    private AutoCompleteTextView autoCompleteChild, autoCompleteMother;
    private Spinner spinnerVaccineType, spinnerDoseNo, spinnerScheduledAge;
    private HashMap<String, String> childGuardianMap = new HashMap<>();
    private EditText etNextImmDate, etDateAdministered;// CHANGE THESE: From EditText to AutoCompleteTextView
    private AutoCompleteTextView etHealthWorker;
    private AutoCompleteTextView etAdminBy;
    private AutoCompleteTextView etDispensedBy;


    // Prenatal Fields
    private EditText etPrenatalWeight, etPrenatalBP, etFundalHeight, etFetalHeartTone, etDiagnosisTreatment, etReturnCheckup;
    private Spinner spinnerTetanusToxoid;
    private TextView tvSelectPatientLabel;

    // Medicine Fields & Inventory Data
    private AutoCompleteTextView autoCompleteMedicine;
    private EditText etMedDosage, etMedQty, etMedPurpose;
    private List<String> healthWorkerNames = new ArrayList<>();
    private Map<String, DataSnapshot> medicineInventoryMap = new HashMap<>();
    private LinearLayout layoutMedicineFields;
    private ArrayAdapter<String> patientAdapter;
    private ArrayAdapter<String> childAdapter;
    private ArrayAdapter<String> healthWorkerAdapter;
    // Database & Lists
    private DatabaseReference mDatabase;
    private List<String> patientNames = new ArrayList<>();
    private List<String> childNames = new ArrayList<>();
    private List<UserAccount> patientAccountList = new ArrayList<>();
    private String linkedPatientUid = "";
    private int selectedPosition = -1;
    private String currentRecordType = "Consultation";
    private String currentUid = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_consultation_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        // Get Maria's ID from SharedPreferences
        if (getContext() != null) {
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("MobiCarePrefs", android.content.Context.MODE_PRIVATE);
            currentUid = prefs.getString("loggedUserKey", "");
        }
        // Initialize Layouts
        layoutConsultation = view.findViewById(R.id.layoutConsultationFields);
        layoutImmunization = view.findViewById(R.id.layoutImmunizationFields);
        layoutPrenatal = view.findViewById(R.id.layoutPrenatalFields);
        layoutConsultationSearch = view.findViewById(R.id.layoutConsultationSearch);
        layoutCommonConsultation = view.findViewById(R.id.layoutCommonConsultation);
        layoutMedicineFields = view.findViewById(R.id.layoutMedicineFields);

        // Initialize Cards
        cardConsultation = view.findViewById(R.id.cardConsultation);
        cardImmunization = view.findViewById(R.id.cardImmunization);
        cardMedicine = view.findViewById(R.id.cardMedicine);
        cardPrenatal = view.findViewById(R.id.cardPrenatal);

        // Initialize Common Search & Save
        autoCompletePatient = view.findViewById(R.id.autoCompletePatient);
        spinnerPatientType = view.findViewById(R.id.spinnerPatientType);
        btnSave = view.findViewById(R.id.btnSaveRecord);

        // Initialize Specific Record Fields
        initConsultationFields(view);
        initImmunizationFields(view);
        initPrenatalFields(view);
        initMedicineFields(view);

        // Setup Logic
        setupPatientSelection();
        setupAdapters();
        setupImmunizationSpinners();
        setupMedicineAutocomplete();
        loadHealthWorkers();

        // Click Listeners for Pickers
        view.findViewById(R.id.btnBackConsultation).setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        etDate.setOnClickListener(v -> showDatePicker(etDate));
        etTime.setOnClickListener(v -> showTimePicker());
        etDateAdministered.setOnClickListener(v -> showDatePicker(etDateAdministered));
        if (etNextImmDate != null) etNextImmDate.setOnClickListener(v -> showDatePicker(etNextImmDate));
        if (etReturnCheckup != null) etReturnCheckup.setOnClickListener(v -> showDatePicker(etReturnCheckup));

        // Tab Switching
        cardConsultation.setOnClickListener(v -> switchTab("Consultation"));
        cardImmunization.setOnClickListener(v -> switchTab("Immunization"));
        cardMedicine.setOnClickListener(v -> switchTab("Medicine"));
        cardPrenatal.setOnClickListener(v -> switchTab("Prenatal"));

        btnSave.setOnClickListener(v -> validateAndSave());
        cardConsultation.performClick();
    }

    private void initConsultationFields(View view) {
        // 1. Link the view FIRST
        etDate = view.findViewById(R.id.etConsultDate);
        etTime = view.findViewById(R.id.etConsultTime);
        etPurpose = view.findViewById(R.id.etPurpose);
        etHealthWorker = view.findViewById(R.id.etHealthWorker); // Ensure this ID matches XML
        etNotes = view.findViewById(R.id.etNotes);

        // 2. ONLY THEN set the listener
        if (etHealthWorker != null) {
            etHealthWorker.setOnClickListener(v -> etHealthWorker.showDropDown());
        }
    }

    private void initImmunizationFields(View view) {
        autoCompleteChild = view.findViewById(R.id.autoCompleteChild);
        autoCompleteMother = view.findViewById(R.id.autoCompleteMother);
        spinnerVaccineType = view.findViewById(R.id.spinnerVaccineType);
        spinnerDoseNo = view.findViewById(R.id.spinnerDoseNo);
        spinnerScheduledAge = view.findViewById(R.id.spinnerScheduledAge);
        etDateAdministered = view.findViewById(R.id.etImmDateAdministered);

        // FIX: Find the view BEFORE setting the listener
        etAdminBy = view.findViewById(R.id.etAdminBy);
        if (etAdminBy != null) {
            etAdminBy.setOnClickListener(v -> etAdminBy.showDropDown());
        }

        etNextImmDate = view.findViewById(R.id.etNextImmDate);
    }

    private void initPrenatalFields(View view) {
        etPrenatalBP = view.findViewById(R.id.etPrenatalBP);
        etPrenatalWeight = view.findViewById(R.id.etPrenatalWeight);
        etFundalHeight = view.findViewById(R.id.etFundalHeight);
        etFetalHeartTone = view.findViewById(R.id.etFetalHeartTone);
        spinnerTetanusToxoid = view.findViewById(R.id.spinnerTetanusToxoid);
        String[] ttValues = {"None", "1st Dose", "2nd Dose", "3rd Dose", "4th Dose", "5th Dose"};
        spinnerTetanusToxoid.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, ttValues));
        etDiagnosisTreatment = view.findViewById(R.id.etDiagnosisTreatment);
        etReturnCheckup = view.findViewById(R.id.etReturnCheckup);
        tvReturnCheckupLabel = view.findViewById(R.id.tvReturnCheckupLabel);
        tvSelectPatientLabel = view.findViewById(R.id.tvSelectPatientLabel);
    }

    private void initMedicineFields(View view) {
        autoCompleteMedicine = view.findViewById(R.id.autoCompleteMedicine);
        etMedDosage = view.findViewById(R.id.etMedDosage);
        etMedQty = view.findViewById(R.id.etMedQty);
        etMedPurpose = view.findViewById(R.id.etMedPurpose);
        etDispensedBy = view.findViewById(R.id.etDispensedBy);

        // Safety check
        if (etDispensedBy != null) {
            etDispensedBy.setOnClickListener(v -> etDispensedBy.showDropDown());
        }
    }

    private void setupPatientSelection() {
        autoCompletePatient.setOnItemClickListener((parent, v, pos, id) -> {
            String selectedName = (String) parent.getItemAtPosition(pos);
            for (int i = 0; i < patientAccountList.size(); i++) {
                UserAccount selected = patientAccountList.get(i);
                if (selected.name.equalsIgnoreCase(selectedName)) {
                    selectedPosition = i;
                    linkedPatientUid = selected.uid;
                    Toast.makeText(getContext(), "Patient Linked: " + selected.name, Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        });

        autoCompleteChild.setOnItemClickListener((parent, v, pos, id) -> {
            String selectedChildName = (String) parent.getItemAtPosition(pos);
            linkedPatientUid = "";
            selectedPosition = -1;
            String guardianName = childGuardianMap.get(selectedChildName.trim());

            for (int i = 0; i < patientAccountList.size(); i++) {
                UserAccount account = patientAccountList.get(i);
                if (account.name.equalsIgnoreCase(selectedChildName)) {
                    selectedPosition = i;
                    linkedPatientUid = account.uid;
                    if (guardianName != null && !guardianName.isEmpty()) {
                        autoCompleteMother.setText(guardianName);
                    } else {
                        autoCompleteMother.setText("Guardian Not Found");
                    }
                    break;
                }
            }
        });
    }

    private void loadPatientsFromFirebase(String type) {
        String folder = type.equals("Parent/Guardian") ? "Patients_Guardians" : "Patients_Children";
        mDatabase.child(folder).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;

                patientNames.clear();
                patientAccountList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String name, uid;
                    if (type.equals("Parent/Guardian")) {
                        name = ds.child("fullName").getValue(String.class);
                        uid = ds.child("linkedUid").getValue(String.class);
                    } else {
                        String fName = ds.child("firstName").getValue(String.class);
                        String lName = ds.child("lastName").getValue(String.class);
                        name = (fName != null ? fName : "") + " " + (lName != null ? lName : "");
                        uid = ds.child("parentUid").getValue(String.class);
                    }
                    if (name != null && uid != null) {
                        patientNames.add(name);
                        patientAccountList.add(new UserAccount(name, ds.getKey(), uid));
                    }
                }

                patientAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, patientNames);
                autoCompletePatient.setAdapter(patientAdapter);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadHealthWorkers() {
        mDatabase.child("HealthWorkers").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;

                healthWorkerNames.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String name = ds.child("fullName").getValue(String.class);
                    if (name != null) healthWorkerNames.add(name);
                }

                if (getContext() != null) {
                    healthWorkerAdapter = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_dropdown_item_1line, healthWorkerNames);

                    // Attach to ALL fields across different tabs
                    if (etDispensedBy != null) etDispensedBy.setAdapter(healthWorkerAdapter);
                    if (etHealthWorker != null) etHealthWorker.setAdapter(healthWorkerAdapter);
                    if (etAdminBy != null) etAdminBy.setAdapter(healthWorkerAdapter);

                    // Auto-fill Maria's name into ALL of them
                    if (!currentUid.isEmpty()) {
                        mDatabase.child("HealthWorkers").child(currentUid).child("fullName").get()
                                .addOnSuccessListener(snap -> {
                                    if (snap.exists() && isAdded()) {
                                        String maria = snap.getValue(String.class);
                                        if (etDispensedBy != null) etDispensedBy.setText(maria);
                                        if (etHealthWorker != null) etHealthWorker.setText(maria);
                                        if (etAdminBy != null) etAdminBy.setText(maria);
                                    }
                                });
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    private void setupMedicineAutocomplete() {
        mDatabase.child("Inventory").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> availableMeds = new ArrayList<>();
                medicineInventoryMap.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String name = ds.child("itemName").getValue(String.class);
                    String qtyStr = ds.child("quantity").getValue(String.class);
                    int qty = 0;
                    try { qty = Integer.parseInt(qtyStr); } catch (Exception ignored) {}
                    if (name != null && qty > 0) {
                        availableMeds.add(name);
                        medicineInventoryMap.put(name, ds);
                    }
                }
                if (getContext() != null) {
                    autoCompleteMedicine.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, availableMeds));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveConsultationRecord() {
        DatabaseReference consultRef = mDatabase.child("Consultations").push();
        HashMap<String, Object> data = new HashMap<>();
        data.put("patientUid", linkedPatientUid);
        data.put("patientName", autoCompletePatient.getText().toString());
        data.put("patientType", spinnerPatientType.getSelectedItem().toString());
        data.put("date", etDate.getText().toString());
        data.put("time", etTime.getText().toString());
        data.put("reason", etPurpose.getText().toString());
        data.put("healthWorker", etHealthWorker.getText().toString());
        data.put("notes", etNotes.getText().toString());
        data.put("status", "scheduled");

        consultRef.setValue(data).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Consultation Saved!", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigateUp();
            }
        });
    }

    private void saveMedicineRecord() {
        String medName = autoCompleteMedicine.getText().toString().trim();
        String qtyInput = etMedQty.getText().toString().trim();
        String date = etDate.getText().toString().trim(); // Get shared date
        DataSnapshot medSnapshot = medicineInventoryMap.get(medName);

        if (medSnapshot == null) {
            Toast.makeText(getContext(), "Please select a valid medicine", Toast.LENGTH_SHORT).show();
            return;
        }

        int currentStock = Integer.parseInt(medSnapshot.child("quantity").getValue(String.class));
        int dispenseQty;
        try {
            dispenseQty = Integer.parseInt(qtyInput);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dispenseQty > currentStock) {
            Toast.makeText(getContext(), "Insufficient stock! Only " + currentStock + " left.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Update Inventory Stock
        mDatabase.child("Inventory").child(medSnapshot.getKey()).child("quantity")
                .setValue(String.valueOf(currentStock - dispenseQty));

        // 2. Save Dispensing Record
        DatabaseReference medRef = mDatabase.child("Medicine_Records").push();
        HashMap<String, Object> data = new HashMap<>();
        data.put("patientUid", linkedPatientUid);
        data.put("patientName", autoCompletePatient.getText().toString());
        data.put("medicineName", medName);
        data.put("dosage", etMedDosage.getText().toString().trim());
        data.put("quantity", qtyInput);
        data.put("purpose", etMedPurpose.getText().toString().trim());
        data.put("dispensedBy", etDispensedBy.getText().toString().trim());
        data.put("date", date); // Added date field
        data.put("timestamp", ServerValue.TIMESTAMP);

        medRef.setValue(data).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Medicine Record Saved & Stock Updated!", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).navigateUp();
        });
    }

    private void saveImmunizationRecord() {
        String selectedVaccine = spinnerVaccineType.getSelectedItem().toString();

        // 1. Find the matching item in the Inventory Map
        DataSnapshot targetVaccineSnapshot = null;
        for (Map.Entry<String, DataSnapshot> entry : medicineInventoryMap.entrySet()) {
            // This checks if "BCG" is inside "Bacille Calmette Guein (BCG)"
            if (entry.getKey().toLowerCase().contains(selectedVaccine.toLowerCase()) ||
                    selectedVaccine.toLowerCase().contains(entry.getKey().toLowerCase())) {
                targetVaccineSnapshot = entry.getValue();
                break;
            }
        }

        // 2. If found, check stock and subtract
        if (targetVaccineSnapshot != null) {
            String qtyStr = targetVaccineSnapshot.child("quantity").getValue(String.class);
            int currentStock = 0;
            try { currentStock = Integer.parseInt(qtyStr); } catch (Exception e) { currentStock = 0; }

            if (currentStock <= 0) {
                Toast.makeText(getContext(), "Out of Stock: " + selectedVaccine, Toast.LENGTH_SHORT).show();
                return; // Stop saving if no stock
            }

            // Subtract 1 dose from Inventory
            String invKey = targetVaccineSnapshot.getKey();
            mDatabase.child("Inventory").child(invKey).child("quantity").setValue(String.valueOf(currentStock - 1));
        } else {
            // Optional: If you want to require the vaccine to exist in inventory to save
            // Toast.makeText(getContext(), "Vaccine not found in Inventory!", Toast.LENGTH_SHORT).show();
            // return;
        }

        // 3. Save the Record (Your original logic)
        DatabaseReference immRef = mDatabase.child("Immunizations").push();
        HashMap<String, Object> data = new HashMap<>();
        data.put("patientUid", linkedPatientUid);
        data.put("childName", autoCompleteChild.getText().toString());
        data.put("parentGuardian", autoCompleteMother.getText().toString());
        data.put("vaccineType", selectedVaccine);
        data.put("doseNo", spinnerDoseNo.getSelectedItem().toString());
        data.put("scheduledAge", spinnerScheduledAge.getSelectedItem().toString());
        data.put("dateAdministered", etDateAdministered.getText().toString());
        data.put("nextSchedule", etNextImmDate.getText().toString());
        data.put("administeredBy", etAdminBy.getText().toString());
        data.put("timestamp", ServerValue.TIMESTAMP);

        immRef.setValue(data).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Immunization Saved & Inventory Updated!", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigateUp();
            }
        });
    }

    private void savePrenatalRecord() {
        DatabaseReference prenatalRef = mDatabase.child("Prenatal_Records").push();
        HashMap<String, Object> data = new HashMap<>();
        data.put("patientUid", linkedPatientUid);
        data.put("patientName", autoCompletePatient.getText().toString());
        data.put("bloodPressure", etPrenatalBP.getText().toString());
        data.put("weight", etPrenatalWeight.getText().toString());
        data.put("fundalHeight", etFundalHeight.getText().toString());
        data.put("fetalHeartTone", etFetalHeartTone.getText().toString());
        data.put("tetanusToxoid", spinnerTetanusToxoid.getSelectedItem().toString());
        data.put("diagnosisTreatment", etDiagnosisTreatment.getText().toString());
        data.put("date", etDate.getText().toString());
        data.put("healthWorker", etHealthWorker.getText().toString());
        data.put("returnCheckupDate", etReturnCheckup.getText().toString());
        data.put("status", "scheduled");

        prenatalRef.setValue(data).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Prenatal Record Saved!", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigateUp();
            }
        });
    }

    private void switchTab(String type) {
        currentRecordType = type;
        resetAllCards();
        layoutConsultation.setVisibility(View.GONE);
        layoutImmunization.setVisibility(View.GONE);
        layoutPrenatal.setVisibility(View.GONE);
        layoutCommonConsultation.setVisibility(View.GONE);
        layoutMedicineFields.setVisibility(View.GONE);

        layoutConsultationSearch.setVisibility(View.VISIBLE);
        tvSelectPatientLabel.setVisibility(View.VISIBLE);
        spinnerPatientType.setVisibility(View.VISIBLE);

        switch (type) {
            case "Consultation":
                highlightCard(cardConsultation, "#1B75BC", "#E8F0FE");
                layoutConsultation.setVisibility(View.VISIBLE);
                layoutCommonConsultation.setVisibility(View.VISIBLE);
                loadPatientsFromFirebase(spinnerPatientType.getSelectedItem().toString());
                btnSave.setText("Save Consultation");
                break;
            case "Immunization":
                highlightCard(cardImmunization, "#2E7D32", "#F1F8E9");
                layoutImmunization.setVisibility(View.VISIBLE);
                layoutConsultationSearch.setVisibility(View.GONE);
                loadImmunizationData();
                setupMedicineAutocomplete(); // <--- CRITICAL: Keeps the inventory map fresh
                btnSave.setText("Save Immunization");
                break;
            case "Prenatal":
                highlightCard(cardPrenatal, "#E91E63", "#FCE4EC");
                layoutConsultation.setVisibility(View.VISIBLE);
                layoutPrenatal.setVisibility(View.VISIBLE);
                tvSelectPatientLabel.setVisibility(View.GONE);
                spinnerPatientType.setVisibility(View.GONE);
                loadPatientsFromFirebase("Parent/Guardian");
                btnSave.setText("Save Prenatal Record");
                break;
            case "Medicine":
                highlightCard(cardMedicine, "#FF8F00", "#FFF8E1");
                layoutMedicineFields.setVisibility(View.VISIBLE);
                loadPatientsFromFirebase(spinnerPatientType.getSelectedItem().toString());
                btnSave.setText("Save Medicine Record");
                break;
        }
    }

    private void validateAndSave() {
        String commonPatient = autoCompletePatient.getText().toString().trim();

        if (currentRecordType.equals("Consultation")) {
            String worker = etHealthWorker.getText().toString().trim(); // New check
            if (commonPatient.isEmpty() || linkedPatientUid.isEmpty()) {
                autoCompletePatient.setError("Select patient first");
                return;
            }
            if (worker.isEmpty()) {
                etHealthWorker.setError("Health worker required");
                return;
            }
            saveConsultationRecord();
        }
        else if (currentRecordType.equals("Prenatal")) {
            // FIX: Added this block to handle Prenatal saves
            String worker = etHealthWorker.getText().toString().trim();
            if (commonPatient.isEmpty() || linkedPatientUid.isEmpty()) {
                autoCompletePatient.setError("Select patient first");
                return;
            }
            if (worker.isEmpty()) {
                etHealthWorker.setError("Health worker required");
                return;
            }
            savePrenatalRecord();
        }
        else if (currentRecordType.equals("Medicine")) {
            String med = autoCompleteMedicine.getText().toString().trim();
            String qty = etMedQty.getText().toString().trim();
            String worker = etDispensedBy.getText().toString().trim(); // New check

            if (commonPatient.isEmpty() || linkedPatientUid.isEmpty()) {
                autoCompletePatient.setError("Select patient first");
                return;
            }
            if (med.isEmpty()) {
                autoCompleteMedicine.setError("Select medicine");
                return;
            }
            if (worker.isEmpty()) {
                etDispensedBy.setError("Dispenser required");
                return;
            }
            saveMedicineRecord();
        }
        else if (currentRecordType.equals("Immunization")) {
            String worker = etAdminBy.getText().toString().trim(); // New check
            if (autoCompleteChild.getText().toString().isEmpty()) {
                autoCompleteChild.setError("Select child first");
                return;
            }
            if (worker.isEmpty()) {
                etAdminBy.setError("Administered by required");
                return;
            }
            saveImmunizationRecord();
        }
    }

   private void loadImmunizationData() {
        mDatabase.child("Patients_Children").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return; // Prevent crashes if Maria leaves the screen
                childNames.clear();
                childGuardianMap.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String fName = ds.child("firstName").getValue(String.class);
                    String lName = ds.child("lastName").getValue(String.class);
                    String pUid = ds.child("parentUid").getValue(String.class);

                    if (fName != null && lName != null && pUid != null) {
                        String fullName = fName.trim() + " " + lName.trim();
                        childNames.add(fullName);

                        // Fetch guardian name once per child
                        mDatabase.child("Patients_Guardians").orderByChild("linkedUid").equalTo(pUid)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot parentSnapshot) {
                                        for (DataSnapshot parentDs : parentSnapshot.getChildren()) {
                                            String motherName = parentDs.child("fullName").getValue(String.class);
                                            if (motherName != null) childGuardianMap.put(fullName, motherName);
                                        }
                                    }
                                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                                });
                    }
                }
                if (getContext() != null) {
                    childAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, childNames);
                    autoCompleteChild.setAdapter(childAdapter);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupAdapters() {
        String[] types = {"Child", "Parent/Guardian"};
        spinnerPatientType.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, types));
        spinnerPatientType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                loadPatientsFromFirebase(types[pos]);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupImmunizationSpinners() {
        String[] vaccines = {"BCG Vaccine", "Hepatitis B Vaccine", "Pentavalent Vaccine (DPT-Hep B-HIB)", "OPV", "IPV", "PCV", "MMR"};
        spinnerVaccineType.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, vaccines));
        spinnerDoseNo.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, new String[]{"1", "2", "3"}));
        spinnerScheduledAge.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, new String[]{"At birth", "1 ½ months", "2 ½ months", "3 ½ months", "9 months", "1 year"}));
    }

    private void highlightCard(com.google.android.material.card.MaterialCardView card, String color, String bgColor) {
        if (card != null) {
            card.setStrokeWidth(6);
            card.setStrokeColor(android.graphics.Color.parseColor(color));
            card.setCardBackgroundColor(android.graphics.Color.parseColor(bgColor));
            LinearLayout layout = (LinearLayout) card.getChildAt(0);
            android.widget.ImageView icon = (android.widget.ImageView) layout.getChildAt(0);
            android.widget.TextView text = (android.widget.TextView) layout.getChildAt(1);
            icon.setColorFilter(android.graphics.Color.parseColor(color));
            text.setTextColor(android.graphics.Color.parseColor(color));
            text.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }

    private void resetAllCards() {
        com.google.android.material.card.MaterialCardView[] cards = {cardConsultation, cardImmunization, cardMedicine, cardPrenatal};
        for (com.google.android.material.card.MaterialCardView c : cards) {
            if (c != null) {
                c.setStrokeWidth(2);
                c.setStrokeColor(android.graphics.Color.parseColor("#EEEEEE"));
                c.setCardBackgroundColor(android.graphics.Color.WHITE);
                LinearLayout layout = (LinearLayout) c.getChildAt(0);
                android.widget.ImageView icon = (android.widget.ImageView) layout.getChildAt(0);
                android.widget.TextView text = (android.widget.TextView) layout.getChildAt(1);
                icon.setColorFilter(android.graphics.Color.parseColor("#95A5A6"));
                text.setTextColor(android.graphics.Color.parseColor("#95A5A6"));
            }
        }
    }

    private void showDatePicker(EditText targetField) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (v, y, m, d) ->
                targetField.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y)),
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        Calendar cal = Calendar.getInstance();
        new TimePickerDialog(requireContext(), (v, h, m) -> {
            String ampm = (h >= 12) ? "PM" : "AM";
            int h12 = (h == 0 || h == 12) ? 12 : h % 12;
            etTime.setText(String.format(Locale.getDefault(), "%02d:%02d %s", h12, m, ampm));
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
    }
}

// Fixed UserAccount class placement
class UserAccount {
    String name, username, uid;
    UserAccount(String n, String u, String id) {
        this.name = n;
        this.username = u;
        this.uid = id;
    }
}