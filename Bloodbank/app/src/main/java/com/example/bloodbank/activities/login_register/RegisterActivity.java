package com.example.bloodbank.activities.login_register;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bloodbank.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        EditText nameField = findViewById(R.id.username);
        EditText emailField = findViewById(R.id.email);
        EditText passwordField = findViewById(R.id.password);
        EditText confirmPasswordField = findViewById(R.id.confirmPassword);
        Button registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(v -> {
            String name = nameField.getText().toString().trim();
            String email = emailField.getText().toString().trim();
            String pass = passwordField.getText().toString().trim();
            String confirmPass = confirmPasswordField.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
            } else if (!pass.equals(confirmPass)) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
            } else {
                mAuth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                String userId = mAuth.getCurrentUser().getUid();

                                // Save user data to Firestore
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("name", name);
                                userData.put("email", email);
                                userData.put("role", "donor"); // Default role
                                userData.put("status", "approved");

                                db.collection("Users").document(userId)
                                        .set(userData)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        findViewById(R.id.registerLayout).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideKeyboard();
            }
            return false;
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }
}
