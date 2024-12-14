package com.example.bloodbank.activities.DonorManagement;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;

import com.example.bloodbank.R;
import com.example.bloodbank.handler.BaseActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
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
                .whereEqualTo("siteId", siteId)
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
            String name = donor.getString("name");
            String age = donor.getString("age");
            String location = donor.getString("location");
            String bloodType = donor.getString("bloodType");

            if (name == null || age == null || location == null || bloodType == null) continue;

            View donorCard = getLayoutInflater().inflate(R.layout.donor_card, donorListLayout, false);

            TextView donorName = donorCard.findViewById(R.id.donorName);
            TextView donorAge = donorCard.findViewById(R.id.donorAge);
            TextView donorLocation = donorCard.findViewById(R.id.donorLocation);
            TextView donorBloodType = donorCard.findViewById(R.id.donorBloodType);

            donorName.setText(name);
            donorAge.setText(age + " years old");
            donorLocation.setText(location);
            donorBloodType.setText(bloodType);

            donorListLayout.addView(donorCard);
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
