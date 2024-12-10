package com.example.bloodbank.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.bloodbank.R;
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
                        String eventImg = document.getString("eventImg");

                        if (title == null || eventTimestamp == null || location == null || eventImg == null) {
                            Log.e(TAG, "Missing data in Firestore document: " + document.getId());
                            continue;
                        }

                        Date eventDate = eventTimestamp.toDate();
                        String formattedDate = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(eventDate);

                        addCampaignCard(campaignList, title, formattedDate, location, eventImg);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching campaigns", e);
                    Toast.makeText(this, "Error fetching campaigns: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addCampaignCard(LinearLayout campaignList, String title, String date, String location, String eventImg) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View cardView = inflater.inflate(R.layout.campaign_card, campaignList, false);

        TextView campaignTitle = cardView.findViewById(R.id.campaignTitle);
        TextView campaignDate = cardView.findViewById(R.id.campaignDate);
        TextView campaignLocation = cardView.findViewById(R.id.campaignLocation);
        ImageView campaignImage = cardView.findViewById(R.id.campaignImage);
        Button registerButton = cardView.findViewById(R.id.registerButton);

        campaignTitle.setText(title);
        campaignDate.setText(date);
        campaignLocation.setText(location);

        Glide.with(this)
                .load(eventImg)
//                .placeholder(R.drawable.placeholder)
//                .error(R.drawable.error)
                .into(campaignImage);

        registerButton.setOnClickListener(v -> Toast.makeText(this, "Registered for " + title, Toast.LENGTH_SHORT).show());

        campaignList.addView(cardView);
    }
}
