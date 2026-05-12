package com.example.mobicare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

public class consultations extends Fragment {

    private DatabaseReference mDatabase;
    private LinearLayout llUpcoming, llCompleted;
    private TextView tvUpcomingCount, tvCompletedCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_consultations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Point to the Consultations node from your screenshot
        mDatabase = FirebaseDatabase.getInstance().getReference("Consultations");

        ImageView btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        llUpcoming = view.findViewById(R.id.llUpcoming);
        llCompleted = view.findViewById(R.id.llCompleted);
        tvUpcomingCount = view.findViewById(R.id.tvUpcomingCount);
        tvCompletedCount = view.findViewById(R.id.tvCompletedCount);

        fetchConsultations();
    }

    private void fetchConsultations() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (llUpcoming != null) llUpcoming.removeAllViews();
                if (llCompleted != null) llCompleted.removeAllViews();

                int upcomingCount = 0;
                int completedCount = 0;
                long currentTime = System.currentTimeMillis();

                for (DataSnapshot consultSnap : snapshot.getChildren()) {
                    String patientName = consultSnap.child("patientName").getValue(String.class);
                    String patientType = consultSnap.child("patientType").getValue(String.class);
                    String purpose = consultSnap.child("purpose").getValue(String.class);
                    String date = consultSnap.child("date").getValue(String.class);
                    String time = consultSnap.child("time").getValue(String.class);
                    String healthWorker = consultSnap.child("healthWorker").getValue(String.class);
                    Long timestamp = consultSnap.child("timestamp").getValue(Long.class);

                    // Create the view card
                    View card = getLayoutInflater().inflate(R.layout.item_consultation_card, null);

                    ((TextView) card.findViewById(R.id.tvPatientName)).setText(patientName != null ? patientName : "Unknown");
                    ((TextView) card.findViewById(R.id.tvPatientType)).setText(patientType != null ? patientType : "");
                    ((TextView) card.findViewById(R.id.tvPurpose)).setText("Purpose: " + (purpose != null ? purpose : ""));
                    ((TextView) card.findViewById(R.id.tvDateTime)).setText("📅 " + date + " at " + time);
                    ((TextView) card.findViewById(R.id.tvWorker)).setText("Assigned to: " + (healthWorker != null ? healthWorker : ""));

                    // Sort into the correct list based on the timestamp
                    if (timestamp != null && timestamp > currentTime) {
                        upcomingCount++;
                        llUpcoming.addView(card);
                    } else {
                        completedCount++;
                        llCompleted.addView(card);
                    }
                }

                if (tvUpcomingCount != null) tvUpcomingCount.setText("Upcoming (" + upcomingCount + ")");
                if (tvCompletedCount != null) tvCompletedCount.setText("Completed (" + completedCount + ")");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load consultations", Toast.LENGTH_SHORT).show();
            }
        });
    }
}