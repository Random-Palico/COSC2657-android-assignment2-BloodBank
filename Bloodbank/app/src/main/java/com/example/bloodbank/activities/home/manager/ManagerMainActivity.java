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
import com.example.bloodbank.activities.NotificationActivity;
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
            intent.putExtra("RECEIVER_IDS", new String[]{"all", "adminManager"});
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

        registerButton.setVisibility(View.VISIBLE);
        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, CampaignDetailActivity.class);
            intent.putExtra("campaignId", document.getId());
            startActivity(intent);
        });

        editButton.setVisibility(View.VISIBLE);
        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, CampaignDetailActivity.class);
            intent.putExtra("campaignId", document.getId());
            startActivity(intent);
        });

        assignButton.setVisibility(View.VISIBLE);
        assignButton.setOnClickListener(v -> showAssignConfirmation(document));

        campaignList.addView(cardView);
    }

    private void showAssignConfirmation(DocumentSnapshot document) {
        new AlertDialog.Builder(this)
                .setTitle("Assign Task")
                .setMessage("Are you sure you want to assign this task?")
                .setPositiveButton("Yes", (dialog, which) -> assignManagerToCampaign(document))
                .setNegativeButton("No", null)
                .show();
    }

    private void assignManagerToCampaign(DocumentSnapshot document) {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String managerName = sharedPreferences.getString("USER_NAME", "Manager");
        String managerEmail = sharedPreferences.getString("USER_EMAIL", "");

        Map<String, Object> updateData = Map.of(
                "managerName", managerName,
                "managerEmail", managerEmail
        );

        db.collection("DonationSites").document(document.getId())
                .update(updateData)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Task assigned successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to assign task.", Toast.LENGTH_SHORT).show());
    }
}
