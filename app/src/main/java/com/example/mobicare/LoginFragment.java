package com.example.mobicare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

public class LoginFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Role Selection
        MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.toggleGroup);
        
        // Sign Up Link
        TextView tvSignUp = view.findViewById(R.id.tvSignUp);
        tvSignUp.setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_registerFragment);
        });

        // Sign In Button
        MaterialButton btnLogin = view.findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(v -> {
            int checkedId = toggleGroup.getCheckedButtonId();
            
            if (checkedId == R.id.btnHealthWorker) {
                // Navigate to Health Worker Dashboard
                Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_healthWorkerDashboardFragment);
            } else if (checkedId == R.id.btnMother) {
                Toast.makeText(getContext(), "Mother Dashboard coming soon!", Toast.LENGTH_SHORT).show();
            } else if (checkedId == R.id.btnAdmin) {
                Toast.makeText(getContext(), "Admin Dashboard coming soon!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Please select a role first", Toast.LENGTH_SHORT).show();
            }
        });
    }
}