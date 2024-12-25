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
    private String userId;
    private String currentUserRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        db = FirebaseFirestore.getInstance();
        setupBottomNavigation();

        profileImage = findViewById(R.id.profileImage);

        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        userId = sharedPreferences.getString("USER_ID", null);
        currentUserRole = sharedPreferences.getString("USER_ROLE", "user");

        if (userId == null) {
            Toast.makeText(this, "Unable to fetch user details. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ImageView notificationButton = findViewById(R.id.notificationButton);

        notificationButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminMainActivity.this, NotificationActivity.class);
            intent.putExtra("RECEIVER_IDS", new String[]{"all", "admin", "adminManager", userId});
            intent.putExtra("USER_ID", userId);
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
        checkForUnreadNotifications();
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

    private void checkForUnreadNotifications() {
        ImageView notificationButton = findViewById(R.id.notificationButton);

        if (userId == null) {
            Log.e(TAG, "User ID is null. Ensure it is passed in the intent or shared preferences.");
            return;
        }

        List<String> validReceiverIds = Arrays.asList("all", "admin", "adminManager", userId);

        db.collection("Notifications").whereIn("receiverId", validReceiverIds).get().addOnSuccessListener(querySnapshot -> {
            if (querySnapshot.isEmpty()) {
                Log.d(TAG, "No notifications found.");
                updateNotificationIcon(notificationButton, false);
                return;
            }

            boolean hasUnreadNotifications = false;

            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                List<String> readBy = (List<String>) document.get("readBy");
                if (readBy == null || !readBy.contains(userId)) {
                    hasUnreadNotifications = true;
                    break;
                }
            }

            updateNotificationIcon(notificationButton, hasUnreadNotifications);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error checking notifications: ", e);
            updateNotificationIcon(notificationButton, false);
        });
    }

    private void updateNotificationIcon(ImageView notificationButton, boolean hasUnreadNotifications) {
        if (hasUnreadNotifications) {
            // Show red icon for unread notifications
            notificationButton.setColorFilter(getResources().getColor(R.color.red), android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            // Show default gray icon for no unread notifications
            notificationButton.setColorFilter(getResources().getColor(R.color.gray), android.graphics.PorterDuff.Mode.SRC_IN);
        }
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

        List<String> managerNames = (List<String>) document.get("managerName");
        List<String> managerEmails = (List<String>) document.get("managerEmail");

        if (managerNames != null && !managerNames.isEmpty()) {
            assignButton.setText("Manage");
            assignButton.setOnClickListener(v -> showManagePopup(document, managerNames, managerEmails));
        } else {
            assignButton.setText("Assign");
            assignButton.setOnClickListener(v -> showAssignPopup(document.getId(), title));
        }

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
        db.collection("DonationSites").document(campaignId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<String> managerNames = (List<String>) documentSnapshot.get("managerName");
                List<String> managerEmails = (List<String>) documentSnapshot.get("managerEmail");

                if (managerNames == null) {
                    managerNames = new ArrayList<>();
                }
                if (managerEmails == null) {
                    managerEmails = new ArrayList<>();
                }

                if (managerEmails.contains(managerEmail)) {
                    Toast.makeText(this, "Manager is already assigned to this campaign!", Toast.LENGTH_SHORT).show();
                    return;
                }

                managerNames.add(managerName);
                managerEmails.add(managerEmail);

                db.collection("DonationSites").document(campaignId).update("managerName", managerNames, "managerEmail", managerEmails).addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Manager assigned successfully!", Toast.LENGTH_SHORT).show();
                    fetchCampaigns();
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to assign manager: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error assigning manager", e);
                });
            } else {
                Toast.makeText(this, "Campaign not found!", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error retrieving campaign data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error retrieving campaign data", e);
        });
    }

    private void showManagePopup(DocumentSnapshot document, List<String> managerNames, List<String> managerEmails) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manage Assigned Managers");

        View dialogView = getLayoutInflater().inflate(R.layout.unassign_manager, null);
        builder.setView(dialogView);

        TextView assignedManagerText = dialogView.findViewById(R.id.assignedManagerText);
        Spinner managerSpinner = dialogView.findViewById(R.id.managerSpinner);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button unassignButton = dialogView.findViewById(R.id.unassignButton);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, managerNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        managerSpinner.setAdapter(adapter);

        StringBuilder managersDisplay = new StringBuilder("Assigned Managers:\n");
        for (int i = 0; i < managerNames.size(); i++) {
            managersDisplay.append(managerNames.get(i)).append(" (").append(managerEmails.get(i)).append(")\n");
        }
        assignedManagerText.setText(managersDisplay.toString().trim());

        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        unassignButton.setOnClickListener(v -> {
            int selectedIndex = managerSpinner.getSelectedItemPosition();
            if (selectedIndex >= 0) {
                String managerEmail = managerEmails.get(selectedIndex);
                String managerName = managerNames.get(selectedIndex);
                unassignManagerFromCampaign(document, new ArrayList<>(managerNames), new ArrayList<>(managerEmails), managerEmail, managerName);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please select a manager to unassign!", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }


    private void unassignManagerFromCampaign(DocumentSnapshot document, List<String> managerNames, List<String> managerEmails, String managerEmail, String managerName) {
        managerNames.remove(managerName);
        managerEmails.remove(managerEmail);

        db.collection("DonationSites").document(document.getId()).update("managerName", managerNames, "managerEmail", managerEmails).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Manager unassigned successfully!", Toast.LENGTH_SHORT).show();
            fetchCampaigns();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to unassign manager: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error unassigning manager", e);
        });
    }
}
