package com.example.mobicare;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            View customBottomNav = findViewById(R.id.bottomNavigation);

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();

                if (id == R.id.loginFragment || id == R.id.splashFragment ||
                        id == R.id.viewRecordsFragment || id == R.id.registerFragment) {

                    customBottomNav.setVisibility(View.GONE);
                } else {
                    customBottomNav.setVisibility(View.VISIBLE);
                    updateNavColors(id);
                }
            });

            findViewById(R.id.nav_home).setOnClickListener(v -> navController.navigate(R.id.healthWorkerDashboardFragment));
            findViewById(R.id.nav_alerts).setOnClickListener(v -> navController.navigate(R.id.alertsFragment));
            findViewById(R.id.nav_consultations).setOnClickListener(v -> navController.navigate(R.id.consultationsFragment));
            findViewById(R.id.nav_add).setOnClickListener(v -> navController.navigate(R.id.addConsultationFragment));
            findViewById(R.id.nav_profile).setOnClickListener(v -> navController.navigate(R.id.profileFragment));
        }

        // Global Notification Listener for the Nav Bar Badge
        listenForGlobalNotifications();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#155A91"));
            getWindow().getDecorView().setSystemUiVisibility(0);
        }

        View navHostView = findViewById(R.id.nav_host_fragment);
        if (navHostView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(navHostView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
                int bottomPadding = (imeInsets.bottom > 0) ? imeInsets.bottom : 0;
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding);
                return insets;
            });
        }
    }

    private void listenForGlobalNotifications() {
        // 1. Get the ID (Same logic as the Fragments)
        String userId = "";
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            android.content.SharedPreferences prefs = getSharedPreferences("MobiCarePrefs", MODE_PRIVATE);
            userId = prefs.getString("loggedUserKey", "");
        }

        if (userId.isEmpty()) return;

        // 2. Query for unread notifications for THIS user
        mDatabase.child("Notifications")
                .orderByChild("receiverUid")
                .equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int unreadCount = 0;
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Boolean isRead = ds.child("isRead").getValue(Boolean.class);
                            if (isRead != null && !isRead) unreadCount++;
                        }
                        updateGlobalNavBadge(unreadCount);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updateGlobalNavBadge(int count) {
        TextView tvNavBadge = findViewById(R.id.tvBadgeNav);
        if (tvNavBadge != null) {
            if (count > 0) {
                tvNavBadge.setVisibility(View.VISIBLE);
                tvNavBadge.setText(String.valueOf(count));
            } else {
                tvNavBadge.setVisibility(View.GONE);
            }
        }
    }

    private void updateNavColors(int currentId) {
        int activeColor = Color.parseColor("#155A91");
        int inactiveColor = Color.parseColor("#8E8E8E");

        // Logic: Highlight if on the list OR on the details screen
        boolean isConsultationActive = (currentId == R.id.consultationsFragment ||
                currentId == R.id.consultationDetailsFragment);
        updateItemColor(R.id.nav_home, currentId == R.id.healthWorkerDashboardFragment, activeColor, inactiveColor);
        updateItemColor(R.id.nav_consultations, isConsultationActive, activeColor, inactiveColor);
        updateItemColor(R.id.nav_add, currentId == R.id.addConsultationFragment, activeColor, inactiveColor);
        updateItemColor(R.id.nav_alerts, currentId == R.id.alertsFragment, activeColor, inactiveColor);
        updateItemColor(R.id.nav_profile, currentId == R.id.profileFragment, activeColor, inactiveColor);
    }

    private void updateItemColor(int layoutId, boolean isActive, int activeColor, int inactiveColor) {
        View layout = findViewById(layoutId);
        if (layout instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) layout;
            int color = isActive ? activeColor : inactiveColor;

            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof ImageView) {
                    ((ImageView) child).setColorFilter(color);
                } else if (child instanceof TextView) {
                    ((TextView) child).setTextColor(color);
                } else if (child instanceof FrameLayout) {
                    FrameLayout frame = (FrameLayout) child;
                    for (int j = 0; j < frame.getChildCount(); j++) {
                        View innerChild = frame.getChildAt(j);
                        if (innerChild instanceof ImageView) {
                            ((ImageView) innerChild).setColorFilter(color);
                        }
                    }
                }
            }
        }
    }
}