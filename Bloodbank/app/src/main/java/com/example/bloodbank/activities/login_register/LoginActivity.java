package com.example.bloodbank.activities.login_register;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bloodbank.R;
import com.example.bloodbank.activities.donors.DonorMainActivity;
import com.example.bloodbank.activities.ManagerMainActivity;
import com.example.bloodbank.activities.admin_super.AdminMainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER = "remember";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();

        EditText emailField = findViewById(R.id.username);
        EditText passwordField = findViewById(R.id.password);
        CheckBox rememberMeCheckBox = findViewById(R.id.rememberMeCheckBox);
        Button loginButton = findViewById(R.id.loginButton);
        TextView registerLink = findViewById(R.id.registerLink);

        if (sharedPreferences.getBoolean(KEY_REMEMBER, false)) {
            emailField.setText(sharedPreferences.getString(KEY_EMAIL, ""));
            passwordField.setText(sharedPreferences.getString(KEY_PASSWORD, ""));
            rememberMeCheckBox.setChecked(true);
        }

        loginButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String pass = passwordField.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields!", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                            String userId = mAuth.getCurrentUser().getUid();
                            db.collection("Users").document(userId).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            String role = documentSnapshot.getString("role");
                                            String name = documentSnapshot.getString("name");

                                            if (role == null || name == null) {
                                                Toast.makeText(this, "Incomplete user data in Firestore!", Toast.LENGTH_SHORT).show();
                                                return;
                                            }

                                            // Save user details
                                            editor.putString(KEY_EMAIL, email);
                                            editor.putString("USER_EMAIL", email);
                                            editor.putString("USER_NAME", name);
                                            editor.putString("USER_ROLE", role);
                                            editor.putString("USER_ID", userId);
                                            editor.putBoolean(KEY_REMEMBER, rememberMeCheckBox.isChecked());
                                            editor.apply();

                                            navigateToRoleActivity(role, name, userId);
                                        } else {
                                            Toast.makeText(this, "User data not found in Firestore!", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Error fetching user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(this, "Login failed: " +
                                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

        });

        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.loginLayout).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideKeyboard();
            }
            return false;
        });
    }

    private void navigateToRoleActivity(String role, String name, String userId) {
        Intent intent = null;

        switch (role) {
            case "donor":
                intent = new Intent(this, DonorMainActivity.class);
                break;
            case "manager":
                intent = new Intent(this, ManagerMainActivity.class);
                break;
            case "admin":
                intent = new Intent(this, AdminMainActivity.class);
                break;
            default:
                Toast.makeText(this, "Unknown role: " + role, Toast.LENGTH_SHORT).show();
                return;
        }

        if (intent != null) {
            intent.putExtra("USER_NAME", name);
            intent.putExtra("USER_ROLE", role);
            intent.putExtra("USER_ID", userId);

            startActivity(intent);
            finish();
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }
}
