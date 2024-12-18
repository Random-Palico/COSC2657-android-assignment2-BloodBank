package com.example.bloodbank.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bloodbank.R;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = "NotificationActivity";

    private FirebaseFirestore db;
    private LinearLayout notificationContainer;
    private TabLayout notificationTabs;
    private TextView noNotificationsText;

    private ArrayList<String> unreadNotificationIds = new ArrayList<>();
    private List<String> receiverIds;
    private String currentTab = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        db = FirebaseFirestore.getInstance();

        notificationContainer = findViewById(R.id.notificationContainer);
        notificationTabs = findViewById(R.id.notificationTabs);
        noNotificationsText = findViewById(R.id.noNotificationsText);

        receiverIds = Arrays.asList(getIntent().getStringArrayExtra("RECEIVER_IDS"));

        setupTabs();
        loadNotifications("all");
    }

    private void setupTabs() {
        notificationTabs.addTab(notificationTabs.newTab().setText("All"));
        notificationTabs.addTab(notificationTabs.newTab().setText("Read"));
        notificationTabs.addTab(notificationTabs.newTab().setText("Pending"));

        notificationTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getText().toString().toLowerCase();
                loadNotifications(currentTab);
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
        unreadNotificationIds.clear();

        db.collection("Notifications")
                .whereIn("receiverId", receiverIds)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Failed to load notifications: ", e);
                        noNotificationsText.setVisibility(View.VISIBLE);
                        noNotificationsText.setText("Failed to load notifications.");
                        return;
                    }

                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        noNotificationsText.setVisibility(View.GONE);
                        notificationContainer.setVisibility(View.VISIBLE);

                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Map<String, Object> notification = doc.getData();
                            String status = (String) notification.get("status");
                            String id = doc.getId();

                            if ("unread".equals(status)) {
                                unreadNotificationIds.add(id);
                            }

                            if ("all".equals(filter) || (filter.equals("read") && "read".equals(status))
                                    || (filter.equals("pending") && "pending".equals(status))) {
                                addNotificationCard(notification, status, filter);
                            }
                        }

                        if (notificationContainer.getChildCount() == 0) {
                            noNotificationsText.setVisibility(View.VISIBLE);
                            noNotificationsText.setText("No notifications to display.");
                        }
                    } else {
                        noNotificationsText.setVisibility(View.VISIBLE);
                        noNotificationsText.setText("No notifications to display.");
                        notificationContainer.setVisibility(View.GONE);
                    }
                });
    }

    private void addNotificationCard(Map<String, Object> notification, String status, String currentFilter) {
        View notificationCard = getLayoutInflater().inflate(R.layout.notification_card, null);

        TextView notificationText = notificationCard.findViewById(R.id.notificationText);
        TextView timestamp = notificationCard.findViewById(R.id.timestamp);

        notificationText.setText((String) notification.get("message"));

        long timestampMillis = (long) notification.get("timestamp");
        String formattedTimestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(timestampMillis));
        timestamp.setText(formattedTimestamp);

        if ("all".equals(currentFilter) && "read".equals(status)) {
            notificationCard.setBackgroundResource(R.drawable.border_card);
            notificationText.setTextColor(Color.GRAY);
        } else {
            notificationCard.setBackgroundResource(R.drawable.border_card);
            notificationText.setTextColor(Color.BLACK);
        }

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(16, 16, 16, 16);
        notificationCard.setLayoutParams(layoutParams);

        notificationContainer.addView(notificationCard);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        markNotificationsAsRead();
    }

    private void markNotificationsAsRead() {
        db.runTransaction(transaction -> {
                    for (String notificationId : unreadNotificationIds) {
                        DocumentSnapshot snapshot = transaction.get(db.collection("Notifications").document(notificationId));
                        if ("unread".equals(snapshot.getString("status"))) {
                            transaction.update(snapshot.getReference(), "status", "read");
                        }
                    }
                    return null;
                }).addOnSuccessListener(aVoid -> Log.d(TAG, "Notifications marked as read."))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update notifications: ", e));
    }
}
