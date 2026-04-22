package com.example.btl_nhom6;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SocialActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private LinearLayout llSearch;
    private EditText etSearch;
    private Button btnSearch;
    private RecyclerView recyclerViewSocial;
    private UserAdapter userAdapter;
    private List<User> displayList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId;
    private String currentUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social);

        db = FirebaseFirestore.getInstance();
        SharedPreferences pref = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        currentUserId = pref.getString("current_user_id", "");
        currentUserName = pref.getString("current_user_name", "Ai đó");

        if (currentUserId.isEmpty()) {
            finish();
            return;
        }

        tabLayout = findViewById(R.id.tabLayout);
        llSearch = findViewById(R.id.llSearch);
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        recyclerViewSocial = findViewById(R.id.recyclerViewSocial);

        recyclerViewSocial.setLayoutManager(new LinearLayoutManager(this));
        setupAdapter();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                refreshContent();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) searchUsers(query);
        });

        refreshContent();
    }

    private void setupAdapter() {
        userAdapter = new UserAdapter(displayList, currentUserId, db, new UserAdapter.OnUserActionListener() {
            @Override
            public void onAction(User user) {
                handleUserAction(user);
            }

            @Override
            public void onDecline(User user) {
                handleDeclineAction(user);
            }

            @Override
            public void onUserClick(User user) {
                Intent intent = new Intent(SocialActivity.this, ProfileActivity.class);
                intent.putExtra("profile_user_id", user.getId());
                startActivity(intent);
            }
        });
        recyclerViewSocial.setAdapter(userAdapter);
    }

    private void refreshContent() {
        int position = tabLayout.getSelectedTabPosition();
        displayList.clear();
        userAdapter.notifyDataSetChanged();
        etSearch.setText("");

        if (position == 0) {
            llSearch.setVisibility(View.VISIBLE);
        } else if (position == 1) {
            llSearch.setVisibility(View.GONE);
            loadPendingRequests();
        } else if (position == 2) {
            llSearch.setVisibility(View.GONE);
            loadFriendsList();
        }
    }

    private void searchUsers(String query) {
        db.collection("users")
                .orderBy("fullName")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    displayList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        if (user.getId() != null && !user.getId().equals(currentUserId)) {
                            displayList.add(user);
                        }
                    }
                    userAdapter.notifyDataSetChanged();
                });
    }

    private void loadPendingRequests() {
        db.collection("friendships")
                .whereEqualTo("friendId", currentUserId)
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    displayList.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        userAdapter.notifyDataSetChanged();
                        return;
                    }
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String requesterId = doc.getString("userId");
                        if (requesterId != null) {
                            db.collection("users").document(requesterId).get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            displayList.add(userDoc.toObject(User.class));
                                            userAdapter.notifyDataSetChanged();
                                        }
                                    });
                        }
                    }
                });
    }

    private void loadFriendsList() {
        db.collection("friendships")
                .whereEqualTo("status", "ACCEPTED")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    displayList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String uid = doc.getString("userId");
                        String fid = doc.getString("friendId");
                        String friendId = currentUserId.equals(uid) ? fid : (currentUserId.equals(fid) ? uid : null);
                        
                        if (friendId != null) {
                            db.collection("users").document(friendId).get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            User friend = userDoc.toObject(User.class);
                                            displayList.add(friend);
                                            userAdapter.notifyDataSetChanged();
                                        }
                                    });
                        }
                    }
                });
    }

    private void handleUserAction(User user) {
        db.collection("friendships")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    String friendshipId = null;
                    String status = null;
                    String initiatorId = null;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String uid = doc.getString("userId");
                        String fid = doc.getString("friendId");
                        if (uid == null || fid == null) continue;

                        if ((uid.equals(currentUserId) && fid.equals(user.getId())) ||
                            (uid.equals(user.getId()) && fid.equals(currentUserId))) {
                            friendshipId = doc.getId();
                            status = doc.getString("status");
                            initiatorId = uid;
                            break;
                        }
                    }

                    if (status == null) {
                        sendFriendRequest(user);
                    } else if (status.equals("PENDING")) {
                        if (!currentUserId.equals(initiatorId)) {
                            acceptFriendRequest(friendshipId, user);
                        }
                    } else if (status.equals("ACCEPTED")) {
                        unfriend(friendshipId, user);
                    }
                });
    }

    private void sendFriendRequest(User user) {
        Map<String, Object> friendship = new HashMap<>();
        friendship.put("userId", currentUserId);
        friendship.put("friendId", user.getId());
        friendship.put("status", "PENDING");
        db.collection("friendships").add(friendship).addOnSuccessListener(doc -> {
            Toast.makeText(this, "Đã gửi lời mời", Toast.LENGTH_SHORT).show();
            userAdapter.notifyDataSetChanged();
            sendNotification(user.getEmail(), currentUserName + " đã gửi lời mời kết bạn");
        });
    }

    private void acceptFriendRequest(String friendshipId, User user) {
        db.collection("friendships").document(friendshipId).update("status", "ACCEPTED")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã chấp nhận kết bạn", Toast.LENGTH_SHORT).show();
                    refreshContent();
                    sendNotification(user.getEmail(), currentUserName + " đã chấp nhận lời mời kết bạn");
                });
    }

    private void unfriend(String friendshipId, User user) {
        new AlertDialog.Builder(this)
                .setTitle("Hủy kết bạn")
                .setMessage("Hủy kết bạn với " + user.getFullName() + "?")
                .setPositiveButton("Hủy", (dialog, which) -> {
                    db.collection("friendships").document(friendshipId).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Đã hủy kết bạn", Toast.LENGTH_SHORT).show();
                                refreshContent();
                            });
                })
                .setNegativeButton("Quay lại", null).show();
    }

    private void handleDeclineAction(User user) {
        db.collection("friendships")
                .whereEqualTo("friendId", currentUserId)
                .whereEqualTo("userId", user.getId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete().addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Đã từ chối lời mời", Toast.LENGTH_SHORT).show();
                            refreshContent();
                        });
                    }
                });
    }

    private void sendNotification(String email, String content) {
        if (email == null) return;
        Notification notif = new Notification(email, content, System.currentTimeMillis());
        db.collection("notifications").add(notif);
    }
}
