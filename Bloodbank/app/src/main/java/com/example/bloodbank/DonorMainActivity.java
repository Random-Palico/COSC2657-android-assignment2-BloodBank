package com.example.bloodbank;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DonorMainActivity extends AppCompatActivity {

    private static final String TAG = "DonorMainActivity";
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_donor);

        TextView welcomeUser = findViewById(R.id.welcomeUser);
        String userName = getIntent().getStringExtra("USER_NAME");
        welcomeUser.setText(userName != null ? "Hi " + userName : "Hi Donor");

        db = FirebaseFirestore.getInstance();
        LinearLayout campaignList = findViewById(R.id.campaignList);

        // Fetch campaigns
        fetchCampaigns(campaignList);
    }

    private void fetchCampaigns(LinearLayout campaignList) {
        db.collection("DonationSites")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No campaigns available!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String title = document.getString("siteName");
                        String location = document.getString("address");
                        Timestamp eventTimestamp = document.getTimestamp("eventDate");

                        if (title == null || eventTimestamp == null || location == null) {
                            Log.e(TAG, "Missing data in Firestore document: " + document.getId());
                            continue;
                        }

                        Date eventDate = eventTimestamp.toDate();
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
                        String formattedDate = dateFormat.format(eventDate);

                        // Add card dynamically
                        addCampaignCard(campaignList, title, formattedDate, location);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching campaigns", e);
                    Toast.makeText(this, "Error fetching campaigns: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addCampaignCard(LinearLayout campaignList, String title, String date, String location) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View cardView = inflater.inflate(R.layout.campaign_card, campaignList, false);

        TextView campaignTitle = cardView.findViewById(R.id.campaignTitle);
        TextView campaignDate = cardView.findViewById(R.id.campaignDate);
        TextView campaignLocation = cardView.findViewById(R.id.campaignLocation);
        Button registerButton = cardView.findViewById(R.id.registerButton);

        campaignTitle.setText(title);
        campaignDate.setText(date);
        campaignLocation.setText(location);

        registerButton.setOnClickListener(v -> Toast.makeText(this, "Registered for " + title, Toast.LENGTH_SHORT).show());

        campaignList.addView(cardView);
    }
}
