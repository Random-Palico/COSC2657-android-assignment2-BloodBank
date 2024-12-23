package com.example.bloodbank.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        findViewById(R.id.confirmButton).setOnClickListener(v -> {
            if (selectedLatLng != null) {
                promptForDetails();
            } else {
                Toast.makeText(this, "Please select a location on the map.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // default location to saigon - may change later
        LatLng defaultLocation = new LatLng(10.7769, 106.7009);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));

        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            selectedLatLng = latLng;
            mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
        });
    }

    private void promptForDetails() {
        // Input box for name and address
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        EditText shortNameInput = new EditText(this);
        shortNameInput.setHint("Enter a short name");
        layout.addView(shortNameInput);

        EditText addressInput = new EditText(this);
        addressInput.setHint("Enter the address");
        layout.addView(addressInput);

        new AlertDialog.Builder(this)
                .setTitle("Location Details")
                .setView(layout)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String shortName = shortNameInput.getText().toString().trim();
                    String address = addressInput.getText().toString().trim();

                    if (shortName.isEmpty() || address.isEmpty()) {
                        Toast.makeText(this, "Both short name and address are required.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    returnLocationData(shortName, address);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void returnLocationData(String shortName, String address) {
        Intent data = new Intent();
        data.putExtra("latitude", selectedLatLng.latitude);
        data.putExtra("longitude", selectedLatLng.longitude);
        data.putExtra("shortName", shortName);
        data.putExtra("address", address);
        setResult(RESULT_OK, data);
        finish();
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
}
