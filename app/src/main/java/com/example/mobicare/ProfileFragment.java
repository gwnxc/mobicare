package com.example.mobicare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
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

public class ProfileFragment extends Fragment {

    private TextView tvHeaderName, tvHeaderRole;
    private View rowUser, rowPhone, rowEmail, rowSpec, rowDate;
    private DatabaseReference mDatabase;
    private String currentUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Dual-ID logic (Auth for Patients, SharedPreferences for Health Workers)
        String userId = "";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("MobiCarePrefs", android.content.Context.MODE_PRIVATE);
            userId = prefs.getString("loggedUserKey", "");
        }
        this.currentUid = userId;

        // 2. Initialize Header Views
        tvHeaderName = view.findViewById(R.id.tvProfileFullName);
        tvHeaderRole = view.findViewById(R.id.tvProfileSubtitleRole);

        // 3. Initialize and Setup Information Rows
        setupInfoRows(view);

        // 4. Setup Firebase (Point to HealthWorkers using currentUid)
        if (!currentUid.isEmpty()) {
            mDatabase = FirebaseDatabase.getInstance().getReference("HealthWorkers").child(currentUid);
        }

        // 5. Navigation Listeners
        view.findViewById(R.id.btnBackProfile).setOnClickListener(v -> requireActivity().onBackPressed());

        view.findViewById(R.id.cvAddRecordProfile).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.addConsultationFragment));

        view.findViewById(R.id.cvViewConsultationsProfile).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.consultationsFragment));

        // Logout stack logic
        View btnLogoutContainer = view.findViewById(R.id.containerLogout);
        if (btnLogoutContainer != null) {
            btnLogoutContainer.setOnClickListener(v -> showLogoutConfirmation());
        }

        // 6. Load Data
        loadProfileData();
    }

    private void setupInfoRows(View view) {
        rowUser = view.findViewById(R.id.layoutUsername);
        rowPhone = view.findViewById(R.id.layoutPhone);
        rowEmail = view.findViewById(R.id.layoutEmail);
        rowSpec = view.findViewById(R.id.layoutSpec);
        rowDate = view.findViewById(R.id.layoutDate);

        setRowDetails(rowUser, "Username", R.drawable.ic_profile);
        setRowDetails(rowPhone, "Phone Number", R.drawable.ic_phone);
        setRowDetails(rowEmail, "Email", R.drawable.ic_mail);
        setRowDetails(rowSpec, "Specialization", R.drawable.ic_work);
        setRowDetails(rowDate, "Date Added", R.drawable.ic_calendar);
    }

    private void setRowDetails(View row, String label, int iconRes) {
        if (row != null) {
            ((TextView) row.findViewById(R.id.tvInfoLabel)).setText(label);
            ((ImageView) row.findViewById(R.id.ivInfoIcon)).setImageResource(iconRes);
        }
    }

    private void loadProfileData() {
        // 1. Get the key from SharedPreferences (Maria's path)
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("MobiCarePrefs", android.content.Context.MODE_PRIVATE);
        String userKey = prefs.getString("loggedUserKey", "");

        if (userKey.isEmpty()) return;

        // 2. Point specifically to the HealthWorker's node
        DatabaseReference profileRef = FirebaseDatabase.getInstance().getReference("HealthWorkers").child(userKey);

        profileRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAdded()) {
                    // Check if your Firebase keys match these exactly (e.g., "fullName" vs "name")
                    String name = snapshot.child("fullName").getValue(String.class);
                    String spec = snapshot.child("specialization").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String user = snapshot.child("username").getValue(String.class);
                    Long date = snapshot.child("registrationDate").getValue(Long.class);

                    // Update Header
                    tvHeaderName.setText(name != null ? name : "N/A");
                    tvHeaderRole.setText(spec != null ? spec : "Health Worker");

                    // Update Row Values - IMPORTANT: Check that rowUser, rowPhone, etc., are initialized
                    updateRowValue(rowUser, user);
                    updateRowValue(rowPhone, phone);
                    updateRowValue(rowEmail, email);
                    updateRowValue(rowSpec, spec);

                    if (date != null) {
                        String dateStr = android.text.format.DateFormat.format("MM/dd/yyyy", new java.util.Date(date)).toString();
                        updateRowValue(rowDate, dateStr);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Helper method to safely update the 'tvInfoValue' inside your included layouts
    private void updateRowValue(View row, String value) {
        if (row != null && value != null) {
            TextView tvValue = row.findViewById(R.id.tvInfoValue);
            if (tvValue != null) {
                tvValue.setText(value);
            }
        }
    }

    private void showLogoutConfirmation() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_logout, null);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnCancelLogout).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnConfirmLogout).setOnClickListener(v -> {
            dialog.dismiss();

            // 1. Clear Firebase Auth (For Patients)
            FirebaseAuth.getInstance().signOut();

            // 2. Clear Maria's session (For Health Workers)
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("MobiCarePrefs", android.content.Context.MODE_PRIVATE);
            prefs.edit().clear().apply();

            // 3. Navigate back to Login
            Navigation.findNavController(requireView()).navigate(R.id.loginFragment);

            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }
}