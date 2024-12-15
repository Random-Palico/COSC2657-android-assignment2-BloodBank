package com.example.bloodbank.handler;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bloodbank.R;
import com.example.bloodbank.activities.DonationSiteActivity;
import com.example.bloodbank.activities.DonorMainActivity;
import com.example.bloodbank.activities.DonorManagement.DonorListActivity;
import com.example.bloodbank.activities.admin_super.AdminMainActivity;
import com.example.bloodbank.activities.profile.ProfileActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BaseActivity extends AppCompatActivity {

    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve user role from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        userRole = sharedPreferences.getString("USER_ROLE", "donor");
    }

    protected void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        if (bottomNav == null) {
            Log.e("BaseActivity", "BottomNavigationView not found in this layout. Skipping setup.");
            return;
        }

        Menu menu = bottomNav.getMenu();

        // Hide menu items based on the user role
        if ("donor".equals(userRole)) {
            menu.findItem(R.id.nav_donor_list).setVisible(false);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                if ("admin".equals(userRole) && !(this instanceof AdminMainActivity)) {
                    navigateToActivity(AdminMainActivity.class);
                } else if ("donor".equals(userRole) && !(this instanceof DonorMainActivity)) {
                    navigateToActivity(DonorMainActivity.class);
                }
                return true;
            } else if (itemId == R.id.nav_sites) {
                if (!(this instanceof DonationSiteActivity)) {
                    navigateToActivity(DonationSiteActivity.class);
                }
                return true;
            } else if (itemId == R.id.nav_donor_list) {
                if ("admin".equals(userRole) && !(this instanceof DonorListActivity)) {
                    navigateToActivity(DonorListActivity.class);
                }
                return true;
            } else if (itemId == R.id.nav_profile) {
                if (!(this instanceof ProfileActivity)) {
                    navigateToActivity(ProfileActivity.class);
                }
                return true;
            }
            return false;
        });

        if (this instanceof AdminMainActivity || this instanceof DonorMainActivity) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        } else if (this instanceof DonationSiteActivity) {
            bottomNav.setSelectedItemId(R.id.nav_sites);
        } else if (this instanceof DonorListActivity) {
            bottomNav.setSelectedItemId(R.id.nav_donor_list);
        } else if (this instanceof ProfileActivity) {
            bottomNav.setSelectedItemId(R.id.nav_profile);
        }
    }

    private void navigateToActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

}
