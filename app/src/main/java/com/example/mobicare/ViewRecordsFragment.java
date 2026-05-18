package com.example.mobicare;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class ViewRecordsFragment extends Fragment {

    private RecyclerView rvRecords;
    private SearchView searchView;
    private ChipGroup chipGroup;
    private DatabaseReference mDatabase;

    // Stats Card Views
    private View cardTotal, cardGuardians, cardChildren;
    private TextView tvTotalCount, tvGuardianCount, tvChildCount;

    private List<Object> allRecords = new ArrayList<>();
    private RecordAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_view_records, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // UI Initialization - Accessible to everyone now
        rvRecords = view.findViewById(R.id.rvRecords);
        searchView = view.findViewById(R.id.searchView);
        chipGroup = view.findViewById(R.id.chipGroupFilter);

        // Initialize Stats Cards
        cardTotal = view.findViewById(R.id.cardTotal);
        cardGuardians = view.findViewById(R.id.cardGuardians);
        cardChildren = view.findViewById(R.id.cardChildren);

        setupStatsCardUI();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        loadDataFromFirebase();

        // Search Listener
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) adapter.getFilter().filter(newText);
                return true;
            }
        });

        // Filter Listener
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (adapter == null) return;
            if (checkedId == R.id.chipGuardians) adapter.getFilter().filter("Guardian");
            else if (checkedId == R.id.chipChildren) adapter.getFilter().filter("Child");
            else adapter.getFilter().filter("");
        });

        // Common Back Button logic
        view.findViewById(R.id.btnBackRecords).setOnClickListener(v ->
                androidx.navigation.Navigation.findNavController(v).navigateUp());
    }

    private void setupStatsCardUI() {
        // Total Card Styling
        ((TextView)cardTotal.findViewById(R.id.tvStatLabel)).setText("Total");
        ((ImageView)cardTotal.findViewById(R.id.ivStatIcon)).setImageResource(R.drawable.ic_guardian);
        tvTotalCount = cardTotal.findViewById(R.id.tvStatCount);

        // Guardians Card Styling
        ((TextView)cardGuardians.findViewById(R.id.tvStatLabel)).setText("Guardians");
        ((ImageView)cardGuardians.findViewById(R.id.ivStatIcon)).setImageResource(R.drawable.ic_profile);
        ((ImageView)cardGuardians.findViewById(R.id.ivStatIcon)).setColorFilter(Color.parseColor("#1ABC9C"));
        cardGuardians.findViewById(R.id.viewIconBg).setBackgroundResource(R.drawable.circle_light_green);
        tvGuardianCount = cardGuardians.findViewById(R.id.tvStatCount);

        // Children Card Styling
        ((TextView)cardChildren.findViewById(R.id.tvStatLabel)).setText("Children");
        ((ImageView)cardChildren.findViewById(R.id.ivStatIcon)).setImageResource(R.drawable.ic_child);
        ((ImageView)cardChildren.findViewById(R.id.ivStatIcon)).setColorFilter(Color.parseColor("#F1C40F"));
        cardChildren.findViewById(R.id.viewIconBg).setBackgroundResource(R.drawable.circle_light_yellow);
        tvChildCount = cardChildren.findViewById(R.id.tvStatCount);
    }

    private void loadDataFromFirebase() {
        mDatabase.child("Patients_Guardians").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                int guardianCount = (int) snapshot.getChildrenCount();
                if (tvGuardianCount != null) tvGuardianCount.setText(String.valueOf(guardianCount));

                allRecords.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Mother mother = ds.getValue(Mother.class);
                    if (mother != null) {
                        // Force the key inside the model
                        mother.setLinkedUid(ds.getKey());
                        allRecords.add(mother);
                    }
                }
                fetchChildren(guardianCount);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchChildren(int guardianCount) {
        mDatabase.child("Patients_Children").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                int childCount = (int) snapshot.getChildrenCount();
                if (tvChildCount != null) tvChildCount.setText(String.valueOf(childCount));
                if (tvTotalCount != null) tvTotalCount.setText(String.valueOf(guardianCount + childCount));

                List<Object> continuousList = new ArrayList<>(allRecords);
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Child child = ds.getValue(Child.class);
                    if (child != null) {
                        // CRITICAL FIX: Explicitly assign the push key to the child model
                        child.setChildId(ds.getKey());
                        continuousList.add(child);
                    }
                }

                if (rvRecords.getLayoutManager() == null) {
                    rvRecords.setLayoutManager(new LinearLayoutManager(getContext()));
                }

                // Always instantiate a clean adapter to reload items cleanly when returning to this screen
                adapter = new RecordAdapter(continuousList);
                rvRecords.setAdapter(adapter);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}