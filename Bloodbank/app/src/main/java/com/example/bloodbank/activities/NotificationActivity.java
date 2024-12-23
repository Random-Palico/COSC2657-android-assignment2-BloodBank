package com.example.bloodbank.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        db = FirebaseFirestore.getInstance();

        notificationContainer = findViewById(R.id.notificationContainer);
        notificationTabs = findViewById(R.id.notificationTabs);
        noNotificationsText = findViewById(R.id.noNotificationsText);

        receiverIds = Arrays.asList(getIntent().getStringArrayExtra("RECEIVER_IDS"));
        userRole = getIntent().getStringExtra("USER_ROLE");

        setupTabs();
        loadNotifications("all");
        checkPendingNotifications();
    }

    private void setupTabs() {
        notificationTabs.addTab(notificationTabs.newTab().setText("All"));
        notificationTabs.addTab(notificationTabs.newTab().setText("Read"));

        // Show pending tab only for admin
        if ("admin".equalsIgnoreCase(userRole)) {
            notificationTabs.addTab(notificationTabs.newTab().setText("Pending"));
        }

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

    private void checkPendingNotifications() {
        db.collection("RoleRequests")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Log.d(TAG, "Pending role requests available.");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to check pending notifications: ", e));
    }

    private void loadNotifications(String filter) {
        notificationContainer.removeAllViews();
        unreadNotificationIds.clear();

        if ("pending".equals(filter)) {
            loadPendingRequests();
        } else {
            db.collection("Notifications")
                    .whereIn("receiverId", receiverIds)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (querySnapshot == null || querySnapshot.isEmpty()) {
                            showNoNotificationsMessage();
                            return;
                        }

                        notificationContainer.setVisibility(View.VISIBLE);
                        noNotificationsText.setVisibility(View.GONE);

                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Map<String, Object> notification = doc.getData();
                            String status = (String) notification.get("status");

                            if ("unread".equals(status)) {
                                unreadNotificationIds.add(doc.getId());
                            }

                            addNotificationCard(notification, filter, status);
                        }

                        if (notificationContainer.getChildCount() == 0) {
                            showNoNotificationsMessage();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load notifications: ", e);
                        showNoNotificationsMessage();
                    });
        }
    }

    private void loadPendingRequests() {
        db.collection("RoleRequests")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        showNoNotificationsMessage();
                        return;
                    }

                    notificationContainer.setVisibility(View.VISIBLE);
                    noNotificationsText.setVisibility(View.GONE);

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String userId = doc.getString("userId");

                        db.collection("Users").document(userId).get()
                                .addOnSuccessListener(userDoc -> {
                                    String userName = userDoc.exists() ? userDoc.getString("name") : "Unknown User";
                                    addPendingRequestCard(doc, userName);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error fetching user data: ", e);
                                    addPendingRequestCard(doc, "Error fetching user");
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching pending requests: ", e);
                    showNoNotificationsMessage();
                });
    }

    private void addPendingRequestCard(DocumentSnapshot roleRequest, String userName) {
        View pendingCard = getLayoutInflater().inflate(R.layout.pending_card, null);

        TextView requestText = pendingCard.findViewById(R.id.userName);
        Button acceptButton = pendingCard.findViewById(R.id.acceptButton);
        Button declineButton = pendingCard.findViewById(R.id.declineButton);

        String roleId = roleRequest.getId();
        requestText.setText(userName + " has requested to be a Manager");

        acceptButton.setOnClickListener(v -> {
            db.collection("Users").document(roleRequest.getString("userId"))
                    .update("role", "manager")
                    .addOnSuccessListener(aVoid -> {
                        db.collection("RoleRequests").document(roleId)
                                .update("status", "approved");
                        Toast.makeText(this, "Request approved!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error approving request: ", e));
        });

        declineButton.setOnClickListener(v -> {
            db.collection("RoleRequests").document(roleId)
                    .update("status", "declined")
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Request declined.", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Log.e(TAG, "Error declining request: ", e));
        });

        notificationContainer.addView(pendingCard);
    }

    private void addNotificationCard(Map<String, Object> notification, String currentFilter, String status) {
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

    private void showNoNotificationsMessage() {
        notificationContainer.setVisibility(View.GONE);
        noNotificationsText.setVisibility(View.VISIBLE);
        noNotificationsText.setText("No notifications to display.");
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
                .addOnFailureListener(e -> Log.e(TAG, "Error marking notifications as read: ", e));
    }
}
