package com.example.bloodbank.activities.home.admin_super;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.bloodbank.R;
import com.example.bloodbank.activities.CampaignDetailActivity;
import com.example.bloodbank.activities.NotificationActivity;
import com.example.bloodbank.activities.add_edit_campaign.AddCampaignActivity;
import com.example.bloodbank.activities.add_edit_campaign.EditCampaignActivity;
import com.example.bloodbank.handler.BaseActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminMainActivity extends BaseActivity {

    private static final String TAG = "AdminMainActivity";
    private FirebaseFirestore db;
    private LinearLayout campaignList;
    private List<DocumentSnapshot> campaigns = new ArrayList<>();
    private ImageView profileImage;
    private String currentUserId;
    private String currentUserRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        db = FirebaseFirestore.getInstance();
        setupBottomNavigation();

        profileImage = findViewById(R.id.profileImage);

        // Retrieve user details from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        currentUserId = sharedPreferences.getString("USER_ID", null);
        currentUserRole = sharedPreferences.getString("USER_ROLE", "user");

        if (currentUserId == null) {
            Toast.makeText(this, "Unable to fetch user details. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ImageView notificationButton = findViewById(R.id.notificationButton);
        checkForUnreadNotifications(notificationButton);


        final String EXTRA_RECEIVER_IDS = "RECEIVER_IDS";
        final String EXTRA_USER_ROLE = "USER_ROLE";

        notificationButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminMainActivity.this, NotificationActivity.class);
            intent.putExtra("RECEIVER_IDS", new String[]{"all", "admin", "adminManager"});
            intent.putExtra("USER_ID", currentUserId);
            intent.putExtra("USER_ROLE", currentUserRole);
            startActivity(intent);
        });

        TextView welcomeUser = findViewById(R.id.welcomeUser);
        String userName = sharedPreferences.getString("USER_NAME", "Admin");
        welcomeUser.setText("Hi " + userName);

        fetchProfileImage();

        Button addCampaignButton = findViewById(R.id.addCampaignButton);
        addCampaignButton.setOnClickListener(v -> {
            if (!"admin".equals(currentUserRole)) {
                Toast.makeText(this, "You don't have permission to add campaigns!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, AddCampaignActivity.class);
            intent.putExtra("USER_ROLE", currentUserRole);
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


    private void checkForUnreadNotifications(ImageView notificationButton) {
        String userId = getIntent().getStringExtra("USER_ID");
        String userRole = getIntent().getStringExtra("USER_ROLE");

        if (userId == null || userRole == null) {
            Log.e(TAG, "User ID or Role is null. Ensure it is passed in the intent.");
            return;
        }

        // Include user-specific, "all", and role-specific IDs in the query
        List<String> validReceiverIds = new ArrayList<>(Arrays.asList("all", userId));
        if ("admin".equalsIgnoreCase(userRole)) {
            validReceiverIds.add("admin");
        } else if ("adminManager".equalsIgnoreCase(userRole)) {
            validReceiverIds.add("adminManager");
        }

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

        db.collection("Users").whereEqualTo("email", userEmail).get().addOnSuccessListener(querySnapshot -> {
            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                String profileImageUrl = document.getString("profileImage");

                Log.d(TAG, "Profile Image URL: " + profileImageUrl);

                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    Glide.with(this).load(profileImageUrl).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).placeholder(R.drawable.ic_placeholder).error(R.drawable.ic_placeholder).into(profileImage);
                } else {
                    profileImage.setImageResource(R.drawable.ic_placeholder);
                }
            } else {
                Log.e(TAG, "No user found with the given email.");
                profileImage.setImageResource(R.drawable.ic_placeholder);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching user profile: " + e.getMessage(), e);
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

            SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            String role = sharedPreferences.getString("USER_ROLE", "donor");
            intent.putExtra("USER_ROLE", role);

            startActivity(intent);
        });
        campaignList.addView(cardView);
    }

    private void showAssignPopup(String campaignId, String campaignTitle) {
        FirebaseFirestore.getInstance().collection("Users").whereEqualTo("role", "manager").get().addOnSuccessListener(querySnapshot -> {
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

            showManagerDropdownDialog(campaignId, campaignTitle, managerNames, managerEmails);
        }).addOnFailureListener(e -> Toast.makeText(this, "Error fetching managers", Toast.LENGTH_SHORT).show());
    }

    private void showManagerDropdownDialog(String campaignId, String campaignTitle, String[] managerNames, String[] managerEmails) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Assign Manager to " + campaignTitle);

        View dialogView = getLayoutInflater().inflate(R.layout.assign_manager, null);
        builder.setView(dialogView);

        Spinner managerSpinner = dialogView.findViewById(R.id.managerSpinner);
        Button assignButton = dialogView.findViewById(R.id.assignButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, managerNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        managerSpinner.setAdapter(adapter);

        AlertDialog dialog = builder.create();

        assignButton.setOnClickListener(v -> {
            int selectedIndex = managerSpinner.getSelectedItemPosition();
            if (selectedIndex >= 0) {
                String selectedManagerName = managerNames[selectedIndex];
                String selectedManagerEmail = managerEmails[selectedIndex];

                assignManagerToCampaign(campaignId, selectedManagerName, selectedManagerEmail);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please select a manager!", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void assignManagerToCampaign(String campaignId, String managerName, String managerEmail) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("managerName", managerName);
        updateData.put("managerEmail", managerEmail);

        FirebaseFirestore.getInstance().collection("DonationSites").document(campaignId).update(updateData).addOnSuccessListener(aVoid -> Toast.makeText(this, "Manager assigned successfully", Toast.LENGTH_SHORT).show()).addOnFailureListener(e -> Toast.makeText(this, "Error assigning manager", Toast.LENGTH_SHORT).show());
    }
}
