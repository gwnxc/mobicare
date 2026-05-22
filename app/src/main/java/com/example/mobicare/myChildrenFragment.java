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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class myChildrenFragment extends Fragment {

    private String loggedInUserId;
    private LinearLayout llChildrenList;
    private TextView tvCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_children, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireActivity().getSharedPreferences("MobicarePrefs", Context.MODE_PRIVATE);
        loggedInUserId = prefs.getString("loggedInUser", null);

        ImageView btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        llChildrenList = view.findViewById(R.id.llChildrenList);
        tvCount = view.findViewById(R.id.tvCount);

        fetchMyChildren(view);
    }

    private void fetchMyChildren(View mainView) {
        if (loggedInUserId == null) return;

        DatabaseReference childrenRef = FirebaseDatabase.getInstance().getReference("Patients_Children");

        // ---> CHANGED: Using "parentUid" based on your Firebase screenshot! <---
        childrenRef.orderByChild("parentUid").equalTo(loggedInUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                llChildrenList.removeAllViews();
                int count = 0;

                for (DataSnapshot childSnap : snapshot.getChildren()) {
                    count++;
                    String childId = childSnap.getKey();

                    // Combine first and last name from your DB
                    String fName = childSnap.child("firstName").getValue(String.class);
                    String lName = childSnap.child("lastName").getValue(String.class);
                    String fullName = (fName != null ? fName : "") + " " + (lName != null ? lName : "");

                    String gender = childSnap.child("gender").getValue(String.class);
                    String birthDateStr = childSnap.child("birthDate").getValue(String.class);

                    // Note: Weight & Height aren't in your DB screenshot, so we default to "--"
                    // If you add them later, change this to fetch them.
                    String weight = "--";
                    String height = "--";

                    String age = calculateAge(birthDateStr);

                    addCardToUI(childId, fullName.trim(), age, gender, weight, height, mainView);
                }
                tvCount.setText(count + " children registered");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load children", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addCardToUI(String childId, String name, String age, String gender, String weight, String height, View mainView) {
        // Pass the parent layout (llChildrenList) and 'false' so it keeps your XML margins!
        View card = getLayoutInflater().inflate(R.layout.item_child_card, llChildrenList, false);

        TextView tvName = card.findViewById(R.id.tvChildName);
        TextView tvBasic = card.findViewById(R.id.tvChildBasicDetails);
        TextView tvStats = card.findViewById(R.id.tvChildStats);

        tvName.setText(name);
        tvBasic.setText(age + " • " + (gender != null ? gender : "N/A"));
        tvStats.setText(weight + " kg  •  " + height + " cm");

        card.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("selectedChildId", childId);
            Navigation.findNavController(mainView).navigate(R.id.action_myChildrenFragment_to_childProfileFragment, bundle);
        });

        llChildrenList.addView(card);
    }

    // Helper method to calculate "X months" or "X years"
    private String calculateAge(String birthDateStr) {
        if (birthDateStr == null || birthDateStr.isEmpty()) return "Unknown age";
        try {
            // Assuming format is DD/MM/YYYY based on your DB screenshot (01/05/2026)
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

            if (age > 0) {
                return age + (age == 1 ? " year" : " years");
            } else {
                return months + (months == 1 ? " month" : " months");
            }
        } catch (ParseException e) {
            return "Infant";
        }
    }
}