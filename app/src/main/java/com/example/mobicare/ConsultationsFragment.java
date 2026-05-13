package com.example.mobicare;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

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

import java.util.ArrayList;
import java.util.List;

public class ConsultationsFragment extends Fragment {

    private LinearLayout llUpcoming, llCompleted;
    private TextView tvUpcomingCount, tvCompletedCount;
    private DatabaseReference mDatabase;
    private String currentUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_consultations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Acknowledging our Auth/Prefs Check for Maria/Health Workers
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            SharedPreferences prefs = requireContext().getSharedPreferences("MobiCarePrefs", Context.MODE_PRIVATE);
            currentUid = prefs.getString("loggedUserKey", "");
        }

        // 2. Link the UI Containers from your XML
        llUpcoming = view.findViewById(R.id.llUpcoming);
        llCompleted = view.findViewById(R.id.llCompleted);
        tvUpcomingCount = view.findViewById(R.id.tvUpcomingCount);
        tvCompletedCount = view.findViewById(R.id.tvCompletedCount);

        // 3. Back Button (Matches R.id.btnBack in your XML)
        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        }

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Consultations");
        fetchConsultations();
    }

    private void fetchConsultations() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                llUpcoming.removeAllViews();
                llCompleted.removeAllViews();

                int upcoming = 0;
                int completed = 0;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Consultation c = ds.getValue(Consultation.class);
                    if (c != null) {
                        c.id = ds.getKey();

                        // Sort into the correct container based on status
                        if ("scheduled".equalsIgnoreCase(c.status) || "pending".equalsIgnoreCase(c.status)) {
                            upcoming++;
                            addConsultationCardToUI(llUpcoming, c);
                        } else {
                            completed++;
                            addConsultationCardToUI(llCompleted, c);
                        }
                    }
                }

                // Update the counters in your XML
                tvUpcomingCount.setText("Upcoming (" + upcoming + ")");
                tvCompletedCount.setText("Completed (" + completed + ")");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addConsultationCardToUI(LinearLayout container, Consultation consultation) {
        // This manually inflates the items since we aren't using a RecyclerView anymore
        View card = getLayoutInflater().inflate(R.layout.item_consultation_card, container, false);

        // Find views inside your item_consultation_card.xml
        TextView tvName = card.findViewById(R.id.tvPatientName);
        TextView tvDate = card.findViewById(R.id.tvDateTime);

        if (tvName != null) tvName.setText(consultation.patientName);
        if (tvDate != null) tvDate.setText(consultation.date);

        card.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("consultationId", consultation.id);
            bundle.putString("status", consultation.status);
            Navigation.findNavController(requireView()).navigate(R.id.consultationDetailsFragment, bundle);
        });

        container.addView(card);
    }
}