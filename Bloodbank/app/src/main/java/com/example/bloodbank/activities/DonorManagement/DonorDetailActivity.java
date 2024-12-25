package com.example.bloodbank.activities.DonorManagement;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;

import com.bumptech.glide.Glide;
import com.example.bloodbank.R;
import com.example.bloodbank.handler.BaseActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DonorDetailActivity extends BaseActivity {
    private static final String TAG = "DonorDetailActivity";
    private FirebaseFirestore db;
    private LinearLayout donorListLayout;
    private List<DocumentSnapshot> donors;
    private String siteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donor_detail);

        donorListLayout = findViewById(R.id.donorListLayout);
        SearchView searchView = findViewById(R.id.searchView);
        ImageButton backButton = findViewById(R.id.backButton);
        TextView downloadReportButton = findViewById(R.id.downloadReportLabel);

        backButton.setOnClickListener(v -> finish());

        String userRole = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getString("USER_ROLE", "donor");
        if ("admin".equals(userRole)) {
            downloadReportButton.setVisibility(View.VISIBLE);
            downloadReportButton.setOnClickListener(v -> downloadReport());
        } else {
            downloadReportButton.setVisibility(View.GONE);
        }

        siteId = getIntent().getStringExtra("SITE_ID");
        String siteName = getIntent().getStringExtra("SITE_NAME");

        TextView pageTitle = findViewById(R.id.pageTitle);
        pageTitle.setText("Donor List in " + siteName);

        db = FirebaseFirestore.getInstance();
        donors = new ArrayList<>();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterDonors(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterDonors(newText);
                return true;
            }
        });

        fetchDonors();
    }

    private void fetchDonors() {
        db.collection("Donors").whereEqualTo("campaignId", siteId).get().addOnSuccessListener(querySnapshot -> {
            donors.clear();
            donors.addAll(querySnapshot.getDocuments());
            displayDonors(donors);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching donors", e);
            Toast.makeText(this, "Error fetching donors", Toast.LENGTH_SHORT).show();
        });
    }

    private void displayDonors(List<DocumentSnapshot> donors) {
        donorListLayout.removeAllViews();
        for (DocumentSnapshot donor : donors) {
            View donorCard = getLayoutInflater().inflate(R.layout.donor_card, donorListLayout, false);

            ImageView donorProfileImage = donorCard.findViewById(R.id.donorImage);
            TextView donorName = donorCard.findViewById(R.id.donorName);
            TextView donorAge = donorCard.findViewById(R.id.donorAge);
            TextView donorLocation = donorCard.findViewById(R.id.donorLocation);
            TextView donorBloodType = donorCard.findViewById(R.id.donorBloodType);
            TextView donorBloodUnit = donorCard.findViewById(R.id.donorBloodUnit);

            donorName.setText(donor.getString("name"));

            String dob = donor.getString("dob");
            donorAge.setText("Age: " + (dob != null ? calculateAge(dob) : "N/A"));

            donorLocation.setText(donor.getString("location"));
            donorBloodType.setText(donor.getString("bloodType"));

            Long bloodUnit = donor.getLong("bloodUnit");
            donorBloodUnit.setText("Blood Units (mL): " + (bloodUnit != null ? bloodUnit.toString() : "N/A"));

            String profileImageUrl = donor.getString("profileImage");
            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                Glide.with(this).load(profileImageUrl).placeholder(R.drawable.ic_placeholder).into(donorProfileImage);
            } else {
                donorProfileImage.setImageResource(R.drawable.ic_placeholder);
            }

            donorCard.setOnClickListener(v -> openDonorInfo(donor));

            donorListLayout.addView(donorCard);
        }
    }

    private void openDonorInfo(DocumentSnapshot donor) {
        String userId = donor.getString("userId");
        if (userId != null && !userId.isEmpty()) {
            db.collection("Users").document(userId).get().addOnSuccessListener(userSnapshot -> {
                if (userSnapshot.exists()) {
                    String email = userSnapshot.getString("email");
                    Intent intent = new Intent(this, DonorInfoActivity.class);
                    intent.putExtra("name", donor.getString("name"));
                    intent.putExtra("email", email != null ? email : "N/A");
                    intent.putExtra("bloodType", donor.getString("bloodType"));
                    intent.putExtra("bloodUnit", donor.getLong("bloodUnit") != null ? donor.getLong("bloodUnit").toString() : "N/A");
                    intent.putExtra("dob", donor.getString("dob"));
                    intent.putExtra("location", donor.getString("location"));
                    intent.putExtra("userId", donor.getString("userId"));
                    intent.putExtra("profileImage", donor.getString("profileImage"));
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to fetch user data", e);
                Toast.makeText(this, "Failed to fetch user data", Toast.LENGTH_SHORT).show();
            });
        } else {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadReport() {
        db.collection("Donors").get().addOnSuccessListener(donorSnapshot -> {
            long totalBloodUnits = 0;
            StringBuilder donorReport = new StringBuilder();

            for (DocumentSnapshot donor : donorSnapshot.getDocuments()) {
                String name = donor.getString("name");
                String bloodType = donor.getString("bloodType");
                long bloodUnits = donor.getLong("bloodUnit") != null ? donor.getLong("bloodUnit") : 0;
                String location = donor.getString("location");

                totalBloodUnits += bloodUnits;

                donorReport.append("Name: ").append(name).append("\n").append("Blood Type: ").append(bloodType).append("\n").append("Blood Units (mL): ").append(bloodUnits).append("\n").append("Location: ").append(location).append("\n\n");
            }

            String finalDonorReport = donorReport.toString();
            long finalTotalBloodUnits = totalBloodUnits;

            db.collection("DonationSites").document(siteId).get().addOnSuccessListener(siteSnapshot -> {
                String siteName = siteSnapshot.getString("siteName");
                String shortName = siteSnapshot.getString("shortName");
                List<String> managerEmails = (List<String>) siteSnapshot.get("managerEmail");
                List<String> managerNames = (List<String>) siteSnapshot.get("managerName");
                String address = siteSnapshot.getString("address");
                List<String> requiredBloodTypes = (List<String>) siteSnapshot.get("requiredBloodTypes");

                String managerEmail = (managerEmails != null && !managerEmails.isEmpty()) ? managerEmails.get(0) : "N/A";
                String managerName = (managerNames != null && !managerNames.isEmpty()) ? managerNames.get(0) : "N/A";
                String requiredBloodTypeString = (requiredBloodTypes != null) ? String.join(", ", requiredBloodTypes) : "N/A";

                StringBuilder report = new StringBuilder();
                report.append("Donation Site Report\n\n").append("Site Name: ").append(siteName).append("\n").append("Short Name: ").append(shortName).append("\n").append("Manager Email: ").append(managerEmail).append("\n").append("Manager Name: ").append(managerName).append("\n").append("Address: ").append(address).append("\n").append("Required Blood Types: ").append(requiredBloodTypeString).append("\n\n").append("Total Blood Units Collected (mL): ").append(finalTotalBloodUnits).append("\n\n").append("Donors:\n").append(finalDonorReport);

                saveReportToFile(report.toString());
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Error fetching site details", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error fetching site details", e);
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error fetching donor data", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error fetching donor data", e);
        });
    }

    private void saveReportToFile(String reportContent) {
        try {
            // Save the file downloads directory
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File reportFile = new File(downloadsDir, "DonationReport.txt");

            FileWriter writer = new FileWriter(reportFile);
            writer.write(reportContent);
            writer.close();

            Toast.makeText(this, "Download report complete", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error saving report", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error saving report", e);
        }
    }

    private String calculateAge(String dob) {
        if (dob == null || dob.isEmpty()) return "N/A";

        try {
            String[] parts = dob.split("/");
            int year = Integer.parseInt(parts[2]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[0]);

            Calendar birthDate = Calendar.getInstance();
            birthDate.set(year, month - 1, day);

            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR);

            if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }

            return String.valueOf(age);
        } catch (Exception e) {
            e.printStackTrace();
            return "N/A";
        }
    }

    private void filterDonors(String query) {
        if (query.isEmpty()) {
            displayDonors(donors);
            return;
        }

        List<DocumentSnapshot> filtered = new ArrayList<>();
        for (DocumentSnapshot donor : donors) {
            if (donor.getString("name").toLowerCase().contains(query.toLowerCase())) {
                filtered.add(donor);
            }
        }
        displayDonors(filtered);
    }
}
