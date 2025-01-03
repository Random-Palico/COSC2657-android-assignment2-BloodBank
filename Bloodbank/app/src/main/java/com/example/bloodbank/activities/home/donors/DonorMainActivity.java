package com.example.bloodbank.activities.home.donors;

import android.app.AlertDialog;
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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.bloodbank.R;
import com.example.bloodbank.activities.CampaignDetailActivity;
import com.example.bloodbank.activities.DonorRegisterActivity;
import com.example.bloodbank.activities.NotificationActivity;
import com.example.bloodbank.handler.BaseActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

        ImageView notificationButton = findViewById(R.id.notificationButton);

        notificationButton.setOnClickListener(v -> {
            SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            String userId = sharedPreferences.getString("USER_ID", null);

            if (userId == null) {
                Toast.makeText(this, "User ID is missing!", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(DonorMainActivity.this, NotificationActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("RECEIVER_IDS", new String[]{"all", userId});
            intent.putExtra("USER_ROLE", "donor");
            startActivity(intent);
        });
    }

    private void checkForUnreadNotifications() {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("USER_ID", null);

        if (TextUtils.isEmpty(userId)) {
            Log.e(TAG, "User ID is missing. Notifications cannot be fetched.");
            return;
        }

        List<String> validReceiverIds = Arrays.asList("all", userId);

        db.collection("Notifications").whereIn("receiverId", validReceiverIds).get().addOnSuccessListener(querySnapshot -> {
            if (querySnapshot.isEmpty()) {
                Log.d(TAG, "No notifications found.");
                updateNotificationIcon(false);
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

            updateNotificationIcon(hasUnreadNotifications);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error checking notifications: ", e);
            updateNotificationIcon(false);
        });
    }

    private void updateNotificationIcon(boolean hasUnreadNotifications) {
        ImageView notificationButton = findViewById(R.id.notificationButton);
        if (hasUnreadNotifications) {
            notificationButton.setColorFilter(getResources().getColor(R.color.red), android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            notificationButton.setColorFilter(getResources().getColor(R.color.gray), android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == 100) {
                SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
                String userName = sharedPreferences.getString("USER_NAME", "Donor");
                TextView welcomeUser = findViewById(R.id.welcomeUser);
                welcomeUser.setText("Hi " + userName);
                fetchProfileImage();
            }
        }
    }


    private void initializeViews() {
        TextView welcomeUser = findViewById(R.id.welcomeUser);
        profileImage = findViewById(R.id.profileImage);
        campaignList = findViewById(R.id.campaignList);

        String userName = getIntent().getStringExtra("USER_NAME");
        if (userName == null) {
            userName = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getString("USER_NAME", "Donor");
        }
        welcomeUser.setText(userName != null ? "Hi " + userName : "Hi Donor");

        getIntent().getStringExtra("USER_ROLE");

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
    protected void onStart() {
        super.onStart();
        fetchProfileImage();
        fetchCampaigns();
        checkForUnreadNotifications();

        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("USER_ID", "");

        db.collection("Users").document(userId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String bloodType = snapshot.getString("bloodType");
                Log.d(TAG, "onStart - Blood Type from Firestore: " + bloodType);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("USER_BLOOD_TYPE", bloodType);
                editor.apply();
            } else {
                Log.e(TAG, "onStart - User data not found in Firestore.");
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "onStart - Failed to fetch user data: " + e.getMessage());
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        fetchProfileImage();
        fetchCampaigns();
        checkForUnreadNotifications();

        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("USER_ID", "");

        db.collection("Users").document(userId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String bloodType = snapshot.getString("bloodType");
                Log.d(TAG, "onResume - Blood Type from Firestore: " + bloodType);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("USER_BLOOD_TYPE", bloodType);
                editor.apply();
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

    private void proceedToRegistration(DocumentSnapshot document, String title, String date, String location, String address, String eventImg, String userName, String userBloodType, String userLocation) {
        Intent intent = new Intent(this, DonorRegisterActivity.class);
        intent.putExtra("campaignId", document.getId());
        intent.putExtra("campaignTitle", title);
        intent.putExtra("campaignDate", date);
        intent.putExtra("campaignLocation", location);
        intent.putExtra("campaignAddress", address);
        intent.putExtra("campaignImage", eventImg);

        intent.putExtra("userName", userName);
        intent.putExtra("userBloodType", userBloodType);
        intent.putExtra("userLocation", userLocation);

        startActivity(intent);
    }


    private void addCampaignCard(DocumentSnapshot document, String title, String date, String location, String eventImg) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View cardView = inflater.inflate(R.layout.campaign_card, campaignList, false);

        TextView campaignTitle = cardView.findViewById(R.id.campaignTitle);
        TextView campaignDate = cardView.findViewById(R.id.campaignDate);
        TextView campaignLocation = cardView.findViewById(R.id.campaignLocation);
        ImageView campaignImage = cardView.findViewById(R.id.campaignImage);
        TextView campaignRequiredBloodTypes = cardView.findViewById(R.id.campaignRequiredBloodTypes);
        Button registerButton = cardView.findViewById(R.id.registerButton);
        Button editButton = cardView.findViewById(R.id.editButton);
        Button assignButton = cardView.findViewById(R.id.assignButton);

        campaignTitle.setText(title);
        campaignDate.setText(date);
        campaignLocation.setText(location);

        Glide.with(this).load(eventImg).into(campaignImage);

        List<String> requiredBloodTypes = (List<String>) document.get("requiredBloodTypes");
        if (requiredBloodTypes != null) {
            campaignRequiredBloodTypes.setText("Required Blood Types: " + String.join(", ", requiredBloodTypes));
        } else {
            campaignRequiredBloodTypes.setText("Required Blood Types: Not specified");
        }


        String address = document.getString("address");

        // Register button visible for donors
        registerButton.setVisibility(View.VISIBLE);
        registerButton.setOnClickListener(v -> {
            SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            String userName = sharedPreferences.getString("USER_NAME", "");
            String userBloodType = sharedPreferences.getString("USER_BLOOD_TYPE", "");
            String userLocation = sharedPreferences.getString("USER_LOCATION", "");

            if (requiredBloodTypes != null && requiredBloodTypes.contains("All")) {
                proceedToRegistration(document, title, date, location, address, eventImg, userName, userBloodType, userLocation);
            } else if (userBloodType == null || userBloodType.isEmpty()) {
                showConfirmationDialog(() -> proceedToRegistration(document, title, date, location, address, eventImg, userName, userBloodType, userLocation));
            } else if (requiredBloodTypes != null && requiredBloodTypes.contains(userBloodType)) {
                proceedToRegistration(document, title, date, location, address, eventImg, userName, userBloodType, userLocation);
            } else {
                Toast.makeText(this, "Your blood type is not required for this campaign.", Toast.LENGTH_LONG).show();
            }
        });


        editButton.setVisibility(View.GONE);
        assignButton.setVisibility(View.GONE);

        campaignList.addView(cardView);

        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(this, CampaignDetailActivity.class);
            intent.putExtra("campaignId", document.getId());
            intent.putExtra("campaignTitle", title);
            intent.putExtra("campaignDescription", document.getString("description"));
            intent.putExtra("campaignDate", date);
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
    }

    private void showConfirmationDialog(Runnable onConfirm) {
        new AlertDialog.Builder(this).setTitle("No Blood Type Data").setMessage("You have not provided your blood type. Do you still want to register for this campaign?").setPositiveButton("Yes", (dialog, which) -> onConfirm.run()).setNegativeButton("No", null).show();
    }

}



