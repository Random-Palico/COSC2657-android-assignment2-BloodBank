package com.example.bloodbank.activities.DonorManagement;

import android.content.Intent;
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

public class DonorListActivity extends BaseActivity {
    private static final String TAG = "DonorListActivity";
    private FirebaseFirestore db;
    private LinearLayout siteListLayout;
    private List<DocumentSnapshot> donationSites;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donor_list);

        setupBottomNavigation();

        siteListLayout = findViewById(R.id.siteListLayout);
        SearchView searchView = findViewById(R.id.searchView);
        db = FirebaseFirestore.getInstance();
        donationSites = new ArrayList<>();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterSites(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSites(newText);
                return true;
            }
        });

        fetchDonationSites();
    }

    private void fetchDonationSites() {
        String userRole = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getString("USER_ROLE", "donor");
        String userEmail = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getString("USER_EMAIL", "");

        if ("admin".equals(userRole)) {
            // Admin role - fetch all campaigns
            db.collection("DonationSites").get().addOnSuccessListener(querySnapshot -> {
                donationSites.clear();
                donationSites.addAll(querySnapshot.getDocuments());
                displayDonationSites(donationSites, userRole);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching donation sites", e);
                Toast.makeText(this, "Error fetching sites", Toast.LENGTH_SHORT).show();
            });
        } else if ("manager".equals(userRole)) {
            // Manager role fetch only the campaigns they assigned to
            db.collection("DonationSites").whereArrayContains("managerEmail", userEmail).get().addOnSuccessListener(querySnapshot -> {
                donationSites.clear();
                donationSites.addAll(querySnapshot.getDocuments());
                displayDonationSites(donationSites, userRole);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching assigned donation sites", e);
                Toast.makeText(this, "Error fetching sites", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void displayDonationSites(List<DocumentSnapshot> sites, String userRole) {
        siteListLayout.removeAllViews();

        if (sites.isEmpty()) {
            // Display a message based on the users' role
            TextView noSitesMessage = new TextView(this);
            noSitesMessage.setText("admin".equals(userRole) ? "No campaigns available!" : "You are not assigned to any campaigns.");
            noSitesMessage.setTextSize(16);
            noSitesMessage.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            siteListLayout.addView(noSitesMessage);
            return;
        }

        for (DocumentSnapshot site : sites) {
            String siteName = site.getString("shortName");
            String siteAddress = site.getString("address");
            List<String> bloodTypes = (List<String>) site.get("requiredBloodTypes");

            if (siteName == null || siteAddress == null || bloodTypes == null) continue;

            View siteCard = getLayoutInflater().inflate(R.layout.site_item, siteListLayout, false);

            TextView name = siteCard.findViewById(R.id.siteName);
            TextView address = siteCard.findViewById(R.id.siteAddress);
            TextView details = siteCard.findViewById(R.id.siteDetails);
            TextView campaignDate = siteCard.findViewById(R.id.campaignDate);

            name.setText(siteName);
            address.setText(siteAddress);
            details.setText("Required: " + bloodTypes.toString());

            if (campaignDate != null) {
                campaignDate.setVisibility(View.GONE);
            }

            siteCard.setOnClickListener(v -> openDonorList(site.getId(), siteName));

            siteListLayout.addView(siteCard);
        }
    }

    private void openDonorList(String siteId, String siteName) {
        Intent intent = new Intent(this, DonorDetailActivity.class);
        intent.putExtra("SITE_ID", siteId);
        intent.putExtra("SITE_NAME", siteName);

        startActivity(intent);
    }

    private void filterSites(String query) {
        if (query.isEmpty()) {
            displayDonationSites(donationSites, getSharedPreferences("LoginPrefs", MODE_PRIVATE).getString("USER_ROLE", "donor"));
            return;
        }

        List<DocumentSnapshot> filtered = new ArrayList<>();
        for (DocumentSnapshot site : donationSites) {
            if (site.getString("shortName").toLowerCase().contains(query.toLowerCase())) {
                filtered.add(site);
            }
        }
        displayDonationSites(filtered, getSharedPreferences("LoginPrefs", MODE_PRIVATE).getString("USER_ROLE", "donor"));
    }
}
