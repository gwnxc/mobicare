package com.example.mobicare;

import android.graphics.Color;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class childProfileFragment extends Fragment {

    private String childId;
    private LinearLayout llVaccineList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_child_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            childId = getArguments().getString("selectedChildId");
        }

        ImageView btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        llVaccineList = view.findViewById(R.id.llVaccineList);

        if (childId != null) {
            fetchChildDetails(view);
            fetchChildImmunizations();
        } else {
            Toast.makeText(getContext(), "Error loading child profile", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigateUp();
        }

        // ---> FIXED: Moved the View Records button logic here! <---
        com.google.android.material.button.MaterialButton btnViewRecords = view.findViewById(R.id.btnViewRecords);
        if (btnViewRecords != null) {
            btnViewRecords.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString("selectedChildId", childId);

                // Get the name from the TextView to pass to the next screen
                TextView tvName = view.findViewById(R.id.tvProfileName);
                if (tvName != null) {
                    bundle.putString("selectedChildName", tvName.getText().toString());
                }

                Navigation.findNavController(view).navigate(R.id.action_childProfileFragment_to_healthRecordsFragment, bundle);
            });
        }
    }

    private void fetchChildDetails(View view) {
        DatabaseReference childRef = FirebaseDatabase.getInstance().getReference("Patients_Children").child(childId);

        childRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Fetching from Firebase keys
                    String fName = snapshot.child("firstName").getValue(String.class);
                    String mName = snapshot.child("middleName").getValue(String.class);
                    String lName = snapshot.child("lastName").getValue(String.class);

                    // Concatenating names correctly
                    String fullName = (fName != null ? fName : "") + " " +
                            (mName != null ? mName + " " : "") +
                            (lName != null ? lName : "");

                    String gender = snapshot.child("gender").getValue(String.class);
                    String birth = snapshot.child("birthDate").getValue(String.class);
                    String bPlace = snapshot.child("placeOfBirth").getValue(String.class);

                    // Binding to views
                    TextView tvName = view.findViewById(R.id.tvProfileName);
                    TextView tvBasic = view.findViewById(R.id.tvProfileBasic);
                    TextView tvBirth = view.findViewById(R.id.tvProfileBirth);
                    TextView tvBirthPlace = view.findViewById(R.id.tvProfileBirthPlace);

                    if (tvName != null) tvName.setText(fullName.trim());
                    if (tvBasic != null) tvBasic.setText(calculateAge(birth) + " • " + (gender != null ? gender : "N/A"));
                    if (tvBirth != null) tvBirth.setText(birth != null ? birth : "--");

                    // Bind the new Place of Birth field
                    if (tvBirthPlace != null) {
                        tvBirthPlace.setText("Place of Birth: " + (bPlace != null ? bPlace : "Not specified"));
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchChildImmunizations() {
        DatabaseReference immunizationsRef = FirebaseDatabase.getInstance().getReference("Immunizations");

        // Query immunizations where childId matches our current child
        immunizationsRef.orderByChild("childId").equalTo(childId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (llVaccineList != null) llVaccineList.removeAllViews();

                int vaccineCount = 0;

                for (DataSnapshot vaxSnap : snapshot.getChildren()) {
                    vaccineCount++;
                    String vaxType = vaxSnap.child("vaccineType").getValue(String.class);
                    String dose = vaxSnap.child("doseNo").getValue(String.class);
                    String dateAdministered = vaxSnap.child("dateAdministered").getValue(String.class);

                    String title = vaxType;
                    if (dose != null && !dose.isEmpty()) {
                        title += " (Dose " + dose + ")";
                    }

                    addVaccineToUI(title, dateAdministered);
                }

                if (vaccineCount == 0 && llVaccineList != null) {
                    TextView tvNoVax = new TextView(getContext());
                    tvNoVax.setText("No immunization records found.");
                    tvNoVax.setTextColor(Color.parseColor("#94A3B8"));
                    llVaccineList.addView(tvNoVax);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addVaccineToUI(String title, String date) {
        if (llVaccineList == null || getContext() == null) return;

        View vaxView = getLayoutInflater().inflate(R.layout.item_vaccine_record, null);

        TextView tvTitle = vaxView.findViewById(R.id.tvVaccineName);
        TextView tvDate = vaxView.findViewById(R.id.tvVaccineDate);

        tvTitle.setText(title != null ? title : "Unknown Vaccine");
        tvDate.setText(date != null ? date : "Date unknown");

        // Add a simple divider line between rows
        View divider = new View(getContext());
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(Color.parseColor("#F1F5F9"));

        llVaccineList.addView(vaxView);
        llVaccineList.addView(divider);
    }

    private String calculateAge(String birthDateStr) {
        if (birthDateStr == null || birthDateStr.isEmpty()) return "Unknown age";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date birthDate = sdf.parse(birthDateStr);
            if (birthDate == null) return "Unknown age";

            Calendar dob = Calendar.getInstance();
            dob.setTime(birthDate);
            Calendar today = Calendar.getInstance();

            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
            int months = today.get(Calendar.MONTH) - dob.get(Calendar.MONTH);

            if (months < 0 || (months == 0 && today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))) {
                age--;
                months += 12;
            }

            if (age > 0) return age + (age == 1 ? " year" : " years");
            else return months + (months == 1 ? " month" : " months");
        } catch (ParseException e) {
            return "Infant";
        }
    }
}