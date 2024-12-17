package com.example.bloodbank.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bloodbank.R;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class NotificationActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout notificationContainer;
    private TabLayout notificationTabs;
    private TextView noNotificationsText;

    private String currentUserId;
    private String currentUserRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        db = FirebaseFirestore.getInstance();

        notificationContainer = findViewById(R.id.notificationContainer);
        notificationTabs = findViewById(R.id.notificationTabs);
        noNotificationsText = findViewById(R.id.noNotificationsText);

        currentUserId = getIntent().getStringExtra("USER_ID");
        currentUserRole = getIntent().getStringExtra("USER_ROLE");

        setupTabs();
        loadNotifications("all");
    }


    private void setupTabs() {
        notificationTabs.addTab(notificationTabs.newTab().setText("All"));
        notificationTabs.addTab(notificationTabs.newTab().setText("Read"));
        notificationTabs.addTab(notificationTabs.newTab().setText("Unread"));
        notificationTabs.addTab(notificationTabs.newTab().setText("Pending"));

        notificationTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String selectedTab = tab.getText().toString().toLowerCase();
                loadNotifications(selectedTab);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void loadNotifications(String filter) {
        notificationContainer.removeAllViews();
        db.collection("Notifications")
                .whereEqualTo("receiverId", currentUserId)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        noNotificationsText.setVisibility(View.VISIBLE);
                        noNotificationsText.setText("Failed to load notifications.");
                        return;
                    }

                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        noNotificationsText.setVisibility(View.GONE);
                        notificationContainer.setVisibility(View.VISIBLE);

                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Map<String, Object> notification = doc.getData();

                            if (filter.equals("all") || filter.equals(notification.get("status"))) {
                                addNotificationCard(notification);
                            }
                        }
                    } else {
                        noNotificationsText.setVisibility(View.VISIBLE);
                        notificationContainer.setVisibility(View.GONE);
                    }
                });
    }


    private void addNotificationCard(Map<String, Object> notification) {
        View notificationCard = getLayoutInflater().inflate(R.layout.notification_card, null);

        TextView notificationText = notificationCard.findViewById(R.id.notificationText);
        TextView timestamp = notificationCard.findViewById(R.id.timestamp);

        notificationText.setText((String) notification.get("message"));

        long timestampMillis = (long) notification.get("timestamp");
        String formattedTimestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(timestampMillis));
        timestamp.setText(formattedTimestamp);

        notificationContainer.addView(notificationCard);
    }
}
