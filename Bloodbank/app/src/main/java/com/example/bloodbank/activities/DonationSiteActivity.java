package com.example.bloodbank.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bloodbank.R;
import com.example.bloodbank.handler.BaseActivity;
import com.example.bloodbank.handler.SiteAdapter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class DonationSiteActivity extends BaseActivity implements OnMapReadyCallback {

    private static final String TAG = "DonationSiteActivity";
    private static final int LOCATION_REQUEST_CODE = 100;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;

    private RecyclerView siteListRecyclerView;
    private SiteAdapter siteAdapter;
    private List<DocumentSnapshot> donationSites;
    private List<DocumentSnapshot> filteredSites;
    private HashMap<Marker, DocumentSnapshot> markerSiteMap;

    private Location userLocation;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation_site);

        setupBottomNavigation();

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        donationSites = new ArrayList<>();
        filteredSites = new ArrayList<>();
        markerSiteMap = new HashMap<>();

        searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterSites(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSites(newText);
                return false;
            }
        });

        siteListRecyclerView = findViewById(R.id.siteListRecyclerView);
        siteListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        siteAdapter = new SiteAdapter(filteredSites, this::onSiteSelected);
        siteListRecyclerView.setAdapter(siteAdapter);

        SupportMapFragment mapFragment = new SupportMapFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mapFragmentContainer, mapFragment)
                .commit();

        mapFragment.getMapAsync(this);

        fetchDonationSites();
        getUserLocation();
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        userLocation = location;
                        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        if (mMap != null) {
                            mMap.addMarker(new MarkerOptions().position(userLatLng).title("Your Location"));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 12));
                        }
                    } else {
                        Toast.makeText(this, "Unable to fetch your location", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get user location", e));
    }

    private void filterSites(String query) {
        if (query == null || query.isEmpty()) {
            filteredSites.clear();
            filteredSites.addAll(donationSites);
        } else {
            filteredSites.clear();
            filteredSites.addAll(donationSites.stream()
                    .filter(site -> site.getString("shortName").toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList()));
        }
        siteAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getUserLocation();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "Map is ready");
        mMap.setOnMarkerClickListener(marker -> {
            DocumentSnapshot siteData = markerSiteMap.get(marker);
            if (siteData != null) {
                onSiteSelected(siteData);
            }
            return false;
        });
    }

    private void fetchDonationSites() {
        db.collection("DonationSites").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    donationSites.clear();
                    donationSites.addAll(queryDocumentSnapshots.getDocuments());
                    filteredSites.clear();
                    filteredSites.addAll(donationSites);
                    siteAdapter.notifyDataSetChanged();
                    populateMap();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching donation sites", e);
                    Toast.makeText(this, "Error loading sites", Toast.LENGTH_SHORT).show();
                });
    }

    private void populateMap() {
        if (mMap == null) return;

        for (DocumentSnapshot site : donationSites) {
            String shortName = site.getString("shortName");
            HashMap<String, Double> location = (HashMap<String, Double>) site.get("locationLatLng");
            if (shortName != null && location != null) {
                LatLng latLng = new LatLng(location.get("lat"), location.get("lng"));
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(shortName)
                        .snippet("Click for details"));
                markerSiteMap.put(marker, site);
            }
        }

        mMap.setOnInfoWindowClickListener(marker -> {
            DocumentSnapshot siteData = markerSiteMap.get(marker);
            if (siteData != null) {
                onSiteSelected(siteData);
            }
        });
    }

    private void onSiteSelected(DocumentSnapshot siteData) {
        HashMap<String, Double> location = (HashMap<String, Double>) siteData.get("locationLatLng");
        if (location != null) {
            LatLng latLng = new LatLng(location.get("lat"), location.get("lng"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            drawRouteToLocation(latLng);
            showSiteDetails(siteData);
        }
    }

    private void drawRouteToLocation(LatLng destination) {
        if (userLocation != null) {
            LatLng userLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
            PolylineOptions options = new PolylineOptions()
                    .add(userLatLng)
                    .add(destination)
                    .color(android.graphics.Color.RED)
                    .width(8);
            mMap.addPolyline(options);
        } else {
            Toast.makeText(this, "Unable to determine your location.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSiteDetails(DocumentSnapshot siteData) {
        String shortName = siteData.getString("shortName");
        List<String> bloodTypes = (List<String>) siteData.get("requiredBloodTypes");

        String details = "Short Name: " + shortName + "\nRequired Blood Types: " + (bloodTypes != null ? bloodTypes.toString() : "N/A");
        Toast.makeText(this, details, Toast.LENGTH_LONG).show();
    }
}
