package com.example.mobicare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections; // Added for sorting
import java.util.List;

public class ConsultationsFragment extends Fragment {

    private RecyclerView rvConsultations;
    private ConsultationAdapter adapter;
    private List<Consultation> consultationList = new ArrayList<>();
    private String currentUid;
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_consultations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String userId = "";

        // 1. Check if a real Firebase Auth user exists (For Patients/Guardians)
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        // 2. If not, check SharedPreferences (For Health Workers like Maria)
        else {
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("MobiCarePrefs", android.content.Context.MODE_PRIVATE);
            userId = prefs.getString("loggedUserKey", "");
        }

        // Now use 'userId' for all your Firebase queries!
        this.currentUid = userId;

        rvConsultations = view.findViewById(R.id.rvConsultations);
        rvConsultations.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize adapter once
        adapter = new ConsultationAdapter(consultationList, consultation -> {
            Bundle bundle = new Bundle();
            bundle.putString("consultationId", consultation.id);
            bundle.putString("status", consultation.status);
            Navigation.findNavController(requireView()).navigate(R.id.consultationDetailsFragment, bundle);
        });
        rvConsultations.setAdapter(adapter);

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Consultations");
        fetchConsultations();

        view.findViewById(R.id.btnBackConsultations).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }

    private void fetchConsultations() {
        // Get the key from SharedPreferences
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("MobiCarePrefs", android.content.Context.MODE_PRIVATE);
        String userKey = prefs.getString("loggedUserKey", "");

        // For a Health Worker, you usually want to see ALL consultations in the system
        // So we remove the .equalTo() filter entirely
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                consultationList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Consultation c = ds.getValue(Consultation.class);
                    if (c != null) {
                        c.id = ds.getKey();
                        consultationList.add(c);
                    }
                }
                java.util.Collections.reverse(consultationList);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}