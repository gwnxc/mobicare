package com.example.mobicare;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class myRecordFragment extends Fragment {

    private String loggedInUserId;

    // UI Elements
    private TextView tvMotherName, tvSectionTitle;
    private TextView tabPrenatal, tabMedicines;

    // Prenatal Elements
    private LinearLayout llPrenatalRecords;
    private TextView tvEmptyState;

    // Medicine Elements
    private LinearLayout llMedicineRecords;
    private TextView tvEmptyStateMedicines;

    // Bottom Info Card
    private TextView tvInfoTitleBottom, tvInfoDescBottom;

    private boolean isMedicineLoaded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- ROBUST USER ID FETCH ---
        SharedPreferences prefs = requireActivity().getSharedPreferences("MobiCarePrefs", Context.MODE_PRIVATE);
        loggedInUserId = prefs.getString("loggedUserKey", null);

        if (loggedInUserId == null || loggedInUserId.isEmpty()) {
            SharedPreferences altPrefs = requireActivity().getSharedPreferences("MobicarePrefs", Context.MODE_PRIVATE);
            loggedInUserId = altPrefs.getString("loggedInUser", null);
        }

        if (loggedInUserId == null || loggedInUserId.isEmpty()) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                loggedInUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            }
        }

        // --- INIT VIEWS ---
        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        tvMotherName = view.findViewById(R.id.tvMotherName);
        tvSectionTitle = view.findViewById(R.id.tvSectionTitle);
        tabPrenatal = view.findViewById(R.id.tabPrenatal);
        tabMedicines = view.findViewById(R.id.tabMedicines);

        llPrenatalRecords = view.findViewById(R.id.llPrenatalRecords);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        llMedicineRecords = view.findViewById(R.id.llMedicineRecords);
        tvEmptyStateMedicines = view.findViewById(R.id.tvEmptyStateMedicines);

        tvInfoTitleBottom = view.findViewById(R.id.tvInfoTitleBottom);
        tvInfoDescBottom = view.findViewById(R.id.tvInfoDescBottom);

        // --- TAB LISTENERS ---
        // Force the views to be clickable just in case the XML property is missing
        if (tabPrenatal != null) {
            tabPrenatal.setClickable(true);
            tabPrenatal.setOnClickListener(v -> showPrenatalTab());
        }

        if (tabMedicines != null) {
            tabMedicines.setClickable(true);
            tabMedicines.setOnClickListener(v -> showMedicinesTab());
        }

        // --- FETCH DATA ---
        if (loggedInUserId != null && !loggedInUserId.isEmpty()) {
            fetchMotherName();
            fetchPrenatalRecords();
        } else {
            Toast.makeText(getContext(), "Error: User not found", Toast.LENGTH_SHORT).show();
            if (tvMotherName != null) tvMotherName.setText("User Not Found");
        }
    }

    private void showPrenatalTab() {
        if (tabPrenatal == null || tabMedicines == null) return;

        // UI Styling
        tabPrenatal.setBackgroundResource(R.drawable.circle_white);
        tabPrenatal.setTextColor(Color.parseColor("#00796B"));
        tabPrenatal.setTypeface(null, Typeface.BOLD);

        tabMedicines.setBackgroundResource(0); // Remove background
        tabMedicines.setTextColor(Color.parseColor("#64748B"));
        tabMedicines.setTypeface(null, Typeface.NORMAL);

        if (tvSectionTitle != null) tvSectionTitle.setText("Prenatal Care History");
        if (tvInfoTitleBottom != null) tvInfoTitleBottom.setText("Prenatal Care Importance");
        if (tvInfoDescBottom != null) tvInfoDescBottom.setText("Regular prenatal checkups help monitor your baby's development. Attend all scheduled visits.");

        // Show/Hide Containers
        if (llMedicineRecords != null) llMedicineRecords.setVisibility(View.GONE);
        if (tvEmptyStateMedicines != null) tvEmptyStateMedicines.setVisibility(View.GONE);

        if (llPrenatalRecords != null && tvEmptyState != null) {
            if (llPrenatalRecords.getChildCount() > 0) {
                llPrenatalRecords.setVisibility(View.VISIBLE);
                tvEmptyState.setVisibility(View.GONE);
            } else {
                llPrenatalRecords.setVisibility(View.GONE);
                tvEmptyState.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showMedicinesTab() {
        if (tabPrenatal == null || tabMedicines == null) return;

        // UI Styling
        tabMedicines.setBackgroundResource(R.drawable.circle_white);
        tabMedicines.setTextColor(Color.parseColor("#00796B"));
        tabMedicines.setTypeface(null, Typeface.BOLD);

        tabPrenatal.setBackgroundResource(0); // Remove background
        tabPrenatal.setTextColor(Color.parseColor("#64748B"));
        tabPrenatal.setTypeface(null, Typeface.NORMAL);

        if (tvSectionTitle != null) tvSectionTitle.setText("Medication History");
        if (tvInfoTitleBottom != null) tvInfoTitleBottom.setText("Medication Safety");
        if (tvInfoDescBottom != null) tvInfoDescBottom.setText("Always take your prescribed medicines exactly as instructed by your health worker.");

        // Show/Hide Containers
        if (llPrenatalRecords != null) llPrenatalRecords.setVisibility(View.GONE);
        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);

        if (!isMedicineLoaded) {
            fetchMedicineRecords();
        } else {
            if (llMedicineRecords != null && tvEmptyStateMedicines != null) {
                if (llMedicineRecords.getChildCount() > 0) {
                    llMedicineRecords.setVisibility(View.VISIBLE);
                    tvEmptyStateMedicines.setVisibility(View.GONE);
                } else {
                    llMedicineRecords.setVisibility(View.GONE);
                    tvEmptyStateMedicines.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void fetchMotherName() {
        DatabaseReference motherRef = FirebaseDatabase.getInstance().getReference("Patients_Guardians").child(loggedInUserId);
        motherRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAdded()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    if (name != null && tvMotherName != null) tvMotherName.setText(name);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchPrenatalRecords() {
        DatabaseReference recordsRef = FirebaseDatabase.getInstance().getReference("Prenatal_Records");

        recordsRef.orderByChild("patientUid").equalTo(loggedInUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || llPrenatalRecords == null || tvEmptyState == null) return;

                llPrenatalRecords.removeAllViews();

                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    return;
                }

                tvEmptyState.setVisibility(View.GONE);
                llPrenatalRecords.setVisibility(View.VISIBLE);

                for (DataSnapshot recordSnap : snapshot.getChildren()) {
                    String date = recordSnap.child("date").getValue(String.class);
                    String bp = recordSnap.child("bloodPressure").getValue(String.class);
                    String weight = recordSnap.child("weight").getValue(String.class);
                    String fh = recordSnap.child("fundalHeight").getValue(String.class);
                    String fht = recordSnap.child("fetalHeartTone").getValue(String.class);
                    String tt = recordSnap.child("tetanusToxoid").getValue(String.class);
                    String diagnosis = recordSnap.child("diagnosisTreatment").getValue(String.class);
                    String returnDate = recordSnap.child("returnCheckupDate").getValue(String.class);

                    addPrenatalRecordToUI(date, bp, weight, fh, fht, tt, diagnosis, returnDate);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchMedicineRecords() {
        DatabaseReference medsRef = FirebaseDatabase.getInstance().getReference("Medicine_Records");

        medsRef.orderByChild("patientUid").equalTo(loggedInUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || llMedicineRecords == null || tvEmptyStateMedicines == null) return;

                llMedicineRecords.removeAllViews();
                isMedicineLoaded = true;

                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    tvEmptyStateMedicines.setVisibility(View.VISIBLE);
                    llMedicineRecords.setVisibility(View.GONE);
                    return;
                }

                tvEmptyStateMedicines.setVisibility(View.GONE);
                llMedicineRecords.setVisibility(View.VISIBLE);

                for (DataSnapshot recordSnap : snapshot.getChildren()) {
                    String date = recordSnap.child("date").getValue(String.class);
                    String name = recordSnap.child("medicineName").getValue(String.class);
                    String dosage = recordSnap.child("dosage").getValue(String.class);
                    String instructions = recordSnap.child("instructions").getValue(String.class);

                    addMedicineRecordToUI(name, date, dosage, instructions);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addPrenatalRecordToUI(String date, String bp, String weight, String fh, String fht, String tt, String diagnosis, String returnDate) {
        if (getContext() == null || llPrenatalRecords == null) return;
        View card = getLayoutInflater().inflate(R.layout.item_prenatal_record, null);

        ((TextView) card.findViewById(R.id.tvPreDate)).setText(date != null ? date : "--");
        ((TextView) card.findViewById(R.id.tvPreBP)).setText(bp != null ? bp : "--");
        ((TextView) card.findViewById(R.id.tvPreWeight)).setText(weight != null ? weight + " kg" : "-- kg");

        String fhText = fh != null ? (fh.contains("cm") ? fh : fh + " cm") : "-- cm";
        ((TextView) card.findViewById(R.id.tvPreFH)).setText(fhText);

        String fhtText = fht != null ? (fht.contains("bpm") ? fht : fht + " bpm") : "-- bpm";
        ((TextView) card.findViewById(R.id.tvPreFHT)).setText(fhtText);

        ((TextView) card.findViewById(R.id.tvPreTT)).setText(tt != null ? "TT Given: " + tt : "TT Given: N/A");
        ((TextView) card.findViewById(R.id.tvPreDiagnosis)).setText(diagnosis != null ? "Diagnosis/Treatment: " + diagnosis : "Diagnosis/Treatment: N/A");
        ((TextView) card.findViewById(R.id.tvPreReturn)).setText(returnDate != null ? "Return check-up: " + returnDate : "Return check-up: N/A");

        llPrenatalRecords.addView(card);
    }

    private void addMedicineRecordToUI(String name, String date, String dosage, String instructions) {
        if (getContext() == null || llMedicineRecords == null) return;
        View card = getLayoutInflater().inflate(R.layout.item_medicine_record, null);

        ((TextView) card.findViewById(R.id.tvMedName)).setText(name != null ? name : "Unknown Medicine");
        ((TextView) card.findViewById(R.id.tvMedDate)).setText(date != null ? "Date Prescribed: " + date : "Date Prescribed: --");
        ((TextView) card.findViewById(R.id.tvMedDosage)).setText(dosage != null ? "Dosage: " + dosage : "Dosage: --");
        ((TextView) card.findViewById(R.id.tvMedInstructions)).setText(instructions != null ? "Instructions: " + instructions : "Instructions: --");

        llMedicineRecords.addView(card);
    }
}