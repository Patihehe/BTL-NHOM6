package com.example.btl_nhom6;

import android.content.Intent;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView rvRecentChats;
    private TextView tvNoChats;
    private RecentChatAdapter adapter;
    private List<User> chatUsers;
    private FirebaseFirestore db;
    private String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        db = FirebaseFirestore.getInstance();
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
        // Lấy toàn bộ tin nhắn liên quan đến người dùng hiện tại để tìm danh sách người đã chat
        db.collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        Set<String> emails = new HashSet<>();
                        for (QueryDocumentSnapshot doc : value) {
                            Message msg = doc.toObject(Message.class);
                            if (msg.getSenderEmail().equals(currentUserEmail)) {
                                emails.add(msg.getReceiverEmail());
                            } else if (msg.getReceiverEmail().equals(currentUserEmail)) {
                                emails.add(msg.getSenderEmail());
                            }
                        }
                        
                        chatUsers.clear();
                        if (emails.isEmpty()) {
                            tvNoChats.setVisibility(View.VISIBLE);
                            adapter.notifyDataSetChanged();
                        } else {
                            tvNoChats.setVisibility(View.GONE);
                            // Query thông tin từng user từ email
                            for (String email : emails) {
                                db.collection("users")
                                        .whereEqualTo("email", email)
                                        .get()
                                        .addOnSuccessListener(queryDocumentSnapshots -> {
                                            for (QueryDocumentSnapshot userDoc : queryDocumentSnapshots) {
                                                chatUsers.add(userDoc.toObject(User.class));
                                            }
                                            adapter.notifyDataSetChanged();
                                        });
                            }
                        }
                    }
                });
    }
}
