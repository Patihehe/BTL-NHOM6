package com.example.btl_nhom6;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity implements PostAdapter.OnPostActionListener {

    private ImageView ivCover, ivAvatar;
    private TextView tvFullName, tvBio, tvLocation, tvDob;
    private Button btnEditProfile, btnChat;
    private RecyclerView rvUserPosts;
    private PostAdapter postAdapter;
    private List<Post> userPostList;
    private FirebaseFirestore db;
    private User profileUser;
    private String profileUserId;
    private String currentUserId;
    private BottomNavigationView bottomNavigation;

    private boolean isChangingAvatar = false;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    if (isChangingAvatar) {
                        profileUser.setAvatarUri(uri.toString());
                        Glide.with(this).load(uri).into(ivAvatar);
                    } else {
                        profileUser.setCoverPhotoUri(uri.toString());
                        Glide.with(this).load(uri).into(ivCover);
                    }
                    db.collection("users").document(profileUserId).set(profileUser);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        SharedPreferences pref = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        currentUserId = pref.getString("current_user_id", "");

        profileUserId = getIntent().getStringExtra("profile_user_id");
        if (profileUserId == null || profileUserId.isEmpty()) {
            profileUserId = currentUserId;
        }

        ivCover = findViewById(R.id.ivCover);
        ivAvatar = findViewById(R.id.ivAvatar);
        tvFullName = findViewById(R.id.tvFullName);
        tvBio = findViewById(R.id.tvBio);
        tvLocation = findViewById(R.id.tvLocation);
        tvDob = findViewById(R.id.tvDob);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnChat = findViewById(R.id.btnChat);
        rvUserPosts = findViewById(R.id.rvUserPosts);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // ĐỒNG BỘ THANH MENU CHO TRANG PROFILE
        if (bottomNavigation != null) {
            if (profileUserId.equals(currentUserId)) {
                bottomNavigation.setSelectedItemId(R.id.nav_profile);
            }
            bottomNavigation.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else if (id == R.id.nav_friends) {
                    startActivity(new Intent(this, SocialActivity.class));
                    finish();
                } else if (id == R.id.nav_notifications) {
                    startActivity(new Intent(this, NotificationActivity.class));
                    finish();
                } else if (id == R.id.nav_menu) {
                    showMenuDialog(); // HIỂN THỊ MENU
                } else if (id == R.id.nav_profile) {
                    if (!profileUserId.equals(currentUserId)) {
                        Intent intent = new Intent(this, ProfileActivity.class);
                        intent.putExtra("profile_user_id", currentUserId);
                        startActivity(intent);
                        finish();
                    }
                }
                return true;
            });
        }

        userPostList = new ArrayList<>();
        postAdapter = new PostAdapter(userPostList, post -> {
            if (profileUserId.equals(currentUserId)) {
                new AlertDialog.Builder(this)
                        .setTitle("Xóa bài viết")
                        .setMessage("Bạn có chắc chắn muốn xóa bài viết này không?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            db.collection("posts").document(post.getPostId()).delete();
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        }, this);
        rvUserPosts.setLayoutManager(new LinearLayoutManager(this));
        rvUserPosts.setAdapter(postAdapter);

        loadProfileData();
        loadUserPosts();

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnChat.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ChatActivity.class);
            intent.putExtra("receiver_email", profileUser.getEmail());
            intent.putExtra("receiver_name", profileUser.getFullName());
            startActivity(intent);
        });
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

    private void loadProfileData() {
        db.collection("users").document(profileUserId).addSnapshotListener((value, error) -> {
            if (value != null && value.exists()) {
                profileUser = value.toObject(User.class);
                displayUserInfo();
                if (!profileUserId.equals(currentUserId)) {
                    btnEditProfile.setVisibility(View.GONE);
                    btnChat.setVisibility(View.VISIBLE);
                } else {
                    btnEditProfile.setVisibility(View.VISIBLE);
                    btnChat.setVisibility(View.GONE);
                }
            }
        });
    }

    private void displayUserInfo() {
        if (profileUser == null) return;
        tvFullName.setText(profileUser.getFullName());
        tvBio.setText(profileUser.getBio());
        tvLocation.setText(profileUser.getLocation());
        tvDob.setText(profileUser.getDob());
        if (profileUser.getAvatarUri() != null && !profileUser.getAvatarUri().isEmpty()) {
            Glide.with(this).load(profileUser.getAvatarUri()).into(ivAvatar);
        }
    }

    private void loadUserPosts() {
        db.collection("posts")
                .whereEqualTo("userId", profileUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        userPostList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            userPostList.add(doc.toObject(Post.class));
                        }
                        postAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void showEditProfileDialog() {
        // Logic chỉnh sửa
    }

    @Override public void onCommentClick(Post post) {}
    @Override public void onShareClick(Post post) {}
}
