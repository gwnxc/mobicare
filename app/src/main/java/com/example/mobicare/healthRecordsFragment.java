package com.example.mobicare;

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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class healthRecordsFragment extends Fragment {

    private String childId;
    private String childName;
    private LinearLayout llFullVaccineHistory;

    private TextView tvProgressText, tvPercent;
    private ProgressBar pbVaccine;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_health_records, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Retrieve data passed from the Profile screen
        if (getArguments() != null) {
            childId = getArguments().getString("selectedChildId");
            childName = getArguments().getString("selectedChildName");
        }

        // Set Header Data
        TextView tvChildSubtitle = view.findViewById(R.id.tvChildSubtitle);
        if (tvChildSubtitle != null && childName != null) {
            tvChildSubtitle.setText(childName);
        }

        ImageView btnBack = view.findViewById(R.id.btnBackRecords);
        if (btnBack != null) btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        // Initialize UI Elements
        llFullVaccineHistory = view.findViewById(R.id.llFullVaccineHistory);
        tvProgressText = view.findViewById(R.id.tvProgressText);
        tvPercent = view.findViewById(R.id.tvPercent);
        pbVaccine = view.findViewById(R.id.pbVaccine);

        if (childId != null) {
            fetchFullImmunizationHistory();
        }
    }

    private void fetchFullImmunizationHistory() {
        DatabaseReference immunizationsRef = FirebaseDatabase.getInstance().getReference("Immunizations");

        immunizationsRef.orderByChild("childId").equalTo(childId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (llFullVaccineHistory != null) llFullVaccineHistory.removeAllViews();

                int completedCount = 0;
                int targetVaccines = 14; // Standard target for infant vaccines to calculate progress

                for (DataSnapshot vaxSnap : snapshot.getChildren()) {
                    completedCount++;

                    String vaxType = vaxSnap.child("vaccineType").getValue(String.class);
                    String dose = vaxSnap.child("doseNo").getValue(String.class);
                    String dateAdministered = vaxSnap.child("dateAdministered").getValue(String.class);
                    String nextSchedule = vaxSnap.child("nextSchedule").getValue(String.class);

                    String title = vaxType;
                    if (dose != null && !dose.isEmpty()) {
                        title += " (Dose " + dose + ")";
                    }

                    addHistoryCardToUI(title, dateAdministered, nextSchedule);
                }

                // Update Progress UI
                if (tvProgressText != null) {
                    tvProgressText.setText(completedCount + " of " + targetVaccines + " completed");
                }
                if (pbVaccine != null) {
                    pbVaccine.setMax(targetVaccines);
                    pbVaccine.setProgress(completedCount);
                }
                if (tvPercent != null) {
                    int percent = (int) (((float) completedCount / targetVaccines) * 100);
                    tvPercent.setText(percent + "%");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load records.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addHistoryCardToUI(String title, String givenDate, String nextDate) {
        if (llFullVaccineHistory == null || getContext() == null) return;

        View card = getLayoutInflater().inflate(R.layout.item_vaccine_history, null);

        TextView tvName = card.findViewById(R.id.tvHistVaccineName);
        TextView tvGiven = card.findViewById(R.id.tvHistGivenDate);
        TextView tvNext = card.findViewById(R.id.tvHistNextDate);
        LinearLayout llNextDose = card.findViewById(R.id.llNextDose);

        tvName.setText(title != null ? title : "Unknown Vaccine");
        tvGiven.setText(givenDate != null ? "Given: " + givenDate : "Given: N/A");

        // Hide the "Next Dose" line if there isn't one scheduled
        if (nextDate != null && !nextDate.isEmpty() && !nextDate.equals("N/A")) {
            tvNext.setText("Next dose: " + nextDate);
        } else {
            llNextDose.setVisibility(View.GONE);
        }

        llFullVaccineHistory.addView(card);
    }
}