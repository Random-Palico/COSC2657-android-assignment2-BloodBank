package com.example.bloodbank.activities.home.manager;

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
import com.example.bloodbank.activities.DonorRegisterActivity;
import com.example.bloodbank.activities.NotificationActivity;
import com.example.bloodbank.activities.add_edit_campaign.EditCampaignActivity;
import com.example.bloodbank.handler.BaseActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ManagerMainActivity extends BaseActivity {

    private static final String TAG = "ManagerMainActivity";
    private FirebaseFirestore db;
    private LinearLayout campaignList;
    private List<DocumentSnapshot> campaigns = new ArrayList<>();
    private ImageView profileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_main);

        db = FirebaseFirestore.getInstance();
        setupBottomNavigation();

        initializeViews();

        fetchCampaigns();

        ImageView notificationButton = findViewById(R.id.notificationButton);
        checkForUnreadNotifications(notificationButton);

        notificationButton.setOnClickListener(v -> {
            SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            String userId = sharedPreferences.getString("USER_ID", null);

            if (userId == null) {
                Toast.makeText(this, "User ID is missing!", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(ManagerMainActivity.this, NotificationActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("RECEIVER_IDS", new String[]{"all", "adminManager", userId});
            intent.putExtra("USER_ROLE", "manager");
            startActivity(intent);
        });
    }

    private void initializeViews() {
        TextView welcomeUser = findViewById(R.id.welcomeUser);
        profileImage = findViewById(R.id.profileImage);
        campaignList = findViewById(R.id.campaignList);

        String userName = getIntent().getStringExtra("USER_NAME");
        if (userName == null) {
            userName = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getString("USER_NAME", "Manager");
        }
        welcomeUser.setText(userName != null ? "Hi " + userName : "Hi Manager");

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

    private void checkForUnreadNotifications(ImageView notificationButton) {
        String userId = getIntent().getStringExtra("USER_ID");
        if (userId == null) {
            Log.e(TAG, "User ID is null. Ensure it is passed in the intent.");
            return;
        }

        List<String> validReceiverIds = new ArrayList<>();
        validReceiverIds.add("all");
        validReceiverIds.add("adminManager");

        db.collection("Notifications")
                .whereIn("receiverId", validReceiverIds)
                .whereEqualTo("status", "unread")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Failed to listen for notifications: " + e.getMessage());
                        return;
                    }

                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        notificationButton.setColorFilter(getResources().getColor(R.color.red), android.graphics.PorterDuff.Mode.SRC_IN);
                    } else {
                        notificationButton.setColorFilter(getResources().getColor(R.color.gray), android.graphics.PorterDuff.Mode.SRC_IN);
                    }
                });
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
                });
    }

    private void fetchCampaigns() {
        Log.d(TAG, "Fetching campaigns...");
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
        String address = document.getString("address");

        // Register button
        registerButton.setVisibility(View.VISIBLE);
        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, DonorRegisterActivity.class);
            intent.putExtra("campaignId", document.getId());
            intent.putExtra("campaignTitle", title);
            intent.putExtra("campaignDate", date);
            intent.putExtra("campaignLocation", location);
            intent.putExtra("campaignAddress", address);
            intent.putExtra("campaignImage", eventImg);

            SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            String userName = sharedPreferences.getString("USER_NAME", "");
            String userBloodType = sharedPreferences.getString("USER_BLOOD_TYPE", "");
            String userLocation = sharedPreferences.getString("USER_LOCATION", "");

            intent.putExtra("userName", userName);
            intent.putExtra("userBloodType", userBloodType);
            intent.putExtra("userLocation", userLocation);

            startActivity(intent);
        });

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

        // Fetch manager data
        Object managerNamesObj = document.get("managerName");
        Object managerEmailsObj = document.get("managerEmail");

        List<String> managerNames = managerNamesObj instanceof List ? (List<String>) managerNamesObj : new ArrayList<>();
        List<String> managerEmails = managerEmailsObj instanceof List ? (List<String>) managerEmailsObj : new ArrayList<>();

        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String currentManagerEmail = sharedPreferences.getString("USER_EMAIL", "");
        String currentManagerName = sharedPreferences.getString("USER_NAME", "Manager");

        boolean isManagerAssigned = managerEmails.contains(currentManagerEmail);

        // Update button text
        assignButton.setText(isManagerAssigned ? "Unassign" : "Assign");

        assignButton.setVisibility(View.VISIBLE);
        assignButton.setOnClickListener(v -> {
            if (isManagerAssigned) {
                showUnassignConfirmation(document, new ArrayList<>(managerNames), new ArrayList<>(managerEmails), currentManagerEmail, currentManagerName);
            } else if (managerEmails.size() < 2) {
                showAssignConfirmation(document, new ArrayList<>(managerNames), new ArrayList<>(managerEmails), currentManagerEmail, currentManagerName);
            } else {
                Toast.makeText(this, "Maximum of 2 managers allowed for this campaign!", Toast.LENGTH_SHORT).show();
            }
        });
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

            String role = sharedPreferences.getString("USER_ROLE", "donor");
            intent.putExtra("USER_ROLE", role);

            startActivity(intent);
        });
        campaignList.addView(cardView);
    }

    private void showAssignConfirmation(DocumentSnapshot document, List<String> managerNames, List<String> managerEmails, String managerEmail, String managerName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Assign Task")
                .setMessage("Are you sure you want to assign yourself to this campaign?")
                .setPositiveButton("Yes", (dialog, which) -> assignManagerToCampaign(document, managerNames, managerEmails, managerEmail, managerName))
                .setNegativeButton("No", null)
                .show();
    }

    private void assignManagerToCampaign(DocumentSnapshot document, List<String> managerNames, List<String> managerEmails, String managerEmail, String managerName) {
        managerNames.add(managerName);
        managerEmails.add(managerEmail);

        db.collection("DonationSites").document(document.getId())
                .update("managerName", managerNames, "managerEmail", managerEmails)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Assigned successfully!", Toast.LENGTH_SHORT).show();
                    fetchCampaigns(); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to assign: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error assigning manager", e);
                });
    }

    private void showUnassignConfirmation(DocumentSnapshot document, List<String> managerNames, List<String> managerEmails, String managerEmail, String managerName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Unassign Task")
                .setMessage("Are you sure you want to unassign yourself from this campaign?")
                .setPositiveButton("Yes", (dialog, which) -> unassignManagerFromCampaign(document, managerNames, managerEmails, managerEmail, managerName))
                .setNegativeButton("No", null)
                .show();
    }

    private void unassignManagerFromCampaign(DocumentSnapshot document, List<String> managerNames, List<String> managerEmails, String managerEmail, String managerName) {
        managerNames.remove(managerName);
        managerEmails.remove(managerEmail);

        db.collection("DonationSites").document(document.getId())
                .update("managerName", managerNames, "managerEmail", managerEmails)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Unassigned successfully!", Toast.LENGTH_SHORT).show();
                    fetchCampaigns();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to unassign: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error unassigning manager", e);
                });
    }
}
