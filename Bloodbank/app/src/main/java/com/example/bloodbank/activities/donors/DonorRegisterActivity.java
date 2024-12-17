package com.example.bloodbank.activities.donors;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bloodbank.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DonorRegisterActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText nameField, dobField;
    private Spinner locationSpinner, bloodTypeSpinner;
    private SeekBar bloodUnitSlider;
    private TextView bloodUnitValue;
    private Button confirmButton, dobPickerButton;

    private String profileImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donor_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupSpinners();
        setupDatePicker();
        loadUserProfile();

        confirmButton.setOnClickListener(v -> validateAndRegisterDonor());
    }

    private void initializeViews() {
        nameField = findViewById(R.id.editNameField);
        dobField = findViewById(R.id.editDobField);
        locationSpinner = findViewById(R.id.editLocationSpinner);
        bloodTypeSpinner = findViewById(R.id.bloodTypeSpinner);
        bloodUnitSlider = findViewById(R.id.bloodUnitSlider);
        bloodUnitValue = findViewById(R.id.bloodUnitValue);
        confirmButton = findViewById(R.id.confirmButton);
        dobPickerButton = findViewById(R.id.dobPickerButton);

        bloodUnitSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                bloodUnitValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void setupSpinners() {
        String[] countries = Locale.getISOCountries();
        String[] countryNames = Arrays.stream(countries)
                .map(code -> new Locale("", code).getDisplayCountry())
                .toArray(String[]::new);

        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, countryNames);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(locationAdapter);

        ArrayAdapter<CharSequence> bloodTypeAdapter = ArrayAdapter.createFromResource(this, R.array.blood_types, android.R.layout.simple_spinner_item);
        bloodTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bloodTypeSpinner.setAdapter(bloodTypeAdapter);
    }

    private void setupDatePicker() {
        dobPickerButton.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                String selectedDob = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
                if (!isValidAge(selectedDob)) {
                    Toast.makeText(this, "Age must be 18 or higher!", Toast.LENGTH_SHORT).show();
                } else {
                    dobField.setText(selectedDob);
                }
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void loadUserProfile() {
        String userId = auth.getCurrentUser().getUid();
        db.collection("Users").document(userId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Map<String, Object> data = snapshot.getData();

                String name = (String) data.get("name");
                String dob = (String) data.get("dob");
                String location = (String) data.get("location");
                String bloodType = (String) data.get("bloodType");
                profileImageUrl = (String) data.get("profileImage");

                // disable field if data exists
                if (name != null && !name.isEmpty()) {
                    nameField.setText(name);
                    nameField.setEnabled(false);
                }

                if (dob != null && !dob.isEmpty()) {
                    dobField.setText(dob);
                    dobField.setEnabled(false);
                    dobPickerButton.setEnabled(false);
                }

                // disable drop down if data exists
                if (location != null && !location.isEmpty()) {
                    ArrayAdapter<String> locationAdapter = (ArrayAdapter<String>) locationSpinner.getAdapter();
                    int locationIndex = locationAdapter.getPosition(location);
                    if (locationIndex >= 0) {
                        locationSpinner.setSelection(locationIndex);
                        locationSpinner.setEnabled(false);
                    }
                }

                if (bloodType != null && !bloodType.isEmpty()) {
                    ArrayAdapter<CharSequence> bloodTypeAdapter = (ArrayAdapter<CharSequence>) bloodTypeSpinner.getAdapter();
                    int bloodTypeIndex = bloodTypeAdapter.getPosition(bloodType);
                    if (bloodTypeIndex >= 0) {
                        bloodTypeSpinner.setSelection(bloodTypeIndex);
                        bloodTypeSpinner.setEnabled(false);
                    }
                }
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show());
    }

    private boolean isValidAge(String dob) {
        if (dob == null || dob.isEmpty()) {
            return false;
        }

        try {
            String[] parts = dob.split("/");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            int year = Integer.parseInt(parts[2]);

            Calendar birthDate = Calendar.getInstance();
            birthDate.set(year, month, day);

            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR);

            if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }

            return age >= 18;
        } catch (Exception e) {
            return false;
        }
    }

    private void validateAndRegisterDonor() {
        int bloodUnits = bloodUnitSlider.getProgress();
        if (bloodUnits <= 0) {
            Toast.makeText(this, "Please select a valid blood unit!", Toast.LENGTH_SHORT).show();
            return;
        }

        String campaignId = getIntent().getStringExtra("campaignId");
        if (campaignId == null || campaignId.isEmpty()) {
            Toast.makeText(this, "Campaign ID is missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        Map<String, Object> donorData = new HashMap<>();
        donorData.put("userId", userId);
        donorData.put("name", nameField.getText().toString().trim());
        donorData.put("dob", dobField.getText().toString().trim());
        donorData.put("location", locationSpinner.getSelectedItem().toString());
        donorData.put("bloodType", bloodTypeSpinner.getSelectedItem().toString());
        donorData.put("bloodUnit", bloodUnits);
        donorData.put("campaignId", campaignId);
        donorData.put("profileImage", profileImageUrl != null ? profileImageUrl : "placeholder_url");

        db.collection("Donors").document(userId).set(donorData).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
