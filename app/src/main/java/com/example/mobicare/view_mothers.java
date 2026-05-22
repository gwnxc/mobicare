package com.example.mobicare;

import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

// iText Imports for PDF
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class view_mothers extends Fragment {

    private DatabaseReference mDatabaseGuardians;
    private DatabaseReference mDatabaseChildren;

    private LinearLayout llPatientsList;
    private TextView tvCount;
    private EditText etSearchPatient;
    private MaterialButtonToggleGroup toggleFilter;

    // Master lists to hold raw data
    private List<PatientItem> guardianList = new ArrayList<>();
    private List<PatientItem> childrenList = new ArrayList<>();

    // The list that is actually displayed and printed
    private List<PatientItem> filteredList = new ArrayList<>();

    private String currentSearchQuery = "";
    private String currentFilterType = "All"; // "All", "Guardians", "Children"

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_view_mothers, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize Database References based on your new structure
        mDatabaseGuardians = FirebaseDatabase.getInstance().getReference("Patients_Guardians");
        mDatabaseChildren = FirebaseDatabase.getInstance().getReference("Patients_Children");

        // 2. UI Setup
        ImageView btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        llPatientsList = view.findViewById(R.id.llPatientsList);
        tvCount = view.findViewById(R.id.tvCount);
        etSearchPatient = view.findViewById(R.id.etSearchPatient);
        toggleFilter = view.findViewById(R.id.toggleFilter);
        FloatingActionButton btnExportPdf = view.findViewById(R.id.btnExportPdf);

        // 3. Setup Listeners for Search and Filter
        setupInteractions();

        // 4. Setup PDF Export
        if (btnExportPdf != null) {
            btnExportPdf.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Generating Report...", Toast.LENGTH_SHORT).show();
                generatePdfReport();
            });
        }

        // 5. Fetch Data
        fetchData();
    }

    private void setupInteractions() {
        // Search Bar Listener
        if (etSearchPatient != null) {
            etSearchPatient.addTextChangedListener(new TextWatcher() {
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
                    else if (checkedId == R.id.btnFilterGuardians) currentFilterType = "Guardians";
                    else if (checkedId == R.id.btnFilterChildren) currentFilterType = "Children";
                    applyFilters();
                }
            });
        }
    }

    private void fetchData() {
        // Fetch Guardians
        mDatabaseGuardians.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                guardianList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String name = snap.child("fullName").getValue(String.class);
                    if (name != null) {
                        String phone = snap.child("phone").getValue(String.class);
                        String address = snap.child("address").getValue(String.class);
                        String age = snap.child("age").getValue(String.class);

                        guardianList.add(new PatientItem(
                                name, "Guardian",
                                (phone != null ? "📞 " + phone : "📞 N/A"),
                                (address != null ? "📍 " + address : "📍 N/A")
                        ));
                    }
                }
                applyFilters();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Fetch Children
        mDatabaseChildren.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                childrenList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String firstName = snap.child("firstName").getValue(String.class);
                    String lastName = snap.child("lastName").getValue(String.class);

                    if (firstName != null && lastName != null) {
                        String fullName = firstName + " " + lastName;
                        String parent = snap.child("parentGuardian").getValue(String.class);
                        String dob = snap.child("birthDate").getValue(String.class);

                        childrenList.add(new PatientItem(
                                fullName, "Child",
                                (parent != null ? "👩‍👧 Parent: " + parent : "Parent: N/A"),
                                (dob != null ? "🎂 Born: " + dob : "Born: N/A")
                        ));
                    }
                }
                applyFilters();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void applyFilters() {
        filteredList.clear();

        // Combine lists
        List<PatientItem> masterList = new ArrayList<>();
        masterList.addAll(guardianList);
        masterList.addAll(childrenList);

        for (PatientItem item : masterList) {
            // 1. Check Category Filter
            boolean matchesType = currentFilterType.equals("All") ||
                    (currentFilterType.equals("Guardians") && item.type.equals("Guardian")) ||
                    (currentFilterType.equals("Children") && item.type.equals("Child"));

            // 2. Check Search Query
            boolean matchesSearch = item.name.toLowerCase().contains(currentSearchQuery);

            if (matchesType && matchesSearch) {
                filteredList.add(item);
            }
        }

        updateUI();
    }

    private void updateUI() {
        if (llPatientsList == null) return;
        llPatientsList.removeAllViews();

        for (PatientItem item : filteredList) {
            // Reusing your item_mother_card layout, but mapping the dynamic data to it
            View card = getLayoutInflater().inflate(R.layout.item_mother_card, llPatientsList, false);

            TextView tvName = card.findViewById(R.id.tvMotherName);
            TextView tvInfo1 = card.findViewById(R.id.tvPhone);
            TextView tvInfo2 = card.findViewById(R.id.tvAddress);
            TextView tvAge = card.findViewById(R.id.tvAge); // We'll repurpose this for the Type badge

            tvName.setText(item.name);
            tvInfo1.setText(item.info1);
            tvInfo2.setText(item.info2);

            tvAge.setText(item.type);
            // Optional: You can style the type badge differently for Guardians vs Children here
            if (item.type.equals("Child")) {
                tvAge.setTextColor(android.graphics.Color.rgb(0x00, 0x4D, 0xAA)); // Blue for child
            } else {
                tvAge.setTextColor(android.graphics.Color.rgb(0x1B, 0x5E, 0x20)); // Green for guardian
            }

            llPatientsList.addView(card);
        }

        if (tvCount != null) {
            tvCount.setText(filteredList.size() + " records found");
        }
    }

    private void generatePdfReport() {
        try {
            // 1. Setup the File Path (Downloads folder)
            String pdfPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();

            // Create a dynamic filename based on the current filter
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File file = new File(pdfPath, currentFilterType + "_Report_" + timeStamp + ".pdf");

            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // 2. Add Title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Mobicare Directory: " + currentFilterType + "\n\n", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            // 3. Create a Table with 3 Columns
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4f, 2f, 4f}); // Column width ratios

            // Table Headers
            addTableHeader(table, "Full Name");
            addTableHeader(table, "Type");
            addTableHeader(table, "Key Details");

            // 4. Populate table using ONLY the filtered list
            for (PatientItem item : filteredList) {
                table.addCell(new PdfPCell(new Phrase(item.name)));
                table.addCell(new PdfPCell(new Phrase(item.type)));

                // Combine the info lines for the table layout
                String combinedDetails = item.info1.replace("📞 ", "").replace("👩‍👧 ", "")
                        + "\n" + item.info2.replace("📍 ", "").replace("🎂 ", "");
                table.addCell(new PdfPCell(new Phrase(combinedDetails)));
            }

            document.add(table);
            document.close();

            Toast.makeText(getContext(), "Report Saved to Downloads!", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addTableHeader(PdfPTable table, String headerTitle) {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        PdfPCell header = new PdfPCell(new Phrase(headerTitle, headerFont));
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.setPaddingBottom(8f);
        header.setBackgroundColor(new com.itextpdf.text.BaseColor(240, 240, 240)); // Light gray header
        table.addCell(header);
    }

    // Helper class to unify Guardian and Child data
    private class PatientItem {
        String name;
        String type; // "Guardian" or "Child"
        String info1; // Phone or Parent
        String info2; // Address or DOB

        PatientItem(String name, String type, String info1, String info2) {
            this.name = name;
            this.type = type;
            this.info1 = info1;
            this.info2 = info2;
        }
    }
}