package com.example.mobicare;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class inventory extends Fragment {

    private DatabaseReference mDatabase;
    private LinearLayout llInventoryList;
    private TextView tvCount;
    private EditText etSearchItem;
    private MaterialButtonToggleGroup toggleFilter;

    // Master lists for searching and filtering
    private List<InventoryItem> masterList = new ArrayList<>();
    private List<InventoryItem> filteredList = new ArrayList<>();

    private String currentSearchQuery = "";
    private String currentFilterType = "All"; // "All", "Medication", "Vaccination"

    public inventory() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inventory, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Hide Bottom Nav if opened from Quick Actions
        boolean showBottomNav = true;
        if (getArguments() != null) {
            showBottomNav = getArguments().getBoolean("showBottomNav", true);
        }
        View bottomNavLayout = view.findViewById(R.id.bottomNavigation);
        if (bottomNavLayout != null) {
            bottomNavLayout.setVisibility(showBottomNav ? View.VISIBLE : View.GONE);
        }

        mDatabase = FirebaseDatabase.getInstance().getReference("Inventory");

        // UI Setup
        ImageView btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        llInventoryList = view.findViewById(R.id.llInventoryList);
        tvCount = view.findViewById(R.id.tvCount);
        etSearchItem = view.findViewById(R.id.etSearchItem);
        toggleFilter = view.findViewById(R.id.toggleFilter);

        MaterialButton btnAddItem = view.findViewById(R.id.btnAddItem);
        if (btnAddItem != null) {
            btnAddItem.setOnClickListener(v -> showInventoryDialog(null, "", "Medication", "", ""));
        }

        setupBottomNavigation(view);
        setupInteractions();
        fetchInventory();
    }

    private void setupInteractions() {
        // Search Bar Listener
        if (etSearchItem != null) {
            etSearchItem.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s.toString().toLowerCase().trim();
                    applyFilters();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // Filter Toggle Listener
        if (toggleFilter != null) {
            toggleFilter.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    if (checkedId == R.id.btnFilterAll) currentFilterType = "All";
                    else if (checkedId == R.id.btnFilterMeds) currentFilterType = "Medication";
                    else if (checkedId == R.id.btnFilterVax) currentFilterType = "Vaccination";
                    applyFilters();
                }
            });
        }
    }

    private void fetchInventory() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                masterList.clear();

                for (DataSnapshot itemSnap : snapshot.getChildren()) {
                    String id = itemSnap.getKey();
                    String name = itemSnap.child("itemName").getValue(String.class);
                    String type = itemSnap.child("itemType").getValue(String.class);
                    String qty = itemSnap.child("quantity").getValue(String.class);
                    String expiry = itemSnap.child("expiryDate").getValue(String.class);

                    if (name != null && !name.trim().isEmpty()) {
                        masterList.add(new InventoryItem(id, name, type, qty, expiry));
                    }
                }

                // Once data is loaded, apply filters to update the screen
                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load inventory.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        filteredList.clear();

        for (InventoryItem item : masterList) {
            // 1. Check Category Filter
            boolean matchesType = currentFilterType.equals("All") || currentFilterType.equals(item.type);

            // 2. Check Search Query
            boolean matchesSearch = item.name.toLowerCase().contains(currentSearchQuery);

            if (matchesType && matchesSearch) {
                filteredList.add(item);
            }
        }

        updateUI();
    }

    private void updateUI() {
        if (llInventoryList == null) return;
        llInventoryList.removeAllViews();

        for (InventoryItem item : filteredList) {
            addInventoryCardToUI(item.id, item.name, item.type, item.qty, item.expiry);
        }

        if (tvCount != null) {
            tvCount.setText(filteredList.size() + " items found");
        }
    }

    private void addInventoryCardToUI(String id, String name, String type, String qty, String expiry) {
        View card = getLayoutInflater().inflate(R.layout.item_inventory_card, null);

        TextView tvName = card.findViewById(R.id.tvItemName);
        TextView tvType = card.findViewById(R.id.tvItemType);
        TextView tvQty = card.findViewById(R.id.tvQuantity);
        TextView tvExpiry = card.findViewById(R.id.tvExpiry);

        tvName.setText(name);
        tvQty.setText(qty != null && !qty.isEmpty() ? "📦 Stock: " + qty : "📦 Stock: 0");
        tvExpiry.setText(expiry != null && !expiry.isEmpty() ? "⏳ Expires: " + expiry : "⏳ No Expiry");

        if (type != null) {
            tvType.setText(type);
            if (type.equals("Vaccination")) {
                tvType.setBackgroundResource(R.drawable.badge_green_light);
                tvType.setTextColor(Color.rgb(0x1B, 0x5E, 0x20));
            } else {
                tvType.setBackgroundResource(R.drawable.badge_blue_light);
                tvType.setTextColor(Color.rgb(0x00, 0x4D, 0xAA));
            }
        }

        card.findViewById(R.id.btnEdit).setOnClickListener(v -> showInventoryDialog(id, name, type, qty, expiry));

        card.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            mDatabase.child(id).removeValue();
            Toast.makeText(getContext(), "Item deleted", Toast.LENGTH_SHORT).show();
        });

        if (llInventoryList != null) llInventoryList.addView(card);
    }

    private void showInventoryDialog(@Nullable String existingId, String currentName, String currentType, String currentQty, String currentExpiry) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_inventory, null);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        EditText etName = dialogView.findViewById(R.id.etItemName);
        Spinner spinnerType = dialogView.findViewById(R.id.spinnerItemType);
        EditText etQty = dialogView.findViewById(R.id.etQuantity);
        EditText etExpiry = dialogView.findViewById(R.id.etExpiryDate);

        MaterialButton btnSave = dialogView.findViewById(R.id.btnDialogSave);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnDialogCancel);
        ImageView btnClose = dialogView.findViewById(R.id.btnDialogClose);

        String[] types = {"Medication", "Vaccination"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, types);
        spinnerType.setAdapter(adapter);

        if (existingId != null) {
            tvDialogTitle.setText("Edit Item");
            etName.setText(currentName);
            etQty.setText(currentQty);
            etExpiry.setText(currentExpiry);

            for (int i = 0; i < types.length; i++) {
                if (types[i].equals(currentType)) {
                    spinnerType.setSelection(i);
                    break;
                }
            }
        }

        etExpiry.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view1, selectedYear, selectedMonth, selectedDay) -> {
                        String date = String.format(Locale.getDefault(), "%02d/%02d/%d", selectedMonth + 1, selectedDay, selectedYear);
                        etExpiry.setText(date);
                    },
                    year, month, day);
            datePickerDialog.show();
        });

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String type = spinnerType.getSelectedItem().toString();
            String qty = etQty.getText().toString().trim();
            String expiry = etExpiry.getText().toString().trim();

            if (!name.isEmpty() && !qty.isEmpty()) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("itemName", name);
                map.put("itemType", type);
                map.put("quantity", qty);
                map.put("expiryDate", expiry);
                map.put("timestamp", System.currentTimeMillis());

                String itemId = (existingId != null) ? existingId : mDatabase.push().getKey();

                if (itemId != null) {
                    mDatabase.child(itemId).setValue(map).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), existingId == null ? "Item Added!" : "Item Updated!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(getContext(), "Failed to save item", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                Toast.makeText(getContext(), "Name and Quantity are required", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void setupBottomNavigation(View view) {
        LinearLayout navHome = view.findViewById(R.id.nav_home);
        LinearLayout navInventory = view.findViewById(R.id.nav_inventory);
        LinearLayout navAlerts = view.findViewById(R.id.nav_alerts);
        LinearLayout navProfile = view.findViewById(R.id.nav_profile);

        // --- Make the Bottom Nav Visible & Highlight 'Inventory' ---
        int activeColor = Color.parseColor("#155A91"); // Blue
        int inactiveColor = Color.parseColor("#8E8E8E"); // Grey

        if (navHome != null) {
            ((ImageView) navHome.getChildAt(0)).setColorFilter(inactiveColor);
            ((TextView) navHome.getChildAt(1)).setTextColor(inactiveColor);
            navHome.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_inventoryFragment_to_adminDashboardFragment));
        }

        if (navInventory != null) {
            ((ImageView) navInventory.getChildAt(0)).setColorFilter(activeColor);
            ((TextView) navInventory.getChildAt(1)).setTextColor(activeColor);
            navInventory.setOnClickListener(v -> {}); // Already on Inventory
        }

        if (navAlerts != null) {
            View alertsIcon = ((android.widget.FrameLayout) navAlerts.getChildAt(0)).getChildAt(0);
            if (alertsIcon instanceof ImageView) ((ImageView) alertsIcon).setColorFilter(inactiveColor);
            ((TextView) navAlerts.getChildAt(1)).setTextColor(inactiveColor);
            // FIXED ID: Uses unique Admin Alerts action
            navAlerts.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_inventoryFragment_to_adminAlertsFragment));
        }

        if (navProfile != null) {
            ((ImageView) navProfile.getChildAt(0)).setColorFilter(inactiveColor);
            ((TextView) navProfile.getChildAt(1)).setTextColor(inactiveColor);
            // FIXED ID: Uses unique Admin Profile action
            navProfile.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_inventoryFragment_to_adminProfileFragment));
        }
    }

    // Helper class to store data temporarily for filtering
    private class InventoryItem {
        String id, name, type, qty, expiry;

        InventoryItem(String id, String name, String type, String qty, String expiry) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.qty = qty;
            this.expiry = expiry;
        }
    }
}