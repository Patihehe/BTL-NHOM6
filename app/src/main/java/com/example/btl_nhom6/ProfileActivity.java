package com.example.btl_nhom6;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity implements PostAdapter.OnPostActionListener {

    private ImageView ivCover, ivAvatar;
    private TextView tvFullName, tvBio, tvLocation, tvDob;
    private Button btnEditProfile, btnChat, btnFriendAction;
    private RecyclerView rvUserPosts;
    private PostAdapter postAdapter;
    private List<Post> userPostList;
    private FirebaseFirestore db;
    private User profileUser;
    private String profileUserId;
    private String currentUserId;
    private String currentUserName;
    private BottomNavigationView bottomNavigation;

    private ListenerRegistration friendshipListener, profileListener, postsListener;
    private boolean isChangingAvatar = false;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception e) {}
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
        currentUserName = pref.getString("current_user_name", "Ai đó");

        handleIntent(getIntent());

        ivCover = findViewById(R.id.ivCover);
        ivAvatar = findViewById(R.id.ivAvatar);
        tvFullName = findViewById(R.id.tvFullName);
        tvBio = findViewById(R.id.tvBio);
        tvLocation = findViewById(R.id.tvLocation);
        tvDob = findViewById(R.id.tvDob);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnChat = findViewById(R.id.btnChat);
        btnFriendAction = findViewById(R.id.btnFriendAction);
        rvUserPosts = findViewById(R.id.rvUserPosts);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        setupBottomNavigation();

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

        loadAllData();

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnChat.setOnClickListener(v -> {
            if (profileUser != null) {
                Intent intent = new Intent(ProfileActivity.this, ChatActivity.class);
                intent.putExtra("receiver_email", profileUser.getEmail());
                intent.putExtra("receiver_name", profileUser.getFullName());
                startActivity(intent);
            }
        });
        btnFriendAction.setOnClickListener(v -> handleFriendAction());
    }

    private void handleIntent(Intent intent) {
        String id = intent.getStringExtra("profile_user_id");
        profileUserId = (id != null && !id.isEmpty()) ? id : currentUserId;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
        loadAllData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigation != null && profileUserId.equals(currentUserId)) {
            bottomNavigation.setSelectedItemId(R.id.nav_profile);
        }
    }

    private void loadAllData() {
        if (friendshipListener != null) friendshipListener.remove();
        if (profileListener != null) profileListener.remove();
        if (postsListener != null) postsListener.remove();

        loadProfileData();
        loadUserPosts();
        checkFriendshipStatus();
    }

    private void setupBottomNavigation() {
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_profile);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                } else if (id == R.id.nav_friends) {
                    startActivity(new Intent(this, SocialActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                } else if (id == R.id.nav_notifications) {
                    startActivity(new Intent(this, NotificationActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                } else if (id == R.id.nav_menu) {
                    showMenuDialog();
                } else if (id == R.id.nav_profile) {
                    if (!profileUserId.equals(currentUserId)) {
                        Intent intent = new Intent(this, ProfileActivity.class);
                        intent.putExtra("profile_user_id", currentUserId);
                        startActivity(intent);
                    }
                }
                return true;
            });
        }
    }

    private void checkFriendshipStatus() {
        if (profileUserId == null || profileUserId.equals(currentUserId)) {
            btnFriendAction.setVisibility(View.GONE);
            return;
        }

        friendshipListener = db.collection("friendships")
                .whereIn("userId", Arrays.asList(currentUserId, profileUserId))
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    
                    String status = null;
                    String initiatorId = null;

                    for (QueryDocumentSnapshot doc : value) {
                        String uid = doc.getString("userId");
                        String fid = doc.getString("friendId");
                        if ((currentUserId.equals(uid) && profileUserId.equals(fid)) || (profileUserId.equals(uid) && currentUserId.equals(fid))) {
                            String s = doc.getString("status");
                            // Ưu tiên trạng thái ACCEPTED nếu có nhiều bản ghi
                            if ("ACCEPTED".equals(s)) {
                                status = s;
                                initiatorId = uid;
                                break;
                            } else {
                                status = s;
                                initiatorId = uid;
                            }
                        }
                    }

                    btnFriendAction.setVisibility(View.VISIBLE);
                    if (status == null) {
                        btnFriendAction.setText("Thêm bạn bè");
                        btnFriendAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1877F2")));
                        btnFriendAction.setTextColor(Color.WHITE);
                    } else if (status.equals("ACCEPTED")) {
                        btnFriendAction.setText("Hủy kết bạn");
                        btnFriendAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E4E6EB")));
                        btnFriendAction.setTextColor(Color.BLACK);
                    } else if (status.equals("PENDING")) {
                        if (currentUserId.equals(initiatorId)) {
                            btnFriendAction.setText("Đã gửi lời mời");
                            btnFriendAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E4E6EB")));
                            btnFriendAction.setTextColor(Color.BLACK);
                        } else {
                            btnFriendAction.setText("Chấp nhận lời mời");
                            btnFriendAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1877F2")));
                            btnFriendAction.setTextColor(Color.WHITE);
                        }
                    }
                });
    }

    private void handleFriendAction() {
        String currentText = btnFriendAction.getText().toString();
        
        if (currentText.equals("Thêm bạn bè")) {
            Map<String, Object> f = new HashMap<>();
            f.put("userId", currentUserId);
            f.put("friendId", profileUserId);
            f.put("status", "PENDING");
            f.put("timestamp", System.currentTimeMillis());
            db.collection("friendships").add(f).addOnSuccessListener(d -> {
                Toast.makeText(this, "Đã gửi lời mời", Toast.LENGTH_SHORT).show();
                if (profileUser != null) {
                    db.collection("notifications").add(new Notification(profileUser.getEmail(), currentUserName + " đã gửi lời mời kết bạn", System.currentTimeMillis()));
                }
            });
        } else if (currentText.equals("Chấp nhận lời mời")) {
            db.collection("friendships")
                    .whereEqualTo("friendId", currentUserId)
                    .whereEqualTo("userId", profileUserId)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            doc.getReference().update("status", "ACCEPTED");
                            if (profileUser != null) {
                                db.collection("notifications").add(new Notification(profileUser.getEmail(), currentUserName + " đã chấp nhận lời mời kết bạn", System.currentTimeMillis()));
                            }
                        }
                    });
        } else if (currentText.equals("Hủy kết bạn") || currentText.equals("Đã gửi lời mời")) {
            String title = currentText.equals("Hủy kết bạn") ? "Hủy kết bạn" : "Hủy lời mời";
            String msg = currentText.equals("Hủy kết bạn") ? "Bạn có chắc chắn muốn hủy kết bạn?" : "Bạn có muốn hủy lời mời kết bạn này?";
            
            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(msg)
                    .setPositiveButton("Đồng ý", (d, w) -> {
                        db.collection("friendships")
                                .whereIn("userId", Arrays.asList(currentUserId, profileUserId))
                                .get().addOnSuccessListener(snapshots -> {
                            for (QueryDocumentSnapshot doc : snapshots) {
                                String u = doc.getString("userId");
                                String f = doc.getString("friendId");
                                if ((currentUserId.equals(u) && profileUserId.equals(f)) || (profileUserId.equals(u) && currentUserId.equals(f))) {
                                    doc.getReference().delete();
                                }
                            }
                        });
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        }
    }

    private void loadProfileData() {
        profileListener = db.collection("users").document(profileUserId).addSnapshotListener((value, error) -> {
            if (value != null && value.exists()) {
                profileUser = value.toObject(User.class);
                displayUserInfo();
                
                boolean isOwnProfile = profileUserId.equals(currentUserId);
                btnEditProfile.setVisibility(isOwnProfile ? View.VISIBLE : View.GONE);
                btnChat.setVisibility(isOwnProfile ? View.GONE : View.VISIBLE);
                
                if (isOwnProfile) {
                    btnFriendAction.setVisibility(View.GONE);
                }
            }
        });
    }

    private void displayUserInfo() {
        if (profileUser == null) return;
        tvFullName.setText(profileUser.getFullName());
        tvBio.setText(profileUser.getBio() != null && !profileUser.getBio().isEmpty() ? profileUser.getBio() : "Chưa có tiểu sử");
        tvLocation.setText("Sống tại " + (profileUser.getLocation() != null && !profileUser.getLocation().isEmpty() ? profileUser.getLocation() : "..."));
        tvDob.setText("Ngày sinh: " + (profileUser.getDob() != null && !profileUser.getDob().isEmpty() ? profileUser.getDob() : "..."));
        if (profileUser.getAvatarUri() != null && !profileUser.getAvatarUri().isEmpty()) {
            Glide.with(this).load(profileUser.getAvatarUri()).into(ivAvatar);
        } else {
            ivAvatar.setImageResource(R.drawable.ic_launcher_background);
        }
        if (profileUser.getCoverPhotoUri() != null && !profileUser.getCoverPhotoUri().isEmpty()) {
            Glide.with(this).load(profileUser.getCoverPhotoUri()).into(ivCover);
        }
    }

    private void loadUserPosts() {
        postsListener = db.collection("posts")
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        builder.setView(view);

        EditText etName = view.findViewById(R.id.etEditName);
        EditText etBio = view.findViewById(R.id.etEditBio);
        EditText etDob = view.findViewById(R.id.etEditDob);
        EditText etLoc = view.findViewById(R.id.etEditLocation);
        Button btnAvatar = view.findViewById(R.id.btnChangeAvatar);
        Button btnCover = view.findViewById(R.id.btnChangeCover);

        etName.setText(profileUser.getFullName());
        etBio.setText(profileUser.getBio());
        etDob.setText(profileUser.getDob());
        etLoc.setText(profileUser.getLocation());

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            profileUser.setFullName(etName.getText().toString());
            profileUser.setBio(etBio.getText().toString());
            profileUser.setDob(etDob.getText().toString());
            profileUser.setLocation(etLoc.getText().toString());
            db.collection("users").document(profileUserId).set(profileUser);
            Toast.makeText(this, "Đã cập nhật!", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Hủy", null);

        btnAvatar.setOnClickListener(v -> { isChangingAvatar = true; pickImageLauncher.launch("image/*"); });
        btnCover.setOnClickListener(v -> { isChangingAvatar = false; pickImageLauncher.launch("image/*"); });

        builder.show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (friendshipListener != null) friendshipListener.remove();
        if (profileListener != null) profileListener.remove();
        if (postsListener != null) postsListener.remove();
    }

    @Override public void onCommentClick(Post post) {}
    @Override public void onShareClick(Post post) {}
}
