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
import com.google.android.material.bottomnavigation.BottomNavigationView;
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
        currentUserName = pref.getString("current_user_name", "");

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

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                searchUsers(query);
            }
        });

        refreshContent();
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_friends);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_friends) {
                return true;
            } else if (itemId == R.id.nav_menu) {
                // Show menu dialog if needed or handle accordingly
                return true;
            }
            return false;
        });
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
        etSearch.setText("");

        if (position == 0) { // Search
            llSearch.setVisibility(View.VISIBLE);
        } else if (position == 1) { // Requests
            llSearch.setVisibility(View.GONE);
            loadPendingRequests();
        } else if (position == 2) { // Friends
            llSearch.setVisibility(View.GONE);
            loadFriendsList();
        }
        userAdapter.notifyDataSetChanged();
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
                        if (!user.getId().equals(currentUserId)) {
                            displayList.add(user);
                        }
                    }
                    userAdapter.notifyDataSetChanged();
                    if (displayList.isEmpty()) {
                        Toast.makeText(this, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadPendingRequests() {
        db.collection("friendships")
                .whereEqualTo("friendId", currentUserId)
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    displayList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String requesterId = doc.getString("userId");
                        db.collection("users").document(requesterId).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        displayList.add(userDoc.toObject(User.class));
                                        userAdapter.notifyDataSetChanged();
                                    }
                                });
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
                        String friendId = uid.equals(currentUserId) ? fid : (fid.equals(currentUserId) ? uid : null);
                        
                        if (friendId != null) {
                            db.collection("users").document(friendId).get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            User friend = userDoc.toObject(User.class);
                                            // Tránh add trùng nếu query trả về nhiều kết quả
                                            boolean exists = false;
                                            for(User u : displayList) if(u.getId().equals(friend.getId())) exists = true;
                                            if(!exists) {
                                                displayList.add(friend);
                                                userAdapter.notifyDataSetChanged();
                                            }
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
                        if ((uid.equals(currentUserId) && fid.equals(user.getId())) ||
                            (uid.equals(user.getId()) && fid.equals(currentUserId))) {
                            friendshipId = doc.getId();
                            status = doc.getString("status");
                            initiatorId = uid;
                            break;
                        }
                    }

                    if (status == null) {
                        // Send request
                        Map<String, Object> friendship = new HashMap<>();
                        friendship.put("userId", currentUserId);
                        friendship.put("friendId", user.getId());
                        friendship.put("status", "PENDING");
                        db.collection("friendships").add(friendship)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(this, "Đã gửi lời mời", Toast.LENGTH_SHORT).show();
                                    refreshContent();
                                    // Tạo thông báo
                                    Notification notif = new Notification(user.getEmail(), currentUserName + " đã gửi cho bạn lời mời kết bạn", System.currentTimeMillis());
                                    db.collection("notifications").add(notif);
                                });
                    } else if (status.equals("PENDING")) {
                        if (!initiatorId.equals(currentUserId)) {
                            // Accept request
                            db.collection("friendships").document(friendshipId).update("status", "ACCEPTED")
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Đã chấp nhận kết bạn", Toast.LENGTH_SHORT).show();
                                        refreshContent();
                                        // Thông báo cho người gửi
                                        Notification notif = new Notification(user.getEmail(), currentUserName + " đã chấp nhận lời mời kết bạn", System.currentTimeMillis());
                                        db.collection("notifications").add(notif);
                                    });
                        }
                    } else if (status.equals("ACCEPTED")) {
                        // Unfriend
                        String finalFriendshipId = friendshipId;
                        new AlertDialog.Builder(this)
                                .setTitle("Hủy kết bạn")
                                .setMessage("Bạn có chắc chắn muốn hủy kết bạn với " + user.getFullName() + "?")
                                .setPositiveButton("Hủy", (dialog, which) -> {
                                    db.collection("friendships").document(finalFriendshipId).delete()
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(this, "Đã hủy kết bạn", Toast.LENGTH_SHORT).show();
                                                refreshContent();
                                            });
                                })
                                .setNegativeButton("Quay lại", null)
                                .show();
                    }
                });
    }

    private void handleDeclineAction(User user) {
        db.collection("friendships")
                .whereEqualTo("friendId", currentUserId)
                .whereEqualTo("userId", user.getId())
                .whereEqualTo("status", "PENDING")
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
}
