package com.example.btl_nhom6;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView rvRecentChats;
    private TextView tvNoChats;
    private RecentChatAdapter adapter;
    private List<User> chatUsers;
    private AppDatabase db;
    private String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        db = AppDatabase.getInstance(this);
        SharedPreferences pref = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        currentUserEmail = pref.getString("current_user_email", "");

        rvRecentChats = findViewById(R.id.rvRecentChats);
        tvNoChats = findViewById(R.id.tvNoChats);

        chatUsers = new ArrayList<>();
        adapter = new RecentChatAdapter(chatUsers, user -> {
            Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
            intent.putExtra("receiver_email", user.getEmail());
            intent.putExtra("receiver_name", user.getFullName());
            startActivity(intent);
        });

        rvRecentChats.setLayoutManager(new LinearLayoutManager(this));
        rvRecentChats.setAdapter(adapter);

        loadRecentChats();
    }

    private void loadRecentChats() {
        chatUsers.clear();
        // Lấy danh sách email những người đã nhắn tin
        List<String> emails = db.messageDao().getRecentChatUsers(currentUserEmail);
        
        for (String email : emails) {
            User user = db.userDao().getUserByEmail(email);
            if (user != null) {
                chatUsers.add(user);
            }
        }

        adapter.notifyDataSetChanged();

        if (chatUsers.isEmpty()) {
            tvNoChats.setVisibility(View.VISIBLE);
        } else {
            tvNoChats.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecentChats();
    }
}
