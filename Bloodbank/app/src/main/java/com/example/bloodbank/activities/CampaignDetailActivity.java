package com.example.bloodbank.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.bloodbank.R;
import com.example.bloodbank.activities.add_edit_campaign.EditCampaignActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class CampaignDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private TextView titleView, dateView, locationView, addressView, descriptionView, bloodTypesView;
    private ImageView campaignImageView;
    private Button editButton, registerButton;
    private MapView mapView;

    private String campaignId, userRole;
    private LatLng campaignLatLng;

    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    private void proceedToRegistration(String title, String description, String date, String location, String address, String imageUrl, LatLng campaignLatLng, ArrayList<String> bloodTypes, String userName, String userBloodType, String userLocation) {
        Intent registerIntent = new Intent(this, DonorRegisterActivity.class);
        registerIntent.putExtra("campaignId", campaignId);
        registerIntent.putExtra("campaignTitle", title);
        registerIntent.putExtra("campaignDescription", description);
        registerIntent.putExtra("campaignDate", date);
        registerIntent.putExtra("campaignImage", imageUrl);
        registerIntent.putExtra("campaignLocation", location);
        registerIntent.putExtra("campaignAddress", address);
        registerIntent.putExtra("latitude", campaignLatLng.latitude);
        registerIntent.putExtra("longitude", campaignLatLng.longitude);
        registerIntent.putStringArrayListExtra("requiredBloodTypes", bloodTypes);

        registerIntent.putExtra("userName", userName);
        registerIntent.putExtra("userBloodType", userBloodType);
        registerIntent.putExtra("userLocation", userLocation);

        startActivity(registerIntent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campaign_detail);

        titleView = findViewById(R.id.campaignTitle);
        dateView = findViewById(R.id.campaignDate);
        locationView = findViewById(R.id.campaignLocation);
        addressView = findViewById(R.id.campaignAddress);
        descriptionView = findViewById(R.id.campaignDescription);
        bloodTypesView = findViewById(R.id.requiredBloodTypes);
        campaignImageView = findViewById(R.id.campaignImage);
        editButton = findViewById(R.id.editButton);
        registerButton = findViewById(R.id.registerButton);
        mapView = findViewById(R.id.campaignMapView);
        ImageButton backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());

        Intent intent = getIntent();
        campaignId = ((Intent) intent).getStringExtra("campaignId");
        String title = intent.getStringExtra("campaignTitle");
        String description = intent.getStringExtra("campaignDescription");
        String date = intent.getStringExtra("campaignDate");
        String location = intent.getStringExtra("campaignLocation");
        String address = intent.getStringExtra("campaignAddress");
        String imageUrl = intent.getStringExtra("campaignImage");
        double lat = intent.getDoubleExtra("latitude", 0);
        double lng = intent.getDoubleExtra("longitude", 0);
        ArrayList<String> bloodTypes = intent.getStringArrayListExtra("requiredBloodTypes");
        userRole = intent.getStringExtra("USER_ROLE");

        campaignLatLng = new LatLng(lat, lng);

        titleView.setText(title);
        dateView.setText(date);
        locationView.setText(location);
        addressView.setText(address);
        descriptionView.setText(description);
        if (bloodTypes != null) {
            bloodTypesView.setText("Required Blood Types: " + String.join(", ", bloodTypes));
        } else {
            bloodTypesView.setText("Required Blood Types: Not specified");
        }
        Glide.with(this).load(imageUrl).into(campaignImageView);

        // Initialize MapView
        Bundle mapViewBundle = savedInstanceState != null ? savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY) : null;
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        if ("admin".equals(userRole)) {
            editButton.setVisibility(View.VISIBLE);
            registerButton.setVisibility(View.GONE);
        } else if ("manager".equals(userRole)) {
            editButton.setVisibility(View.VISIBLE);
            registerButton.setVisibility(View.VISIBLE);
        } else {
            editButton.setVisibility(View.GONE);
            registerButton.setVisibility(View.VISIBLE);
        }

        // Edit button
        editButton.setOnClickListener(v -> {
            Intent editIntent = new Intent(this, EditCampaignActivity.class);
            editIntent.putExtra("campaignId", campaignId);
            editIntent.putExtra("campaignTitle", title);
            editIntent.putExtra("campaignDescription", description);
            editIntent.putExtra("campaignDate", date);
            editIntent.putExtra("campaignImage", imageUrl);
            editIntent.putExtra("campaignLocation", location);
            editIntent.putExtra("campaignAddress", address);
            editIntent.putExtra("latitude", campaignLatLng.latitude);
            editIntent.putExtra("longitude", campaignLatLng.longitude);
            editIntent.putStringArrayListExtra("requiredBloodTypes", bloodTypes);

            startActivity(editIntent);
        });

        registerButton.setOnClickListener(v -> {
            SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            String userName = sharedPreferences.getString("USER_NAME", "");
            String userBloodType = sharedPreferences.getString("USER_BLOOD_TYPE", "");
            String userLocation = sharedPreferences.getString("USER_LOCATION", "");

            if (bloodTypes != null && bloodTypes.contains("All")) {
                proceedToRegistration(title, description, date, location, address, imageUrl, campaignLatLng, bloodTypes, userName, userBloodType, userLocation);
            } else if (userBloodType == null || userBloodType.isEmpty()) {
                showConfirmationDialog(() -> proceedToRegistration(title, description, date, location, address, imageUrl, campaignLatLng, bloodTypes, userName, userBloodType, userLocation));
            } else if (bloodTypes != null && bloodTypes.contains(userBloodType)) {
                proceedToRegistration(title, description, date, location, address, imageUrl, campaignLatLng, bloodTypes, userName, userBloodType, userLocation);
            } else {
                Toast.makeText(CampaignDetailActivity.this, "Your blood type is not required for this campaign.", Toast.LENGTH_LONG).show();
            }
        });


    }

    private void showConfirmationDialog(Runnable onConfirm) {
        new AlertDialog.Builder(this).setTitle("No Blood Type Data").setMessage("You have not provided your blood type. Do you still want to register for this campaign?").setPositiveButton("Yes", (dialog, which) -> onConfirm.run()).setNegativeButton("No", null).show();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(campaignLatLng, 15));
        googleMap.addMarker(new MarkerOptions().position(campaignLatLng).title("Campaign Location"));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
        }

        mapView.onSaveInstanceState(mapViewBundle);
    }
}
