package com.example.bloodbank.activities;

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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.bloodbank.R;
import com.example.bloodbank.handler.BaseActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class DonorMainActivity extends BaseActivity {

    private static final String TAG = "DonorMainActivity";
    private FirebaseFirestore db;
    private LinearLayout campaignList;
    private List<DocumentSnapshot> campaigns = new ArrayList<>();
    private ImageView profileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_donor);

        db = FirebaseFirestore.getInstance();

        setupBottomNavigation();

        initializeViews();

        fetchCampaigns();
    }

    private void initializeViews() {
        // Get user name and role from intent
        TextView welcomeUser = findViewById(R.id.welcomeUser);
        profileImage = findViewById(R.id.profileImage);
        campaignList = findViewById(R.id.campaignList);

        String userName = getIntent().getStringExtra("USER_NAME");
        String role = getIntent().getStringExtra("USER_ROLE");

        welcomeUser.setText(userName != null ? "Hi " + userName : "Hi Donor");

        fetchProfileImage();

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchProfileImage();
        fetchCampaigns();
    }

    private void fetchProfileImage() {
        String userEmail = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getString("USER_EMAIL", null);
        if (userEmail == null) {
            Log.e(TAG, "User email not found in SharedPreferences.");
            profileImage.setImageResource(R.drawable.ic_placeholder);
            return;
        }

        db.collection("Users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        String profileImageUrl = document.getString("profileImage");

                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profileImageUrl)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .skipMemoryCache(true)
                                    .placeholder(R.drawable.ic_placeholder)
                                    .error(R.drawable.ic_placeholder)
                                    .into(profileImage);
                        } else {
                            profileImage.setImageResource(R.drawable.ic_placeholder);
                        }
                    } else {
                        Log.e(TAG, "No user found with the given email.");
                        profileImage.setImageResource(R.drawable.ic_placeholder);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user profile: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to load profile image", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchCampaigns() {
        Log.d(TAG, "Fetching campaigns...");
        if (campaignList == null) {
            Log.e(TAG, "Campaign list layout is null. Make sure the layout ID is correct.");
            return;
        }

        db.collection("DonationSites").get().addOnSuccessListener(querySnapshot -> {
            if (querySnapshot.isEmpty()) {
                Toast.makeText(this, "No campaigns available!", Toast.LENGTH_SHORT).show();
                return;
            }

            campaigns.clear();
            campaigns.addAll(querySnapshot.getDocuments());
            displayCampaigns(campaigns);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching campaigns", e);
            Toast.makeText(this, "Error fetching campaigns: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void displayCampaigns(List<DocumentSnapshot> campaignDocuments) {
        if (campaignList == null) {
            Log.e(TAG, "Campaign list layout is null. Ensure layout ID matches.");
            return;
        }

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

            addCampaignCard(document, title, eventDateStr, location, eventImg);
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

    private void addCampaignCard(DocumentSnapshot document, String title, String date, String location, String eventImg) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View cardView = inflater.inflate(R.layout.campaign_card, campaignList, false);

        TextView campaignTitle = cardView.findViewById(R.id.campaignTitle);
        TextView campaignDate = cardView.findViewById(R.id.campaignDate);
        TextView campaignLocation = cardView.findViewById(R.id.campaignLocation);
        ImageView campaignImage = cardView.findViewById(R.id.campaignImage);
        Button registerButton = cardView.findViewById(R.id.registerButton);
        Button editButton = cardView.findViewById(R.id.editButton);
        Button assignButton = cardView.findViewById(R.id.assignButton);

        campaignTitle.setText(title);
        campaignDate.setText(date);
        campaignLocation.setText(location);

        Glide.with(this).load(eventImg).into(campaignImage);

        // Register button visible for donors
        registerButton.setVisibility(View.VISIBLE);
        registerButton.setOnClickListener(v -> Toast.makeText(this, "Registered for " + title, Toast.LENGTH_SHORT).show());

        // Hide the edit and assign buttons - Admin functions
        editButton.setVisibility(View.GONE);
        assignButton.setVisibility(View.GONE);

        campaignList.addView(cardView);
    }
}



