package com.example.bloodbank.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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

    private LatLng userLatLng;
    private Polyline currentPolyline;

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

        siteListRecyclerView = findViewById(R.id.siteListRecyclerView);
        siteListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        siteAdapter = new SiteAdapter(filteredSites, this::onSiteSelected);
        siteListRecyclerView.setAdapter(siteAdapter);

        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterSites(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSites(newText);
                return true;
            }
        });


        SupportMapFragment mapFragment = new SupportMapFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.mapFragmentContainer, mapFragment).commit();

        mapFragment.getMapAsync(this);

        fetchDonationSites();
        getUserLocation();
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            populateMap();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                if (mMap != null) {
                    addUserLocationMarker();
                }
            }
            populateMap();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get user location", e);
            populateMap();
        });
    }

    private void addUserLocationMarker() {
        if (userLatLng != null) {
            mMap.addMarker(new MarkerOptions().position(userLatLng).title("Your Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 12));
        }
    }

    private void filterSites(String query) {
        if (query == null || query.isEmpty()) {
            filteredSites.clear();
            filteredSites.addAll(donationSites);
        } else {
            filteredSites.clear();
            filteredSites.addAll(donationSites.stream().filter(site -> site.getString("shortName").toLowerCase().contains(query.toLowerCase())).collect(Collectors.toList()));
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
        db.collection("DonationSites").get().addOnSuccessListener(queryDocumentSnapshots -> {
            donationSites.clear();
            donationSites.addAll(queryDocumentSnapshots.getDocuments());
            filteredSites.clear();
            filteredSites.addAll(donationSites);
            siteAdapter.notifyDataSetChanged();
            populateMap();
        }).addOnFailureListener(e -> Log.e(TAG, "Error fetching donation sites", e));
    }

    private void populateMap() {
        if (mMap == null) return;

        if (userLatLng != null) {
            addUserLocationMarker();
        }

        for (DocumentSnapshot site : donationSites) {
            String shortName = site.getString("shortName");
            String address = site.getString("address");
            HashMap<String, Double> location = (HashMap<String, Double>) site.get("locationLatLng");
            if (shortName != null && location != null) {
                LatLng latLng = new LatLng(location.get("lat"), location.get("lng"));
                Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title(shortName).snippet(address).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                markerSiteMap.put(marker, site);
            }
        }

        if (!donationSites.isEmpty()) {
            HashMap<String, Double> firstLocation = (HashMap<String, Double>) donationSites.get(0).get("locationLatLng");
            if (firstLocation != null) {
                LatLng firstLatLng = new LatLng(firstLocation.get("lat"), firstLocation.get("lng"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLatLng, 12));
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
        if (userLatLng == null) return;

        String apiKey = getString(R.string.google_maps_key);
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + userLatLng.latitude + "," + userLatLng.longitude + "&destination=" + destination.latitude + "," + destination.longitude + "&key=" + apiKey;

        // Make HTTP request to fetch the route
        new Thread(() -> {
            try {
                URL directionUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) directionUrl.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                reader.close();

                String response = responseBuilder.toString();
                JSONObject jsonObject = new JSONObject(response);

                JSONArray routes = jsonObject.getJSONArray("routes");
                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                    String encodedPolyline = overviewPolyline.getString("points");

                    List<LatLng> polylinePoints = decodePolyline(encodedPolyline);

                    runOnUiThread(() -> {
                        if (currentPolyline != null) {
                            currentPolyline.remove();
                        }

                        PolylineOptions options = new PolylineOptions().addAll(polylinePoints).color(android.graphics.Color.RED).width(8);
                        currentPolyline = mMap.addPolyline(options);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching directions: ", e);
            }
        }).start();
    }

    private static List<LatLng> decodePolyline(String encoded) {
        List<LatLng> polyline = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            polyline.add(new LatLng((lat / 1E5), (lng / 1E5)));
        }

        return polyline;
    }


    private void showSiteDetails(DocumentSnapshot siteData) {
        String shortName = siteData.getString("shortName");
        String address = siteData.getString("address");
        List<String> bloodTypes = (List<String>) siteData.get("requiredBloodTypes");

        String details = "Short Name: " + shortName + "\nAddress: " + address + "\nRequired Blood Types: " + (bloodTypes != null ? bloodTypes.toString() : "N/A");
        Log.d(TAG, details);
    }
}
