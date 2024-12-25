package com.example.bloodbank.activities.DonorManagement;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.bloodbank.R;

public class DonorInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donor_info);

        ImageView profileImage = findViewById(R.id.profileImage);
        TextView name = findViewById(R.id.name);
        TextView email = findViewById(R.id.email);
        TextView bloodType = findViewById(R.id.bloodType);
        TextView bloodUnit = findViewById(R.id.bloodUnit);
        TextView dob = findViewById(R.id.dob);
        TextView location = findViewById(R.id.location);
        TextView userId = findViewById(R.id.userId);
        ImageButton backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());

        String donorName = getIntent().getStringExtra("name");
        String donorEmail = getIntent().getStringExtra("email");
        String donorBloodType = getIntent().getStringExtra("bloodType");
        String donorBloodUnit = getIntent().getStringExtra("bloodUnit");
        String donorDob = getIntent().getStringExtra("dob");
        String donorLocation = getIntent().getStringExtra("location");
        String donorUserId = getIntent().getStringExtra("userId");
        String profileImageUrl = getIntent().getStringExtra("profileImage");

        name.setText(donorName);
        email.setText(donorEmail);
        bloodType.setText(donorBloodType);
        bloodUnit.setText(donorBloodUnit);
        dob.setText(donorDob);
        location.setText(donorLocation);
        userId.setText(donorUserId);

        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Glide.with(this).load(profileImageUrl).placeholder(R.drawable.ic_placeholder).into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.ic_placeholder);
        }
    }
}
