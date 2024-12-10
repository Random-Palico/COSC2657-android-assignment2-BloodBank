package com.example.bloodbank.activities.admin_super;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;

import com.bumptech.glide.Glide;
import com.example.bloodbank.R;
import com.example.bloodbank.handler.BaseActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminMainActivity extends BaseActivity {

    private static final String TAG = "AdminMainActivity";
    private FirebaseFirestore db;
    private LinearLayout campaignList;
    private List<DocumentSnapshot> campaigns = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        setupBottomNavigation();

        TextView welcomeUser = findViewById(R.id.welcomeUser);
        String userName = getIntent().getStringExtra("USER_NAME");
        String role = getIntent().getStringExtra("USER_ROLE");

        if (userName == null || role == null) {
            SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            userName = sharedPreferences.getString("USER_NAME", "Admin");
            role = sharedPreferences.getString("USER_ROLE", "admin");
        }

        welcomeUser.setText(userName != null ? "Hi " + userName : "Hi Admin");

        Button addCampaignButton = findViewById(R.id.addCampaignButton);
        String finalRole = role;
        addCampaignButton.setOnClickListener(v -> {
            if (!"admin".equals(finalRole)) {
                Toast.makeText(this, "You don't have permission to add campaigns!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, AddCampaignActivity.class);
            intent.putExtra("USER_ROLE", finalRole);
            startActivityForResult(intent, 100);
        });

        campaignList = findViewById(R.id.campaignList);
        SearchView searchView = findViewById(R.id.searchView);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterCampaigns(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCampaigns(newText);
                return true;
            }
        });

        db = FirebaseFirestore.getInstance();
        fetchCampaigns();
    }

    // Handle result from AddCampaignActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            fetchCampaigns();
        }
    }


    private void fetchCampaigns() {
        db.collection("DonationSites")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No campaigns available!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    campaigns.clear();
                    campaigns.addAll(querySnapshot.getDocuments());
                    displayCampaigns(campaigns);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching campaigns", e);
                    Toast.makeText(this, "Error fetching campaigns: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void displayCampaigns(List<DocumentSnapshot> campaignDocuments) {
        campaignList.removeAllViews();
        for (DocumentSnapshot document : campaignDocuments) {
            String title = document.getString("siteName");
            String location = document.getString("shortName");
            String eventDateStr = document.getString("eventDate");
            String eventImg = document.getString("eventImg");

            if (title == null || eventDateStr == null || location == null || eventImg == null) {
                Log.e(TAG, "Missing data in Firestore document: " + document.getId());
                continue;
            }

            String formattedDate = eventDateStr;

            addCampaignCard(campaignList, title, formattedDate, location, eventImg);
        }
    }

    private void filterCampaigns(String query) {
        if (TextUtils.isEmpty(query)) {
            displayCampaigns(campaigns);
            return;
        }

        List<DocumentSnapshot> filteredCampaigns = new ArrayList<>();
        for (DocumentSnapshot document : campaigns) {
            String title = document.getString("siteName");
            if (title != null && title.toLowerCase().contains(query.toLowerCase())) {
                filteredCampaigns.add(document);
            }
        }

        displayCampaigns(filteredCampaigns);
    }

    private void addCampaignCard(LinearLayout campaignList, String title, String date, String location, String eventImg) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View cardView = inflater.inflate(R.layout.campaign_card, campaignList, false);

        TextView campaignTitle = cardView.findViewById(R.id.campaignTitle);
        TextView campaignDate = cardView.findViewById(R.id.campaignDate);
        TextView campaignLocation = cardView.findViewById(R.id.campaignLocation);
        ImageView campaignImage = cardView.findViewById(R.id.campaignImage);
        Button editButton = cardView.findViewById(R.id.editButton);

        campaignTitle.setText(title);
        campaignDate.setText(date);
        campaignLocation.setText(location);

        Glide.with(this)
                .load(eventImg)
                .into(campaignImage);

        Button registerButton = cardView.findViewById(R.id.registerButton);
        registerButton.setVisibility(View.GONE);

        editButton.setVisibility(View.VISIBLE);
        editButton.setText("Edit");
        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditCampaignActivity.class);
            intent.putExtra("campaignTitle", title);
            intent.putExtra("campaignDate", date);
            intent.putExtra("campaignLocation", location);
            intent.putExtra("campaignImage", eventImg);
            startActivity(intent);
        });

        campaignList.addView(cardView);
    }
}
