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
import java.util.ArrayList;
import java.util.List;

public class SocialActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private LinearLayout llSearch;
    private EditText etSearch;
    private Button btnSearch;
    private RecyclerView recyclerViewSocial;
    private UserAdapter userAdapter;
    private List<User> displayList = new ArrayList<>();
    private AppDatabase db;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social);

        db = AppDatabase.getInstance(this);
        SharedPreferences pref = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        currentUserId = pref.getInt("current_user_id", -1);

        if (currentUserId == -1) {
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

        // Initial view
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
                // Mở trang cá nhân của người khác
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
            displayList.addAll(db.friendshipDao().getPendingRequests(currentUserId));
        } else if (position == 2) { // Friends
            llSearch.setVisibility(View.GONE);
            displayList.addAll(db.friendshipDao().getFriends(currentUserId));
        }
        userAdapter.notifyDataSetChanged();
    }

    private void searchUsers(String query) {
        displayList.clear();
        displayList.addAll(db.friendshipDao().searchUsers(query, currentUserId));
        userAdapter.notifyDataSetChanged();
        if (displayList.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleUserAction(User user) {
        Friendship friendship = db.friendshipDao().getFriendship(currentUserId, user.getId());

        if (friendship == null) {
            // Send request
            Friendship newRequest = new Friendship(currentUserId, user.getId(), "PENDING");
            db.friendshipDao().sendFriendRequest(newRequest);
            Toast.makeText(this, "Đã gửi lời mời kết bạn", Toast.LENGTH_SHORT).show();
        } else if (friendship.getStatus().equals("PENDING") && friendship.getFriendId() == currentUserId) {
            // Accept request
            friendship.setStatus("ACCEPTED");
            db.friendshipDao().updateFriendshipStatus(friendship);
            Toast.makeText(this, "Đã chấp nhận lời mời", Toast.LENGTH_SHORT).show();
        } else if (friendship.getStatus().equals("ACCEPTED")) {
            // Hủy kết bạn
            new AlertDialog.Builder(this)
                    .setTitle("Hủy kết bạn")
                    .setMessage("Bạn có chắc chắn muốn hủy kết bạn với " + user.getFullName() + "?")
                    .setPositiveButton("Hủy kết bạn", (dialog, which) -> {
                        db.friendshipDao().deleteFriendship(friendship);
                        Toast.makeText(this, "Đã hủy kết bạn", Toast.LENGTH_SHORT).show();
                        refreshContent();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        }
        
        userAdapter.notifyDataSetChanged();
        if (tabLayout.getSelectedTabPosition() != 0) {
            refreshContent();
        }
    }

    private void handleDeclineAction(User user) {
        Friendship friendship = db.friendshipDao().getFriendship(currentUserId, user.getId());
        if (friendship != null && friendship.getStatus().equals("PENDING") && friendship.getFriendId() == currentUserId) {
            db.friendshipDao().deleteFriendship(friendship);
            Toast.makeText(this, "Đã từ chối lời mời", Toast.LENGTH_SHORT).show();
            refreshContent();
        }
    }
}
