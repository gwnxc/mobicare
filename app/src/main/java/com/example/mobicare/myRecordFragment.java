package com.example.mobicare;

import android.content.Context;
import android.content.SharedPreferences;
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

public class myRecordFragment extends Fragment {

    private String loggedInUserId;
    private LinearLayout llPrenatalRecords;
    private TextView tvEmptyState;
    private TextView tvMotherName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireActivity().getSharedPreferences("MobicarePrefs", Context.MODE_PRIVATE);
        loggedInUserId = prefs.getString("loggedInUser", null);

        ImageView btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        llPrenatalRecords = view.findViewById(R.id.llPrenatalRecords);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        tvMotherName = view.findViewById(R.id.tvMotherName);

        if (loggedInUserId != null) {
            fetchMotherName();
            fetchPrenatalRecords();
        }
    }

    private void fetchMotherName() {
        DatabaseReference motherRef = FirebaseDatabase.getInstance().getReference("Patients_Guardians").child(loggedInUserId);
        motherRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    if (name != null) tvMotherName.setText(name);
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
                llPrenatalRecords.removeAllViews();

                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    return;
                }

                tvEmptyState.setVisibility(View.GONE);

                for (DataSnapshot recordSnap : snapshot.getChildren()) {
                    String date = recordSnap.child("date").getValue(String.class);
                    String bp = recordSnap.child("bloodPressure").getValue(String.class);
                    String weight = recordSnap.child("weight").getValue(String.class);
                    String fh = recordSnap.child("fundalHeight").getValue(String.class);
                    String fht = recordSnap.child("fetalHeartTone").getValue(String.class);
                    String tt = recordSnap.child("tetanusToxoid").getValue(String.class);
                    String diagnosis = recordSnap.child("diagnosisTreatment").getValue(String.class);
                    String returnDate = recordSnap.child("returnCheckupDate").getValue(String.class);

                    addRecordToUI(date, bp, weight, fh, fht, tt, diagnosis, returnDate);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load records.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addRecordToUI(String date, String bp, String weight, String fh, String fht, String tt, String diagnosis, String returnDate) {
        if (getContext() == null) return;
        View card = getLayoutInflater().inflate(R.layout.item_prenatal_record, null);

        TextView tvDate = card.findViewById(R.id.tvPreDate);
        TextView tvBP = card.findViewById(R.id.tvPreBP);
        TextView tvWeight = card.findViewById(R.id.tvPreWeight);
        TextView tvFH = card.findViewById(R.id.tvPreFH);
        TextView tvFHT = card.findViewById(R.id.tvPreFHT);
        TextView tvTT = card.findViewById(R.id.tvPreTT);
        TextView tvDiag = card.findViewById(R.id.tvPreDiagnosis);
        TextView tvReturn = card.findViewById(R.id.tvPreReturn);

        tvDate.setText(date != null ? date : "--");
        tvBP.setText(bp != null ? bp : "--");
        tvWeight.setText(weight != null ? weight + " kg" : "-- kg");

        // Clean up formatting in case they already typed "cm" or "bpm" in DB
        tvFH.setText(fh != null ? (fh.contains("cm") ? fh : fh + " cm") : "-- cm");
        tvFHT.setText(fht != null ? (fht.contains("bpm") ? fht : fht + " bpm") : "-- bpm");

        tvTT.setText(tt != null ? "TT Given: " + tt : "TT Given: N/A");
        tvDiag.setText(diagnosis != null ? "Diagnosis/Treatment: " + diagnosis : "Diagnosis/Treatment: N/A");
        tvReturn.setText(returnDate != null ? "Return check-up: " + returnDate : "Return check-up: N/A");

        llPrenatalRecords.addView(card);
    }
}