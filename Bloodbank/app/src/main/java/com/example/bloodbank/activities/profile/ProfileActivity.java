package com.example.bloodbank.activities.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.bloodbank.R;
import com.example.bloodbank.activities.login_register.LoginActivity;
import com.example.bloodbank.handler.BaseActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Map;

public class ProfileActivity extends BaseActivity {

    private static final int REQUEST_CODE_EDIT_PROFILE = 101;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout profileDetailsLayout, missingInfoCard;
    private ImageView profileImage;
    private TextView userName, userEmail, userDob, userAge, userBloodType, userLocation;
    private Button editProfileButton, signOutButton, updateInfoButton;
    private String profileImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        setupBottomNavigation();

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        loadUserProfile();

        signOutButton.setOnClickListener(v -> {
            auth.signOut();
            Toast.makeText(this, "Signed out successfully!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        editProfileButton.setOnClickListener(v -> navigateToEditUpdateProfile());
        updateInfoButton.setOnClickListener(v -> navigateToEditUpdateProfile());
    }

    private void initializeViews() {
        profileDetailsLayout = findViewById(R.id.profileDetailsLayout);
        missingInfoCard = findViewById(R.id.missingInfoCard);

        profileImage = findViewById(R.id.profileImage);
        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        userDob = findViewById(R.id.userDob);
        userAge = findViewById(R.id.userAge);
        userBloodType = findViewById(R.id.userBloodType);
        userLocation = findViewById(R.id.userLocation);

        editProfileButton = findViewById(R.id.editProfileButton);
        signOutButton = findViewById(R.id.signOutButton);
        updateInfoButton = findViewById(R.id.updateInfoButton);
    }

    private void loadUserProfile() {
        String userId = auth.getCurrentUser().getUid();

        db.collection("Users").document(userId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                displayUserData(snapshot.getData());
            } else {
                Toast.makeText(this, "User data not found!", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show());
    }

    private void displayUserData(Map<String, Object> data) {
        userName.setText("Name: " + (String) data.getOrDefault("name", "N/A"));
        userEmail.setText("Email: " + (String) data.getOrDefault("email", "N/A"));
        userDob.setText("Date of Birth: " + (String) data.getOrDefault("dob", "N/A"));
        userAge.setText("Age: " + calculateAge((String) data.get("dob")));
        userBloodType.setText("Blood Type: " + (String) data.getOrDefault("bloodType", "N/A"));
        userLocation.setText("Location: " + (String) data.getOrDefault("location", "N/A"));

        profileImageUrl = (String) data.get("profileImage");
        if (profileImageUrl != null) {
            Glide.with(this).load(profileImageUrl).into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.ic_placeholder);
        }

        boolean isMissingInfo = data.get("dob") == null || data.get("bloodType") == null || data.get("location") == null;
        missingInfoCard.setVisibility(isMissingInfo ? View.VISIBLE : View.GONE);
    }

    private String calculateAge(String dob) {
        if (dob == null || dob.isEmpty()) return "N/A";

        try {
            String[] parts = dob.split("/");
            int year = Integer.parseInt(parts[2]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[0]);

            Calendar birthDate = Calendar.getInstance();
            birthDate.set(year, month - 1, day);

            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR);

            if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }

            return String.valueOf(age);
        } catch (Exception e) {
            e.printStackTrace();
            return "N/A";
        }
    }

    private void navigateToEditUpdateProfile() {
        Intent intent = new Intent(ProfileActivity.this, EditUpdateProfileActivity.class);
        intent.putExtra("profileImageUrl", profileImageUrl);
        startActivityForResult(intent, REQUEST_CODE_EDIT_PROFILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_EDIT_PROFILE && resultCode == RESULT_OK) {
            loadUserProfile();
        }
    }
}