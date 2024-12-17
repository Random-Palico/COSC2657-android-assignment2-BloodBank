package com.example.bloodbank.activities.profile;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.bloodbank.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditUpdateProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private EditText nameField, dobField, locationField;
    private Spinner bloodTypeSpinner;
    private Button saveButton, chooseImageButton, dobPickerButton;
    private ImageView profileImageView;
    private Bitmap selectedImageBitmap;
    private Uri selectedImageUri;
    private Spinner locationSpinner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_update_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        initializeViews();
        setupBloodTypeSpinner();
        setupLocationSpinner();
        loadUserData();

        saveButton.setOnClickListener(v -> validateAndSaveProfile());
        dobPickerButton.setOnClickListener(v -> showDatePicker());
        chooseImageButton.setOnClickListener(v -> selectImage());
    }

    private void initializeViews() {
        nameField = findViewById(R.id.editNameField);
        dobField = findViewById(R.id.editDobField);
        locationSpinner = findViewById(R.id.editLocationSpinner);
        bloodTypeSpinner = findViewById(R.id.bloodTypeSpinner);
        saveButton = findViewById(R.id.saveButton);
        chooseImageButton = findViewById(R.id.chooseImageButton);
        dobPickerButton = findViewById(R.id.dobPickerButton);
        profileImageView = findViewById(R.id.profileImage);
    }

    private void setupBloodTypeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.blood_types,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bloodTypeSpinner.setAdapter(adapter);
    }

    private void setupLocationSpinner() {
        String[] countries = Locale.getISOCountries();
        String[] countryNames = Arrays.stream(countries)
                .map(code -> new Locale("", code).getDisplayCountry())
                .toArray(String[]::new);

        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, countryNames);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(locationAdapter);
    }


    private void loadUserData() {
        String userId = auth.getCurrentUser().getUid();

        String intentProfileImageUrl = getIntent().getStringExtra("profileImageUrl");
        if (intentProfileImageUrl != null && !intentProfileImageUrl.isEmpty()) {
            Glide.with(this).load(intentProfileImageUrl).into(profileImageView);
        }

        db.collection("Users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Map<String, Object> data = documentSnapshot.getData();

                nameField.setText((String) data.getOrDefault("name", ""));
                dobField.setText((String) data.getOrDefault("dob", ""));
                String location = (String) data.get("location");
                if (location != null) {
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) locationSpinner.getAdapter();
                    int position = adapter.getPosition(location);
                    if (position >= 0) locationSpinner.setSelection(position);
                }


                String bloodType = (String) data.get("bloodType");
                if (bloodType != null) {
                    ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) bloodTypeSpinner.getAdapter();
                    int position = adapter.getPosition(bloodType);
                    if (position >= 0) {
                        bloodTypeSpinner.setSelection(position);
                    }
                }

                String profileImageUrl = (String) data.get("profileImage");
                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    Glide.with(this).load(profileImageUrl).into(profileImageView);
                } else if (intentProfileImageUrl == null || intentProfileImageUrl.isEmpty()) {
                    profileImageView.setImageResource(R.drawable.ic_placeholder);
                }
            } else {
                Toast.makeText(this, "User data not found!", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void validateAndSaveProfile() {
        String dob = dobField.getText().toString();
        if (!isValidAge(dob)) {
            Toast.makeText(this, "Age must be 18 or higher!", Toast.LENGTH_SHORT).show();
            return;
        }
        saveProfileUpdates();
    }

    private boolean isValidAge(String dob) {
        try {
            String[] parts = dob.split("/");
            int year = Integer.parseInt(parts[2]);
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            return currentYear - year >= 18;
        } catch (Exception e) {
            return false;
        }
    }

    private void saveProfileUpdates() {
        String userId = auth.getCurrentUser().getUid();
        String selectedBloodType = bloodTypeSpinner.getSelectedItem().toString();
        String selectedLocation = locationSpinner.getSelectedItem().toString();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", nameField.getText().toString().trim());
        updates.put("dob", dobField.getText().toString().trim());
        updates.put("location", selectedLocation);
        updates.put("bloodType", selectedBloodType);

        db.collection("Users").document(userId).update(updates).addOnSuccessListener(aVoid -> {
            updateSharedPreferences(updates);
            if (selectedImageBitmap != null) {
                uploadProfileImage(userId);
            } else {
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private void updateSharedPreferences(Map<String, Object> updates) {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (updates.containsKey("name")) {
            editor.putString("USER_NAME", updates.get("name").toString());
        }
        editor.apply();
    }


    private void uploadProfileImage(String userId) {
        String path = "profile/" + userId + "/profile.jpg";
        StorageReference storageRef = storage.getReference(path);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        selectedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] data = baos.toByteArray();

        storageRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    db.collection("Users").document(userId).update("profileImage", uri.toString());
                    updateSharedPreferences(Collections.singletonMap("profileImage", uri.toString()));
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }))
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 100);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            if (!isValidAge(String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year))) {
                Toast.makeText(this, "Age must be 18 or higher!", Toast.LENGTH_SHORT).show();
            } else {
                calendar.set(year, month, dayOfMonth);
                dobField.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime()));
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            try {
                selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                profileImageView.setImageBitmap(selectedImageBitmap);
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
