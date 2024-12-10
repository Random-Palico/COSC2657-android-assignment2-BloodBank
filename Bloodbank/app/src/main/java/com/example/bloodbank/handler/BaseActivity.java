package com.example.bloodbank.handler;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bloodbank.activities.admin_super.AdminMainActivity;
import com.example.bloodbank.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                if (!(this instanceof AdminMainActivity)) {
                    startActivity(new Intent(this, AdminMainActivity.class));
                    overridePendingTransition(0, 0);
                }
                return true;
            }
            return false;
        });

        if (this instanceof AdminMainActivity) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }
}
