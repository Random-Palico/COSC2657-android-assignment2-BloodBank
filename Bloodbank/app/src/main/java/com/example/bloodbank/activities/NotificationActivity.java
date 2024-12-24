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

        if ("pending".equalsIgnoreCase(filter)) {
            loadPendingRequests();
            return;
        }

        String[] receiverIdsFromIntent = getIntent().getStringArrayExtra("RECEIVER_IDS");
        if (receiverIdsFromIntent == null || receiverIdsFromIntent.length == 0) {
            Log.e(TAG, "RECEIVER_IDS is null or empty. Ensure it is passed in the intent.");
            showNoNotificationsMessage();
            return;
        }

        List<String> validReceiverIds = Arrays.asList(receiverIdsFromIntent);
        Log.d(TAG, "Valid receiver IDs: " + validReceiverIds);

        db.collection("Notifications")
                .whereIn("receiverId", validReceiverIds)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        Log.d(TAG, "No notifications found for receiver IDs: " + validReceiverIds);
                        showNoNotificationsMessage();
                        return;
                    }

                    notificationContainer.setVisibility(View.VISIBLE);
                    noNotificationsText.setVisibility(View.GONE);
                    Log.d(TAG, "Fetched " + querySnapshot.size() + " notifications");

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> notification = doc.getData();
                        if (notification == null) continue;

                        Log.d(TAG, "Notification data: " + notification);
                        String status = (String) notification.get("status");

                        if ("unread".equals(status)) {
                            unreadNotificationIds.add(doc.getId());
                        }

                        addNotificationCard(notification, filter, status);
                    }

                    if (!unreadNotificationIds.isEmpty()) {
                        markNotificationsAsRead();
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


    private void loadPendingRequests() {
        if (!"admin".equalsIgnoreCase(userRole)) {
            showNoNotificationsMessage();
            return;
        }

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
                                .update("status", "approved")
                                .addOnSuccessListener(aVoid1 -> {
                                    sendNotificationToUser(roleRequest.getString("userId"), "Your role request has been approved.");
                                    updatePendingCardUI(pendingCard, "You have approved " + userName + " to be a Site Manager.");
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Error updating role request status: ", e));
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error approving request: ", e));
        });

        declineButton.setOnClickListener(v -> {
            db.collection("RoleRequests").document(roleId)
                    .update("status", "declined")
                    .addOnSuccessListener(aVoid -> {
                        sendNotificationToUser(roleRequest.getString("userId"), "Your role request has been declined.");

                        updatePendingCardUI(pendingCard, "You have declined " + userName + "'s request.");
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error declining request: ", e));
        });

        notificationContainer.addView(pendingCard);
    }

    private void sendNotificationToUser(String userId, String message) {
        Map<String, Object> notificationData = Map.of(
                "receiverId", userId,
                "message", message,
                "timestamp", System.currentTimeMillis(),
                "status", "unread",
                "type", "roleRequest"
        );

        db.collection("Notifications")
                .add(notificationData)
                .addOnSuccessListener(docRef -> Log.d(TAG, "Notification sent to user: " + userId))
                .addOnFailureListener(e -> Log.e(TAG, "Error sending notification: ", e));
    }

    private void updatePendingCardUI(View pendingCard, String message) {
        pendingCard.setBackgroundResource(R.drawable.border_card);

        TextView requestText = pendingCard.findViewById(R.id.userName);
        Button acceptButton = pendingCard.findViewById(R.id.acceptButton);
        Button declineButton = pendingCard.findViewById(R.id.declineButton);

        requestText.setText(message);
        requestText.setTextColor(Color.GRAY);

        acceptButton.setVisibility(View.GONE);
        declineButton.setVisibility(View.GONE);
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
        if (unreadNotificationIds.isEmpty()) return;

        for (String notificationId : unreadNotificationIds) {
            db.collection("Notifications").document(notificationId)
                    .update("status", "read")
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification marked as read: " + notificationId))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to mark notification as read: " + notificationId, e));
        }
        unreadNotificationIds.clear();
    }
}
