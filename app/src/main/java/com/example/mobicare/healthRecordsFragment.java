package com.example.mobicare;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class healthRecordsFragment extends Fragment {

    private String childId;
    private String childName;

    // UI References
    private LinearLayout llFullVaccineHistory, llMedicineHistory;
    private LinearLayout sectionVaccines, sectionMedicines;
    private TextView tabVaccines, tabMedicines;
    private TextView tvProgressText, tvPercent, tvChildSubtitle;
    private ProgressBar pbVaccine;
    private MaterialButton btnEditRecord;
    private MaterialCardView cvViewOnlyWarning, cvScheduleNotice;

    // Track listeners so we can clean them up nicely
    private ValueEventListener vaxListener, medListener;
    private DatabaseReference immunizationsRef, medsRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_health_records, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize SharedPreferences
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("MobicarePrefs", android.content.Context.MODE_PRIVATE);
        String userRole = prefs.getString("userRole", "").trim();
        String loggedInUid = prefs.getString("loggedUserKey", "");

        // Initialize Shared View References
        tvChildSubtitle = view.findViewById(R.id.tvChildSubtitle);
        cvViewOnlyWarning = view.findViewById(R.id.cvViewOnlyWarning);
        cvScheduleNotice = view.findViewById(R.id.cvScheduleNotice);
        btnEditRecord = view.findViewById(R.id.btnEditRecord);

        tabVaccines = view.findViewById(R.id.tabVaccines);
        tabMedicines = view.findViewById(R.id.tabMedicines);
        sectionVaccines = view.findViewById(R.id.sectionVaccines);
        sectionMedicines = view.findViewById(R.id.sectionMedicines);

        llFullVaccineHistory = view.findViewById(R.id.llFullVaccineHistory);
        llMedicineHistory = view.findViewById(R.id.llMedicineHistory);
        tvProgressText = view.findViewById(R.id.tvProgressText);
        tvPercent = view.findViewById(R.id.tvPercent);
        pbVaccine = view.findViewById(R.id.pbVaccine);

        // 2. Extract Navigation Arguments first (Managers pass both ID and Name)
        if (getArguments() != null) {
            childId = getArguments().getString("selectedChildId");
            childName = getArguments().getString("selectedChildName");
        }

        // 3. Apply Role-Based Layout Configurations
        if ("Patient".equalsIgnoreCase(userRole)) {
            // Patient/Parent view overrides destination targets to match their session
            childId = loggedInUid;

            if (cvViewOnlyWarning != null) cvViewOnlyWarning.setVisibility(View.VISIBLE);
            if (cvScheduleNotice != null) cvScheduleNotice.setVisibility(View.VISIBLE);
            if (btnEditRecord != null) btnEditRecord.setVisibility(View.GONE);
        } else if ("Admin".equalsIgnoreCase(userRole) || "Health Worker".equalsIgnoreCase(userRole)) {
            // Management view hides patient warnings and reveals control tool options
            if (cvViewOnlyWarning != null) cvViewOnlyWarning.setVisibility(View.GONE);
            if (cvScheduleNotice != null) cvScheduleNotice.setVisibility(View.GONE);
            if (btnEditRecord != null) {
                btnEditRecord.setVisibility(View.VISIBLE);
                btnEditRecord.setOnClickListener(v -> Toast.makeText(getContext(), "Opening Edit Form for " + (childName != null ? childName : "Patient"), Toast.LENGTH_SHORT).show());
            }
        }

        // 4. Update Header Subtitle text dynamically
        if (tvChildSubtitle != null) {
            if (childName != null && !childName.isEmpty()) {
                tvChildSubtitle.setText(childName);
            } else if ("Patient".equalsIgnoreCase(userRole)) {
                tvChildSubtitle.setText("My Child's Record");
            } else {
                tvChildSubtitle.setText("Loading Patient...");
            }
        }

        // Back action button configuration
        ImageView btnBack = view.findViewById(R.id.btnBackRecords);
        if (btnBack != null) btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        // 5. Tab Layout Content Toggling Logic
        // Inside onViewCreated in healthRecordsFragment.java
        if (tabVaccines != null && tabMedicines != null) {
            tabVaccines.setOnClickListener(v -> {
                tabVaccines.setBackgroundResource(R.drawable.bg_active_tab); // UPDATED HERE
                tabVaccines.setTextColor(Color.parseColor("#388E3C"));
                tabMedicines.setBackground(null);
                tabMedicines.setTextColor(Color.parseColor("#64748B"));

                sectionVaccines.setVisibility(View.VISIBLE);
                sectionMedicines.setVisibility(View.GONE);
            });

            tabMedicines.setOnClickListener(v -> {
                tabMedicines.setBackgroundResource(R.drawable.bg_active_tab); // UPDATED HERE
                tabMedicines.setTextColor(Color.parseColor("#388E3C"));
                tabVaccines.setBackground(null);
                tabVaccines.setTextColor(Color.parseColor("#64748B"));

                sectionVaccines.setVisibility(View.GONE);
                sectionMedicines.setVisibility(View.VISIBLE);

                fetchMedicineHistory();
            });
        }

        // Run primary query initialization loops
        if (childId != null) {
            fetchFullImmunizationHistory();
        }
    }

    private void fetchFullImmunizationHistory() {
        immunizationsRef = FirebaseDatabase.getInstance().getReference("Immunizations");

        // FIX: Changed to addValueEventListener for real-time history list adjustments
        vaxListener = immunizationsRef.orderByChild("childId").equalTo(childId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || llFullVaccineHistory == null) return;
                llFullVaccineHistory.removeAllViews();

                int completedCount = 0;
                int targetVaccines = 5; // Formatted to match your 5-vax prototype setup scale

                for (DataSnapshot vaxSnap : snapshot.getChildren()) {
                    completedCount++;
                    String vaxType = vaxSnap.child("vaccineType").getValue(String.class);
                    String dose = vaxSnap.child("doseNo").getValue(String.class);
                    String dateAdministered = vaxSnap.child("dateAdministered").getValue(String.class);
                    String nextSchedule = vaxSnap.child("nextSchedule").getValue(String.class);

                    // Dynamic name resolving fallback
                    if (childName == null && vaxSnap.hasChild("childName")) {
                        childName = vaxSnap.child("childName").getValue(String.class);
                        if (tvChildSubtitle != null) tvChildSubtitle.setText(childName);
                    }

                    String title = vaxType;
                    if (dose != null && !dose.isEmpty()) {
                        title += " (Dose " + dose + ")";
                    }
                    addHistoryCardToUI(title, dateAdministered, nextSchedule);
                }

                // UI Progress Meter calculations updates
                if (tvProgressText != null) tvProgressText.setText(completedCount + " of " + targetVaccines + " completed");
                if (pbVaccine != null) {
                    pbVaccine.setMax(targetVaccines);
                    pbVaccine.setProgress(completedCount);
                }
                if (tvPercent != null) {
                    int percent = targetVaccines > 0 ? (int) (((float) completedCount / targetVaccines) * 100) : 0;
                    tvPercent.setText(percent + "%");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchMedicineHistory() {
        if (llMedicineHistory == null) return;

        if (childId == null || childId.isEmpty()) {
            return;
        }

        medsRef = FirebaseDatabase.getInstance().getReference("Medicine_Records");

        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("MobiCarePrefs", android.content.Context.MODE_PRIVATE);
        String currentRole = prefs.getString("userRole", "").trim();

        String queryId = childId;
        if ("Patient".equalsIgnoreCase(currentRole)) {
            queryId = prefs.getString("loggedUserKey", "");
        }

        final String finalQueryId = queryId;

        // 1. Check for records matching the patientUid/childId first
        medListener = medsRef.orderByChild("patientUid").equalTo(finalQueryId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || llMedicineHistory == null) return;

                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    llMedicineHistory.removeAllViews();
                    for (DataSnapshot medSnap : snapshot.getChildren()) {
                        displayMedicationSnapshot(medSnap);
                    }
                } else if (!"Patient".equalsIgnoreCase(currentRole) && childName != null && !childName.isEmpty()) {
                    // 2. MANAGEMENT FALLBACK: If nothing matches by ID and the user is an Admin/HW,
                    // search by patientName string matching ("Bea Garcia") to reconcile string discrepancies.
                    medsRef.orderByChild("patientName").equalTo(childName).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot nameSnapshot) {
                            if (!isAdded() || llMedicineHistory == null) return;
                            llMedicineHistory.removeAllViews();

                            if (nameSnapshot.exists() && nameSnapshot.getChildrenCount() > 0) {
                                for (DataSnapshot medSnap : nameSnapshot.getChildren()) {
                                    displayMedicationSnapshot(medSnap);
                                }
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                } else {
                    llMedicineHistory.removeAllViews();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Helper method to keep data parsing dry, clean, and reusable
    private void displayMedicationSnapshot(DataSnapshot medSnap) {
        String name = medSnap.child("medicineName").getValue(String.class);
        String dosage = medSnap.child("dosage").getValue(String.class);
        String purpose = medSnap.child("purpose").getValue(String.class);
        String date = medSnap.child("date").getValue(String.class);

        if (date == null || date.isEmpty()) {
            date = "No Date Provided";
        }

        addMedicineCardToUI(name, dosage, purpose, date);
    }

    private void addMedicineCardToUI(String name, String dosage, String purpose, String date) {
        if (llMedicineHistory == null || getContext() == null) return;

        // Inflate the base medicine card item layout
        View card = getLayoutInflater().inflate(R.layout.item_medicine_history, null);

        // --- ADDED: Programmatic Spacing & Layout Parameters ---
        // Creates layout parameters explicitly designed for a vertical LinearLayout container
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        // Convert 14dp of breathing room spacing seamlessly into pixels
        int marginInDp = 14;
        float scale = getResources().getDisplayMetrics().density;
        int marginInPx = (int) (marginInDp * scale + 0.5f);

        // Apply margins: Left (0), Top (0), Right (0), Bottom (marginInPx)
        layoutParams.setMargins(0, 0, 0, marginInPx);
        card.setLayoutParams(layoutParams); // Bind the spacing parameters directly to the card view
        // ------------------------------------------------------

        // Bind your TextView elements as normal
        TextView tvName = card.findViewById(R.id.tvMedName);
        TextView tvDosage = card.findViewById(R.id.tvMedDosage);
        TextView tvPurpose = card.findViewById(R.id.tvMedPurpose);
        TextView tvDate = card.findViewById(R.id.tvMedDate);

        if (tvName != null) tvName.setText(name != null ? name : "Unknown Medication");
        if (tvDosage != null) tvDosage.setText(dosage != null ? "Dosage: " + dosage : "Dosage: N/A");
        if (tvPurpose != null) tvPurpose.setText(purpose != null ? "Purpose: " + purpose : "Purpose: N/A");
        if (tvDate != null) tvDate.setText("Prescribed: " + date);

        // Append the newly spaced card container right into your scroll container view
        llMedicineHistory.addView(card);
    }

    private void addHistoryCardToUI(String title, String givenDate, String nextDate) {
        if (llFullVaccineHistory == null || getContext() == null) return;

        View card = getLayoutInflater().inflate(R.layout.item_vaccine_history, null);
        TextView tvName = card.findViewById(R.id.tvHistVaccineName);
        TextView tvGiven = card.findViewById(R.id.tvHistGivenDate);
        LinearLayout llNextDose = card.findViewById(R.id.llNextDose);

        if (tvName != null) tvName.setText(title != null ? title : "Unknown Vaccine");
        if (tvGiven != null) tvGiven.setText(givenDate != null ? "Given: " + givenDate : "Given: N/A");

        if (llNextDose != null) llNextDose.setVisibility(View.GONE);
        llFullVaccineHistory.addView(card);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // CLEANUP: Kill the active listeners to block memory leaks when shifting screens
        if (immunizationsRef != null && vaxListener != null) {
            immunizationsRef.removeEventListener(vaxListener);
        }
        if (medsRef != null && medListener != null) {
            medsRef.removeEventListener(medListener);
        }
    }
}