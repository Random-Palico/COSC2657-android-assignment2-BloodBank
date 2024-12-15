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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.bloodbank.R;
import com.example.bloodbank.activities.CampaignDetailActivity;
import com.example.bloodbank.handler.BaseActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminMainActivity extends BaseActivity {

    private static final String TAG = "AdminMainActivity";
    private FirebaseFirestore db;
    private LinearLayout campaignList;
    private List<DocumentSnapshot> campaigns = new ArrayList<>();
    private ImageView profileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        db = FirebaseFirestore.getInstance();

        setupBottomNavigation();

        profileImage = findViewById(R.id.profileImage);
        String userName = getIntent().getStringExtra("USER_NAME");
        String role = getIntent().getStringExtra("USER_ROLE");

        if (userName == null || role == null) {
            SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            userName = sharedPreferences.getString("USER_NAME", "User");
            role = sharedPreferences.getString("USER_ROLE", "user");
        }

        // Update the UI dynamically based on the retrieved name and role
        TextView welcomeUser = findViewById(R.id.welcomeUser);
        welcomeUser.setText("Hi " + userName);


        fetchProfileImage();

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

        fetchCampaigns();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchProfileImage();
        fetchCampaigns();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == 100) {
                SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
                String userName = sharedPreferences.getString("USER_NAME", "Admin");
                TextView welcomeUser = findViewById(R.id.welcomeUser);
                welcomeUser.setText("Hi " + userName);
                fetchProfileImage();
            } else if (requestCode == 200 || requestCode == 300) {
                fetchCampaigns();
                Toast.makeText(this, "Campaign updated successfully!", Toast.LENGTH_SHORT).show();
            }
        }
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

                        Log.d(TAG, "Profile Image URL: " + profileImageUrl);

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
        campaignList.removeAllViews();
        for (DocumentSnapshot document : campaignDocuments) {
            String title = document.getString("siteName");
            String location = document.getString("shortName");
            String eventDateStr = document.getString("eventDate");
            String eventImg = document.getString("eventImg");

            String address = document.getString("address");

            if (title == null || eventDateStr == null || location == null || eventImg == null) {
                Log.e(TAG, "Missing data in Firestore document: " + document.getId());
                continue;
            }

            addCampaignCard(campaignList, document, title, eventDateStr, location, eventImg, address);
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

    private void addCampaignCard(LinearLayout campaignList, DocumentSnapshot document, String title, String date, String location, String eventImg, String address) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View cardView = inflater.inflate(R.layout.campaign_card, campaignList, false);

        TextView campaignTitle = cardView.findViewById(R.id.campaignTitle);
        TextView campaignDate = cardView.findViewById(R.id.campaignDate);
        TextView campaignLocation = cardView.findViewById(R.id.campaignLocation);
        ImageView campaignImage = cardView.findViewById(R.id.campaignImage);
        Button editButton = cardView.findViewById(R.id.editButton);
        Button assignButton = cardView.findViewById(R.id.assignButton);

        campaignTitle.setText(title);
        campaignDate.setText(date);
        campaignLocation.setText(location);

        Glide.with(this).load(eventImg).into(campaignImage);

        Button registerButton = cardView.findViewById(R.id.registerButton);
        registerButton.setVisibility(View.GONE);

        editButton.setVisibility(View.VISIBLE);
        editButton.setText("Edit");

        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditCampaignActivity.class);
            intent.putExtra("campaignId", document.getId());
            intent.putExtra("campaignTitle", title);
            intent.putExtra("campaignDescription", document.getString("description"));
            intent.putExtra("campaignDate", document.getString("eventDate"));
            intent.putExtra("campaignImage", eventImg);
            intent.putExtra("campaignLocation", location);
            intent.putExtra("campaignAddress", address);

            Map<String, Object> locationLatLng = (Map<String, Object>) document.get("locationLatLng");
            if (locationLatLng != null) {
                double lat = (double) locationLatLng.get("lat");
                double lng = (double) locationLatLng.get("lng");
                intent.putExtra("latitude", lat);
                intent.putExtra("longitude", lng);
            }

            ArrayList<String> bloodTypes = (ArrayList<String>) document.get("requiredBloodTypes");
            intent.putStringArrayListExtra("requiredBloodTypes", bloodTypes);

            startActivityForResult(intent, 200);
        });

        assignButton.setVisibility(View.VISIBLE);
        assignButton.setText("Assign");
        assignButton.setOnClickListener(v -> showAssignPopup(document.getId(), title));

        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(this, CampaignDetailActivity.class);
            intent.putExtra("campaignId", document.getId());
            intent.putExtra("campaignTitle", title);
            intent.putExtra("campaignDescription", document.getString("description"));
            intent.putExtra("campaignDate", document.getString("eventDate"));
            intent.putExtra("campaignImage", eventImg);
            intent.putExtra("campaignLocation", location);
            intent.putExtra("campaignAddress", address);

            Map<String, Object> locationLatLng = (Map<String, Object>) document.get("locationLatLng");
            if (locationLatLng != null) {
                double lat = (double) locationLatLng.get("lat");
                double lng = (double) locationLatLng.get("lng");
                intent.putExtra("latitude", lat);
                intent.putExtra("longitude", lng);
            }

            ArrayList<String> bloodTypes = (ArrayList<String>) document.get("requiredBloodTypes");
            intent.putStringArrayListExtra("requiredBloodTypes", bloodTypes);

            // Retrieve role from SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            String role = sharedPreferences.getString("USER_ROLE", "donor");
            intent.putExtra("USER_ROLE", role);

            startActivity(intent);
        });


        campaignList.addView(cardView);
    }


    private void showAssignPopup(String campaignId, String campaignTitle) {
        FirebaseFirestore.getInstance()
                .collection("Users")
                .whereEqualTo("role", "manager")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No managers available", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<DocumentSnapshot> managers = querySnapshot.getDocuments();
                    String[] managerNames = new String[managers.size()];
                    String[] managerEmails = new String[managers.size()];

                    for (int i = 0; i < managers.size(); i++) {
                        managerNames[i] = managers.get(i).getString("name");
                        managerEmails[i] = managers.get(i).getString("email");
                    }

                    showManagerSelectionDialog(campaignId, campaignTitle, managerNames, managerEmails);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching managers", Toast.LENGTH_SHORT).show());
    }

    private void showManagerSelectionDialog(String campaignId, String campaignTitle, String[] managerNames, String[] managerEmails) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Assign Manager to " + campaignTitle);

        builder.setItems(managerNames, (dialog, which) -> {
            String selectedManagerName = managerNames[which];
            String selectedManagerEmail = managerEmails[which];

            assignManagerToCampaign(campaignId, selectedManagerName, selectedManagerEmail);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void assignManagerToCampaign(String campaignId, String managerName, String managerEmail) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("managerName", managerName);
        updateData.put("managerEmail", managerEmail);

        FirebaseFirestore.getInstance()
                .collection("DonationSites")
                .document(campaignId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Manager assigned successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error assigning manager", Toast.LENGTH_SHORT).show());
    }
}
