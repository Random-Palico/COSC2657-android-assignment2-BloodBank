package com.example.bloodbank.activities.admin_super;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bloodbank.R;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddCampaignActivity extends AppCompatActivity {

    private EditText titleField, descriptionField;
    private Button dateButton, confirmButton, locationButton;
    private ImageButton imageButton;
    private ImageView selectedImageView;
    private TextView locationPreview;
    private Bitmap selectedImageBitmap;
    private String selectedDate, selectedShortName;
    private LatLng selectedLatLng;
    private Uri selectedImageUri;

    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_campaign);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        titleField = findViewById(R.id.titleField);
        descriptionField = findViewById(R.id.descriptionField);
        dateButton = findViewById(R.id.dateButton);
        confirmButton = findViewById(R.id.confirmButton);
        imageButton = findViewById(R.id.imageButton);
        selectedImageView = findViewById(R.id.selectedImageView);
        locationButton = findViewById(R.id.locationButton);
        locationPreview = findViewById(R.id.locationPreview);

        dateButton.setOnClickListener(v -> showDatePicker());

        ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        try {
                            selectedImageUri = result.getData().getData();
                            selectedImageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                            selectedImageView.setImageBitmap(selectedImageBitmap);
                            selectedImageView.setVisibility(View.VISIBLE);
                        } catch (Exception e) {
                            Toast.makeText(this, "Error selecting image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }
                });

        imageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        locationButton.setOnClickListener(v -> openMapPicker());

        confirmButton.setOnClickListener(v -> addCampaign());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            selectedDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime());
            dateButton.setText(selectedDate);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openMapPicker() {
        Intent intent = new Intent(this, MapPickerActivity.class);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            double lat = data.getDoubleExtra("latitude", 0);
            double lng = data.getDoubleExtra("longitude", 0);
            selectedShortName = data.getStringExtra("shortName");
            selectedLatLng = new LatLng(lat, lng);
            locationPreview.setText("Short Name: " + selectedShortName + "\nLat: " + lat + ", Lng: " + lng);
        }
    }

    private String[] getSelectedBloodTypes() {
        ArrayList<String> selectedTypes = new ArrayList<>();
        CheckBox[] checkBoxes = {
                findViewById(R.id.bloodAPlus),
                findViewById(R.id.bloodAMinus),
                findViewById(R.id.bloodBPlus),
                findViewById(R.id.bloodBMinus),
                findViewById(R.id.bloodOPlus),
                findViewById(R.id.bloodOMinus),
                findViewById(R.id.bloodABPlus),
                findViewById(R.id.bloodABMinus)
        };

        for (CheckBox checkBox : checkBoxes) {
            if (checkBox.isChecked()) {
                selectedTypes.add(checkBox.getText().toString());
            }
        }
        return selectedTypes.toArray(new String[0]);
    }

    private void addCampaign() {
        String title = titleField.getText().toString().trim();
        String description = descriptionField.getText().toString().trim();
        String[] requiredBloodTypes = getSelectedBloodTypes();

        if (title.isEmpty() || description.isEmpty() || selectedDate == null || selectedLatLng == null || selectedImageBitmap == null || requiredBloodTypes.length == 0) {
            Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadImageAndSaveCampaign(title, selectedShortName, description, requiredBloodTypes);
    }

    private void uploadImageAndSaveCampaign(String title, String shortName, String description, String[] bloodTypes) {
        String imagePath = "campaigns/" + System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = storage.getReference().child(imagePath);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        selectedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageData = baos.toByteArray();

        storageRef.putBytes(imageData)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    saveCampaignToFirestore(title, shortName, description, uri.toString(), bloodTypes);
                }))
                .addOnFailureListener(e -> Toast.makeText(this, "Error uploading image: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveCampaignToFirestore(String title, String shortName, String description, String imageUrl, String[] bloodTypes) {
        Map<String, Object> campaignData = new HashMap<>();
        campaignData.put("siteName", title);
        campaignData.put("shortName", shortName);
        campaignData.put("description", description);
        campaignData.put("locationLatLng", new HashMap<String, Double>() {{
            put("lat", selectedLatLng.latitude);
            put("lng", selectedLatLng.longitude);
        }});
        campaignData.put("eventDate", selectedDate);
        campaignData.put("eventImg", imageUrl);
        campaignData.put("requiredBloodTypes", bloodTypes); campaignData.put("requiredBloodTypes", new ArrayList<>(List.of(bloodTypes)));

        db.collection("DonationSites").add(campaignData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Campaign added successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error adding campaign: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
