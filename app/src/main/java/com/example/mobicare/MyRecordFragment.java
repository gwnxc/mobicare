package com.example.mobicare;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MyRecordFragment extends Fragment {

    private String targetPatientUid;
    private String passedName;
    private String userRole;

    // UI References matching your XML file character-for-character
    private LinearLayout llPrenatalRecords, llMedicineHistory;
    private LinearLayout sectionPrenatal, sectionMedicines;
    private TextView tabPrenatal, tabMedicines;
    private TextView tvMotherName, tvEmptyState;
    private MaterialCardView cvViewOnlyWarning, cvScheduleNotice;

    // Firebase References
    private DatabaseReference prenatalRef, medsRef;
    private ValueEventListener prenatalListener, medListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize SharedPreferences and Handle Navigation Session Arguments
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("MobicarePrefs", android.content.Context.MODE_PRIVATE);
        userRole = prefs.getString("userRole", "").trim();
        String loggedInUid = prefs.getString("loggedUserKey", "");

        // Initialize core layout mappings
        tvMotherName = view.findViewById(R.id.tvMotherName);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        tabPrenatal = view.findViewById(R.id.tabPrenatal);
        tabMedicines = view.findViewById(R.id.tabMedicines);

        sectionPrenatal = view.findViewById(R.id.sectionPrenatal);
        sectionMedicines = view.findViewById(R.id.sectionMedicines);
        llPrenatalRecords = view.findViewById(R.id.llPrenatalRecords);
        llMedicineHistory = view.findViewById(R.id.llMedicineHistory);
        cvViewOnlyWarning = view.findViewById(R.id.cvViewOnlyWarning);
        cvScheduleNotice = view.findViewById(R.id.cvScheduleNotice);

        // Extract passed Bundle arguments forwarded from management tables
        if (getArguments() != null) {
            targetPatientUid = getArguments().getString("selectedChildId");
            passedName = getArguments().getString("selectedChildName");
        }

        // 2. Structural Role-Based UI Toggling
        if ("Patient".equalsIgnoreCase(userRole)) {
            // Patient/Parent session enforces privacy constraint route
            targetPatientUid = loggedInUid;
            if (cvViewOnlyWarning != null) cvViewOnlyWarning.setVisibility(View.VISIBLE);
            if (cvScheduleNotice != null) cvScheduleNotice.setVisibility(View.VISIBLE);
        } else if ("Admin".equalsIgnoreCase(userRole) || "Health Worker".equalsIgnoreCase(userRole)) {
            // Management role clears parent alert elements
            if (cvViewOnlyWarning != null) cvViewOnlyWarning.setVisibility(View.GONE);
            if (cvScheduleNotice != null) cvScheduleNotice.setVisibility(View.GONE);
        }

        // Set local placeholder text early while background fetches complete
        if (tvMotherName != null) {
            tvMotherName.setText(passedName != null ? passedName : "Loading Mother's Record...");
        }

        // Handle navigation back actions
        ImageView btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        // 3. Tab State Layout Content Toggle Switches
        if (tabPrenatal != null && tabMedicines != null) {
            tabPrenatal.setOnClickListener(v -> {
                tabPrenatal.setBackgroundResource(R.drawable.bg_active_tab);
                tabPrenatal.setTextColor(Color.parseColor("#388E3C"));
                tabMedicines.setBackground(null);
                tabMedicines.setTextColor(Color.parseColor("#64748B"));

                sectionPrenatal.setVisibility(View.VISIBLE);
                sectionMedicines.setVisibility(View.GONE);
            });

            tabMedicines.setOnClickListener(v -> {
                tabMedicines.setBackgroundResource(R.drawable.bg_active_tab);
                tabMedicines.setTextColor(Color.parseColor("#388E3C"));
                tabPrenatal.setBackground(null);
                tabPrenatal.setTextColor(Color.parseColor("#64748B"));

                sectionPrenatal.setVisibility(View.GONE);
                sectionMedicines.setVisibility(View.VISIBLE);

                fetchMedicineRecords();
            });
        }

        // Run network query lifecycle
        if (targetPatientUid != null && !targetPatientUid.isEmpty()) {
            fetchPrenatalRecords();
        }
    }

    private void fetchPrenatalRecords() {
        prenatalRef = FirebaseDatabase.getInstance().getReference("Prenatal_Records");

        prenatalListener = prenatalRef.orderByChild("patientUid").equalTo(targetPatientUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || llPrenatalRecords == null) return;
                llPrenatalRecords.removeAllViews();

                // Multi-Route Fallback: if no records match the ID query string, look up by name matching
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    if (!"Patient".equalsIgnoreCase(userRole) && passedName != null && !passedName.isEmpty()) {
                        prenatalRef.orderByChild("patientName").equalTo(passedName).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot nameSnapshot) {
                                if (!isAdded() || llPrenatalRecords == null) return;
                                processPrenatalSnapshots(nameSnapshot);
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    } else {
                        if (tvEmptyState != null) tvEmptyState.setVisibility(View.VISIBLE);
                    }
                    return;
                }

                processPrenatalSnapshots(snapshot);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void processPrenatalSnapshots(DataSnapshot snapshot) {
        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);

        for (DataSnapshot postSnap : snapshot.getChildren()) {
            String date = postSnap.child("date").getValue(String.class);
            String bp = postSnap.child("bloodPressure").getValue(String.class);
            String weight = postSnap.child("weight").getValue(String.class);
            String fh = postSnap.child("fundalHeight").getValue(String.class);
            String fht = postSnap.child("fetalHeartTone").getValue(String.class);
            String tt = postSnap.child("tetanusToxoid").getValue(String.class);
            String diagnosis = postSnap.child("diagnosisTreatment").getValue(String.class);
            String nextReturn = postSnap.child("returnCheckupDate").getValue(String.class);

            // Re-verify name field parameters reactively
            if (passedName == null && postSnap.hasChild("patientName")) {
                passedName = postSnap.child("patientName").getValue(String.class);
                if (tvMotherName != null) tvMotherName.setText(passedName);
            }

            addPrenatalCardToUI(date, bp, weight, fh, fht, tt, diagnosis, nextReturn);
        }
    }

    private void fetchMedicineRecords() {
        if (llMedicineHistory == null) return;
        medsRef = FirebaseDatabase.getInstance().getReference("Medicine_Records");

        medListener = medsRef.orderByChild("patientUid").equalTo(targetPatientUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || llMedicineHistory == null) return;
                llMedicineHistory.removeAllViews();

                // Multi-Route Fallback for Management roles running mismatch profiles
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    if (!"Patient".equalsIgnoreCase(userRole) && passedName != null && !passedName.isEmpty()) {
                        medsRef.orderByChild("patientName").equalTo(passedName).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot nameSnapshot) {
                                if (!isAdded() || llMedicineHistory == null) return;
                                for (DataSnapshot medSnap : nameSnapshot.getChildren()) {
                                    displayMedicationSnapshot(medSnap);
                                }
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }
                    return;
                }

                for (DataSnapshot medSnap : snapshot.getChildren()) {
                    displayMedicationSnapshot(medSnap);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void displayMedicationSnapshot(DataSnapshot medSnap) {
        String name = medSnap.child("medicineName").getValue(String.class);
        String dosage = medSnap.child("dosage").getValue(String.class);
        String purpose = medSnap.child("purpose").getValue(String.class);
        String date = medSnap.child("date").getValue(String.class);

        View card = getLayoutInflater().inflate(R.layout.item_medicine_history, null);

        // Dynamic padding rules applied programmatically to keep lists unstacked
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, (int) (14 * getResources().getDisplayMetrics().density + 0.5f));
        card.setLayoutParams(layoutParams);

        TextView tvName = card.findViewById(R.id.tvMedName);
        TextView tvDosage = card.findViewById(R.id.tvMedDosage);
        TextView tvPurpose = card.findViewById(R.id.tvMedPurpose);
        TextView tvDate = card.findViewById(R.id.tvMedDate);

        if (tvName != null) tvName.setText(name);
        if (tvDosage != null) tvDosage.setText("Dosage: " + dosage);
        if (tvPurpose != null) tvPurpose.setText("Purpose: " + purpose);
        if (tvDate != null) tvDate.setText("Prescribed: " + (date == null || date.isEmpty() ? "No Date Provided" : date));

        llMedicineHistory.addView(card);
    }

    private void addPrenatalCardToUI(String date, String bp, String weight, String fh, String fht, String tt, String diagnosis, String nextReturn) {
        // Linked directly to your actual item_prenatal_record XML layout resource structure!
        View card = getLayoutInflater().inflate(R.layout.item_prenatal_record, null);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, (int) (16 * getResources().getDisplayMetrics().density + 0.5f));
        card.setLayoutParams(layoutParams);

        TextView tvDate = card.findViewById(R.id.tvPrenatalDate);
        TextView tvBP = card.findViewById(R.id.tvBloodPressure);
        TextView tvW = card.findViewById(R.id.tvWeight);
        TextView tvFH = card.findViewById(R.id.tvFundalHeight);
        TextView tvFHT = card.findViewById(R.id.tvFetalHeartTone);
        TextView tvTT = card.findViewById(R.id.tvTetanusToxoid);
        TextView tvDiag = card.findViewById(R.id.tvDiagnosisTreatment);
        TextView tvReturn = card.findViewById(R.id.tvReturnCheckup);

        if (tvDate != null) tvDate.setText(date != null ? date : "N/A");
        if (tvBP != null) tvBP.setText(bp != null ? bp : "N/A");
        if (tvW != null) tvW.setText(weight != null ? weight + " kg" : "N/A");
        if (tvFH != null) tvFH.setText(fh != null ? fh + " cm" : "N/A");
        if (tvFHT != null) tvFHT.setText(fht != null ? fht : "N/A");
        if (tvTT != null) tvTT.setText("TT Given: " + (tt != null ? "TT" + tt : "None"));
        if (tvDiag != null) tvDiag.setText("Diagnosis/Treatment: " + (diagnosis != null ? diagnosis : "none"));
        if (tvReturn != null) tvReturn.setText("Return check-up: " + (nextReturn != null ? nextReturn : "N/A"));

        llPrenatalRecords.addView(card);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Prevent background lookups and performance drops on screen exit
        if (prenatalRef != null && prenatalListener != null) prenatalRef.removeEventListener(prenatalListener);
        if (medsRef != null && medListener != null) medsRef.removeEventListener(medListener);
    }
}