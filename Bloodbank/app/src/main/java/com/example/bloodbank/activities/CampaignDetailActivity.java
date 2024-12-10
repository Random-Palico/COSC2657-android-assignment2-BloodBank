package com.example.bloodbank.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.bloodbank.R;
import com.example.bloodbank.activities.admin_super.EditCampaignActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class CampaignDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private TextView titleView, dateView, locationView, descriptionView, bloodTypesView;
    private ImageView campaignImageView;
    private Button editButton, registerButton;
    private MapView mapView;

    private String campaignId, userRole;
    private LatLng campaignLatLng;

    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campaign_detail);

        titleView = findViewById(R.id.campaignTitle);
        dateView = findViewById(R.id.campaignDate);
        locationView = findViewById(R.id.campaignLocation);
        descriptionView = findViewById(R.id.campaignDescription);
        bloodTypesView = findViewById(R.id.requiredBloodTypes);
        campaignImageView = findViewById(R.id.campaignImage);
        editButton = findViewById(R.id.editButton);
        registerButton = findViewById(R.id.registerButton);
        mapView = findViewById(R.id.campaignMapView);

        // Get campaign details from intent
        Intent intent = getIntent();
        campaignId = ((Intent) intent).getStringExtra("campaignId");
        String title = intent.getStringExtra("campaignTitle");
        String description = intent.getStringExtra("campaignDescription");
        String date = intent.getStringExtra("campaignDate");
        String location = intent.getStringExtra("campaignLocation");
        String imageUrl = intent.getStringExtra("campaignImage");
        double lat = intent.getDoubleExtra("latitude", 0);
        double lng = intent.getDoubleExtra("longitude", 0);
        ArrayList<String> bloodTypes = intent.getStringArrayListExtra("requiredBloodTypes");
        userRole = intent.getStringExtra("USER_ROLE");

        campaignLatLng = new LatLng(lat, lng);

        titleView.setText(title);
        dateView.setText(date);
        locationView.setText(location);
        descriptionView.setText(description);
        if (bloodTypes != null) {
            bloodTypesView.setText("Required Blood Types: " + String.join(", ", bloodTypes));
        } else {
            bloodTypesView.setText("Required Blood Types: Not specified");
        }
        Glide.with(this).load(imageUrl).into(campaignImageView);

        // Initialize MapView
        Bundle mapViewBundle = savedInstanceState != null
                ? savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY)
                : null;
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        if ("admin".equals(userRole)) {
            editButton.setVisibility(View.VISIBLE);
            registerButton.setVisibility(View.GONE);
        } else {
            editButton.setVisibility(View.GONE);
            registerButton.setVisibility(View.VISIBLE);
        }

        editButton.setOnClickListener(v -> {
            Intent editIntent = new Intent(this, EditCampaignActivity.class);
            editIntent.putExtra("campaignId", campaignId);
            editIntent.putExtra("campaignTitle", title);
            editIntent.putExtra("campaignDescription", description);
            editIntent.putExtra("campaignDate", date);
            editIntent.putExtra("campaignImage", imageUrl);
            editIntent.putExtra("campaignLocation", location);
            editIntent.putExtra("latitude", campaignLatLng.latitude);
            editIntent.putExtra("longitude", campaignLatLng.longitude);
            editIntent.putStringArrayListExtra("requiredBloodTypes", bloodTypes);

            startActivityForResult(editIntent, 300);
        });
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
