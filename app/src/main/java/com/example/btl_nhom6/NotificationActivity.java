package com.example.btl_nhom6;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private TextView tvNoNotif;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private FirebaseFirestore db; // Dùng FirebaseFirestore
    private String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        db = FirebaseFirestore.getInstance();
        SharedPreferences pref = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        currentUserEmail = pref.getString("current_user_email", "");

        rvNotifications = findViewById(R.id.rvNotifications);
        tvNoNotif = findViewById(R.id.tvNoNotif);

        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        listenForNotifications();
    }

    private void listenForNotifications() {
        db.collection("notifications")
                .whereEqualTo("userEmail", currentUserEmail)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        notificationList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Notification notif = doc.toObject(Notification.class);
                            notif.setId(doc.getId());
                            notificationList.add(notif);
                        }
                        adapter.notifyDataSetChanged();

                        if (notificationList.isEmpty()) {
                            tvNoNotif.setVisibility(View.VISIBLE);
                        } else {
                            tvNoNotif.setVisibility(View.GONE);
                        }
                        
                        // Sau khi load xong, đánh dấu tất cả là đã đọc trên Server
                        markAllAsRead();
                    }
                });
    }

    private void markAllAsRead() {
        for (Notification n : notificationList) {
            if (!n.isRead()) {
                db.collection("notifications").document(n.getId()).update("isRead", true);
            }
        }
    }
}
