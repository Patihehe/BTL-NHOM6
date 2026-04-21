package com.example.btl_nhom6;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private TextView tvNoNotif;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private AppDatabase db;
    private String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        db = AppDatabase.getInstance(this);
        SharedPreferences pref = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        currentUserEmail = pref.getString("current_user_email", "");

        rvNotifications = findViewById(R.id.rvNotifications);
        tvNoNotif = findViewById(R.id.tvNoNotif);

        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        loadNotifications();
        markAllAsRead();
    }

    private void loadNotifications() {
        notificationList.clear();
        notificationList.addAll(db.notificationDao().getNotificationsForUser(currentUserEmail));
        adapter.notifyDataSetChanged();

        if (notificationList.isEmpty()) {
            tvNoNotif.setVisibility(View.VISIBLE);
        } else {
            tvNoNotif.setVisibility(View.GONE);
        }
    }

    private void markAllAsRead() {
        for (Notification n : notificationList) {
            if (!n.isRead()) {
                n.setRead(true);
                db.notificationDao().updateNotification(n);
            }
        }
    }
}
