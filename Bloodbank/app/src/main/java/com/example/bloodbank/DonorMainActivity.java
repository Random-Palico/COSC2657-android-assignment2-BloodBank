package com.example.bloodbank;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DonorMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_donor);

        TextView welcomeUser = findViewById(R.id.welcomeUser);
        String userName = getIntent().getStringExtra("USER_NAME");
        welcomeUser.setText("Hi " + userName);
    }
}
