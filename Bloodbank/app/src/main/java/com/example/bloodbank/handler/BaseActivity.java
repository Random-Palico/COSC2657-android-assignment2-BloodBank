package com.example.bloodbank.handler;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bloodbank.R;
import com.example.bloodbank.activities.DonationSiteActivity;
import com.example.bloodbank.activities.DonorManagement.DonorListActivity;
import com.example.bloodbank.activities.admin_super.AdminMainActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                if (!(this instanceof AdminMainActivity)) {
                    startActivity(new Intent(this, AdminMainActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                }
                return true;
            } else if (itemId == R.id.nav_sites) {
                if (!(this instanceof DonationSiteActivity)) {
                    startActivity(new Intent(this, DonationSiteActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                }
                return true;
            } else if (itemId == R.id.nav_donor_list) {
                if (!(this instanceof DonorListActivity)) {
                    startActivity(new Intent(this, DonorListActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                }
                return true;
            }

            return false;
        });

        if (this instanceof AdminMainActivity) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        } else if (this instanceof DonationSiteActivity) {
            bottomNav.setSelectedItemId(R.id.nav_sites);
        } else if (this instanceof DonorListActivity) {
            bottomNav.setSelectedItemId(R.id.nav_donor_list);
        }
    }
}
