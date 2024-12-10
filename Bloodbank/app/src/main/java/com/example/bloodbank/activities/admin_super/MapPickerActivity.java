package com.example.bloodbank.activities.admin_super;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bloodbank.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {
    private MapView mapView;
    private GoogleMap mMap;
    private LatLng selectedLatLng;
    private String shortName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        mapView = findViewById(R.id.mapView);
        Button confirmButton = findViewById(R.id.confirmButton);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        confirmButton.setOnClickListener(v -> {
            if (selectedLatLng != null) {
                promptForShortName();
            } else {
                showToast("Please select a location on the map.");
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Set a default location which is Saigon
        LatLng defaultLocation = new LatLng(10.7769, 106.7009);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));

        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            selectedLatLng = latLng;
            mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
        });
    }

    private void promptForShortName() {
        EditText input = new EditText(this);
        input.setHint("Enter a short name for the location");

        new AlertDialog.Builder(this).setTitle("Short Name for Location").setMessage("Please provide a short name for the selected location.").setView(input).setPositiveButton("Confirm", (dialog, which) -> {
            shortName = input.getText().toString().trim();
            if (!shortName.isEmpty()) {
                returnLocationData();
            } else {
                showToast("Short name cannot be empty.");
            }
        }).setNegativeButton("Cancel", null).show();
    }

    private void returnLocationData() {
        Intent data = new Intent();
        data.putExtra("latitude", selectedLatLng.latitude);
        data.putExtra("longitude", selectedLatLng.longitude);
        data.putExtra("shortName", shortName);
        setResult(RESULT_OK, data);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
