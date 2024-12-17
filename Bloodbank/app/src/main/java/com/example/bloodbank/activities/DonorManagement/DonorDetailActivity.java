package com.example.bloodbank.activities.DonorManagement;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

        siteId = getIntent().getStringExtra("SITE_ID");
        String siteName = getIntent().getStringExtra("SITE_NAME");


        TextView pageTitle = findViewById(R.id.pageTitle);
        pageTitle.setText(siteName);

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
        db.collection("Donors")
                .whereEqualTo("campaignId", siteId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    donors.clear();
                    donors.addAll(querySnapshot.getDocuments());
                    displayDonors(donors);
                })
                .addOnFailureListener(e -> {
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
            donorBloodUnit.setText("Blood Units: " + (bloodUnit != null ? bloodUnit.toString() : "N/A"));

            String profileImageUrl = donor.getString("profileImage");
            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                Glide.with(this).load(profileImageUrl).placeholder(R.drawable.ic_placeholder).into(donorProfileImage);
            } else {
                donorProfileImage.setImageResource(R.drawable.ic_placeholder);
            }

            donorListLayout.addView(donorCard);
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
