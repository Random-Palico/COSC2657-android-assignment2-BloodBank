package com.example.bloodbank.activities.add_edit_campaign;

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

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.bloodbank.R;
import com.example.bloodbank.activities.MapPickerActivity;
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

public class EditCampaignActivity extends AppCompatActivity {

    private EditText titleField, descriptionField;
    private Button dateButton, confirmButton, locationButton;
    private ImageButton imageButton;
    private ImageView selectedImageView;
    private TextView locationPreview;
    private Bitmap selectedImageBitmap;
    private String selectedDate, selectedShortName, selectedAddress;
    private LatLng selectedLatLng;
    private Uri selectedImageUri;

    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private String campaignId;
    private String existingImageUrl;
    private ArrayList<String> requiredBloodTypes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_campaign);

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
        ImageButton backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());

        Intent intent = getIntent();
        campaignId = intent.getStringExtra("campaignId");
        titleField.setText(intent.getStringExtra("campaignTitle"));
        descriptionField.setText(intent.getStringExtra("campaignDescription"));
        selectedDate = intent.getStringExtra("campaignDate");
        existingImageUrl = intent.getStringExtra("campaignImage");
        selectedShortName = intent.getStringExtra("campaignLocation");
        selectedAddress = intent.getStringExtra("campaignAddress");
        double lat = intent.getDoubleExtra("latitude", 0);
        double lng = intent.getDoubleExtra("longitude", 0);
        selectedLatLng = new LatLng(lat, lng);
        requiredBloodTypes = intent.getStringArrayListExtra("requiredBloodTypes");

        dateButton.setText(selectedDate);
        locationPreview.setText("Short Name: " + selectedShortName + "\nAddress: " + selectedAddress + "\nLat: " + lat + ", Lng: " + lng);
        if (existingImageUrl != null) {
            Glide.with(this).load(existingImageUrl).into(selectedImageView);
            selectedImageView.setVisibility(View.VISIBLE);
        }
        preselectBloodTypes();

        dateButton.setOnClickListener(v -> showDatePicker());
        locationButton.setOnClickListener(v -> openMapPicker());
        imageButton.setOnClickListener(v -> selectImage());
        confirmButton.setOnClickListener(v -> updateCampaign());
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

    private void selectImage() {
        Intent intentImage = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intentImage, 200);
    }

    private void preselectBloodTypes() {
        CheckBox[] checkBoxes = {findViewById(R.id.bloodAPlus), findViewById(R.id.bloodAMinus), findViewById(R.id.bloodBPlus), findViewById(R.id.bloodBMinus), findViewById(R.id.bloodOPlus), findViewById(R.id.bloodOMinus), findViewById(R.id.bloodABPlus), findViewById(R.id.bloodABMinus)};

        if (requiredBloodTypes != null) {
            for (CheckBox checkBox : checkBoxes) {
                if (requiredBloodTypes.contains(checkBox.getText().toString())) {
                    checkBox.setChecked(true);
                }
            }
        }
    }

    private String[] getSelectedBloodTypes() {
        ArrayList<String> selectedTypes = new ArrayList<>();
        CheckBox[] checkBoxes = {findViewById(R.id.bloodAPlus), findViewById(R.id.bloodAMinus), findViewById(R.id.bloodBPlus), findViewById(R.id.bloodBMinus), findViewById(R.id.bloodOPlus), findViewById(R.id.bloodOMinus), findViewById(R.id.bloodABPlus), findViewById(R.id.bloodABMinus)};

        for (CheckBox checkBox : checkBoxes) {
            if (checkBox.isChecked()) {
                selectedTypes.add(checkBox.getText().toString());
            }
        }
        return selectedTypes.toArray(new String[0]);
    }

    private void updateCampaign() {
        String title = titleField.getText().toString().trim();
        String description = descriptionField.getText().toString().trim();
        String[] requiredBloodTypes = getSelectedBloodTypes();

        if (title.isEmpty() || description.isEmpty() || selectedDate == null || selectedShortName == null || selectedAddress == null || requiredBloodTypes.length == 0) {
            Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri != null) {
            uploadImageAndSaveCampaign(title, description, requiredBloodTypes);
        } else {
            saveCampaignToFirestore(title, description, existingImageUrl, requiredBloodTypes);
        }
    }

    private void uploadImageAndSaveCampaign(String title, String description, String[] bloodTypes) {
        String imagePath = "campaigns/" + System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = storage.getReference().child(imagePath);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        selectedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageData = baos.toByteArray();

        storageRef.putBytes(imageData).addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            saveCampaignToFirestore(title, description, uri.toString(), bloodTypes);
        })).addOnFailureListener(e -> Toast.makeText(this, "Error uploading image: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveCampaignToFirestore(String title, String description, String imageUrl, String[] bloodTypes) {
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("siteName", title);
        updatedData.put("description", description);
        updatedData.put("eventDate", selectedDate);
        updatedData.put("eventImg", imageUrl);
        updatedData.put("requiredBloodTypes", new ArrayList<>(List.of(bloodTypes)));
        updatedData.put("shortName", selectedShortName);
        updatedData.put("address", selectedAddress);
        updatedData.put("locationLatLng", new HashMap<String, Double>() {{
            put("lat", selectedLatLng.latitude);
            put("lng", selectedLatLng.longitude);
        }});

        db.collection("DonationSites").document(campaignId).get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                Toast.makeText(this, "Campaign not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Compare current data with new data
            Map<String, Object> existingData = snapshot.getData();
            List<String> updatedFields = new ArrayList<>();

            if (!title.equals(existingData.get("siteName"))) {
                updatedFields.add("Title");
            }
            if (!description.equals(existingData.get("description"))) {
                updatedFields.add("Description");
            }
            if (!selectedDate.equals(existingData.get("eventDate"))) {
                updatedFields.add("Event Date");
            }
            if (!selectedShortName.equals(existingData.get("shortName")) || !selectedAddress.equals(existingData.get("address"))) {
                updatedFields.add("Location");
            }
            if (!List.of(bloodTypes).equals(existingData.get("requiredBloodTypes"))) {
                updatedFields.add("Required Blood Types");
            }

            // Update the campaign
            db.collection("DonationSites").document(campaignId).update(updatedData).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Campaign updated successfully!", Toast.LENGTH_SHORT).show();
                sendUpdateNotifications(title, updatedFields);
                finish();
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Error updating campaign: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void sendUpdateNotifications(String title, List<String> updatedFields) {
        StringBuilder combinedMessage = new StringBuilder(title + " has been updated. Changes include: ");

        for (String field : updatedFields) {
            switch (field) {
                case "Location":
                    combinedMessage.append("\n- Location updated to ").append(selectedShortName).append(" at ").append(selectedAddress).append(".");
                    break;

                case "Title":
                    combinedMessage.append("\n- New title: ").append(title).append(".");
                    break;

                case "Description":
                    combinedMessage.append("\n- Description updated.");
                    break;

                case "Event Date":
                    combinedMessage.append("\n- Event date changed to ").append(selectedDate).append(".");
                    break;

                case "Required Blood Types":
                    combinedMessage.append("\n- Required blood types updated to ").append(String.join(", ", getSelectedBloodTypes())).append(".");
                    break;

                default:
                    combinedMessage.append("\n- ").append(field).append(" updated.");
            }
        }

        // Create combined notification
        Map<String, Object> notification = new HashMap<>();
        notification.put("receiverId", "all");
        notification.put("message", combinedMessage.toString());
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("status", "unread");
        notification.put("type", "campaign_update");

        db.collection("Notifications").add(notification).addOnSuccessListener(documentReference -> {
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to send notification: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            double lat = data.getDoubleExtra("latitude", 0);
            double lng = data.getDoubleExtra("longitude", 0);
            selectedShortName = data.getStringExtra("shortName");
            selectedAddress = data.getStringExtra("address");
            selectedLatLng = new LatLng(lat, lng);
            locationPreview.setText("Short Name: " + selectedShortName + "\nAddress: " + selectedAddress + "\nLat: " + lat + ", Lng: " + lng);
        } else if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            try {
                selectedImageUri = data.getData();
                selectedImageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                selectedImageView.setImageBitmap(selectedImageBitmap);
                selectedImageView.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Toast.makeText(this, "Error selecting image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }
}
