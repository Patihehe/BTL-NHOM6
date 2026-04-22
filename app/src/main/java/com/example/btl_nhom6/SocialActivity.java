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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
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
    private BottomNavigationView bottomNavigation;

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
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // 1. XỬ LÝ MENU ĐIỀU HƯỚNG (SỬA LỖI KHÔNG CHUYỂN ĐƯỢC TAB)
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_friends); // In đậm tab hiện tại
            bottomNavigation.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_profile) {
                    Intent intent = new Intent(this, ProfileActivity.class);
                    intent.putExtra("profile_user_id", currentUserId);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (id == R.id.nav_notifications) {
                    startActivity(new Intent(this, NotificationActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_menu) {
                    showMenuDialog();
                    return true;
                }
                return true;
            });
        }

        recyclerViewSocial.setLayoutManager(new LinearLayoutManager(this));
        setupAdapter();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) { refreshContent(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) searchUsers(query);
        });

        refreshContent();
    }

    private void showMenuDialog() {
        String[] options = {"Chế độ tối", "Đăng xuất"};
        new AlertDialog.Builder(this)
                .setTitle("Cài đặt")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) toggleDarkMode();
                    else if (which == 1) logoutUser();
                }).show();
    }

    private void toggleDarkMode() {
        SharedPreferences pref = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isDarkMode = pref.getBoolean("dark_mode", false);
        pref.edit().putBoolean("dark_mode", !isDarkMode).apply();
        AppCompatDelegate.setDefaultNightMode(!isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        recreate();
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().clear().apply();
        startActivity(new Intent(this, LoginActivity.class));
        finishAffinity();
    }

    private void setupAdapter() {
        userAdapter = new UserAdapter(displayList, currentUserId, db, new UserAdapter.OnUserActionListener() {
            @Override public void onAction(User user) { handleUserAction(user); }
            @Override public void onDecline(User user) { handleDeclineAction(user); }
            @Override public void onUserClick(User user) {
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
        if (position == 0) llSearch.setVisibility(View.VISIBLE);
        else if (position == 1) {
            llSearch.setVisibility(View.GONE);
            loadPendingRequests();
        } else if (position == 2) {
            llSearch.setVisibility(View.GONE);
            loadFriendsList();
        }
    }

    private void searchUsers(String query) {
        db.collection("users").orderBy("fullName").startAt(query).endAt(query + "\uf8ff").get()
                .addOnSuccessListener(snapshots -> {
                    displayList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        User user = doc.toObject(User.class);
                        if (user.getId() != null && !user.getId().equals(currentUserId)) displayList.add(user);
                    }
                    userAdapter.notifyDataSetChanged();
                });
    }

    // 2. TỐI ƯU HÓA TẢI LỜI MỜI (SỬA LỖI HIỂN THỊ THIẾU NGƯỜI)
    private void loadPendingRequests() {
        db.collection("friendships")
                .whereEqualTo("friendId", currentUserId)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    displayList.clear();
                    if (snapshots.isEmpty()) {
                        userAdapter.notifyDataSetChanged();
                        return;
                    }
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String requesterId = doc.getString("userId");
                        if (requesterId != null) {
                            db.collection("users").document(requesterId).get().addOnSuccessListener(userDoc -> {
                                if (userDoc.exists()) {
                                    User requester = userDoc.toObject(User.class);
                                    // Kiểm tra tránh trùng lặp
                                    boolean alreadyInList = false;
                                    for(User u : displayList) if(u.getId().equals(requester.getId())) alreadyInList = true;
                                    if(!alreadyInList) {
                                        displayList.add(requester);
                                        userAdapter.notifyDataSetChanged();
                                    }
                                }
                            });
                        }
                    }
                });
    }

    private void loadFriendsList() {
        db.collection("friendships")
                .whereEqualTo("status", "ACCEPTED")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    displayList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String uid = doc.getString("userId");
                        String fid = doc.getString("friendId");
                        String friendId = currentUserId.equals(uid) ? fid : (currentUserId.equals(fid) ? uid : null);
                        if (friendId != null) {
                            db.collection("users").document(friendId).get().addOnSuccessListener(uDoc -> {
                                if (uDoc.exists()) {
                                    User friend = uDoc.toObject(User.class);
                                    displayList.add(friend);
                                    userAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }
                });
    }

    private void handleUserAction(User user) {
        db.collection("friendships").get().addOnSuccessListener(snapshots -> {
            String fId = null, status = null, initiator = null;
            for (QueryDocumentSnapshot doc : snapshots) {
                String u = doc.getString("userId"), f = doc.getString("friendId");
                if ((currentUserId.equals(u) && user.getId().equals(f)) || (user.getId().equals(u) && currentUserId.equals(f))) {
                    fId = doc.getId(); status = doc.getString("status"); initiator = u; break;
                }
            }
            if (status == null) sendReq(user);
            else if (status.equals("PENDING") && !currentUserId.equals(initiator)) acceptReq(fId, user);
            else if (status.equals("ACCEPTED")) unfriend(fId, user);
        });
    }

    private void sendReq(User user) {
        Map<String, Object> f = new HashMap<>();
        f.put("userId", currentUserId); f.put("friendId", user.getId()); f.put("status", "PENDING");
        db.collection("friendships").add(f).addOnSuccessListener(d -> {
            Toast.makeText(this, "Đã gửi lời mời", Toast.LENGTH_SHORT).show();
            db.collection("notifications").add(new Notification(user.getEmail(), currentUserName + " đã gửi lời mời kết bạn", System.currentTimeMillis()));
        });
    }

    private void acceptReq(String fId, User user) {
        db.collection("friendships").document(fId).update("status", "ACCEPTED").addOnSuccessListener(a -> {
            Toast.makeText(this, "Đã chấp nhận", Toast.LENGTH_SHORT).show();
            refreshContent();
            db.collection("notifications").add(new Notification(user.getEmail(), currentUserName + " đã chấp nhận lời mời kết bạn", System.currentTimeMillis()));
        });
    }

    private void unfriend(String fId, User user) {
        new AlertDialog.Builder(this).setTitle("Hủy kết bạn").setMessage("Hủy kết bạn với " + user.getFullName() + "?")
                .setPositiveButton("Hủy", (d, w) -> db.collection("friendships").document(fId).delete().addOnSuccessListener(a -> {
                    Toast.makeText(this, "Đã hủy kết bạn", Toast.LENGTH_SHORT).show();
                    refreshContent();
                })).setNegativeButton("Quay lại", null).show();
    }

    private void handleDeclineAction(User user) {
        db.collection("friendships").whereEqualTo("friendId", currentUserId).whereEqualTo("userId", user.getId()).get()
                .addOnSuccessListener(snapshots -> {
                    for (QueryDocumentSnapshot doc : snapshots) doc.getReference().delete().addOnSuccessListener(a -> refreshContent());
                });
    }
}
