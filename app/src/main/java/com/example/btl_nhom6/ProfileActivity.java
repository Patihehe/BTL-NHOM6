package com.example.btl_nhom6;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
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
    private FirebaseAuth mAuth;

    private boolean isChangingAvatar = false;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
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
        mAuth = FirebaseAuth.getInstance();
        SharedPreferences pref = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        currentUserId = pref.getString("current_user_id", "");

        profileUserId = getIntent().getStringExtra("profile_user_id");
        if (profileUserId == null || profileUserId.isEmpty()) {
            profileUserId = currentUserId;
        }

        if (profileUserId.isEmpty()) {
            finish();
            return;
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
        setupBottomNavigation();

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnChat.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ChatActivity.class);
            intent.putExtra("receiver_email", profileUser.getEmail());
            intent.putExtra("receiver_name", profileUser.getFullName());
            startActivity(intent);
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (profileUserId.equals(currentUserId)) {
            bottomNavigation.setSelectedItemId(R.id.nav_profile);
        }
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_friends) {
                startActivity(new Intent(this, SocialActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                if (!profileUserId.equals(currentUserId)) {
                   Intent intent = new Intent(this, ProfileActivity.class);
                   intent.putExtra("profile_user_id", currentUserId);
                   startActivity(intent);
                   finish();
                }
                return true;
            } else if (itemId == R.id.nav_menu) {
                return true;
            }
            return false;
        });
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
        tvBio.setText(profileUser.getBio() != null && !profileUser.getBio().isEmpty() ? profileUser.getBio() : "Chưa có tiểu sử");
        tvLocation.setText("Sống tại " + (profileUser.getLocation() != null && !profileUser.getLocation().isEmpty() ? profileUser.getLocation() : "..."));
        tvDob.setText("Ngày sinh: " + (profileUser.getDob() != null && !profileUser.getDob().isEmpty() ? profileUser.getDob() : "..."));

        if (profileUser.getAvatarUri() != null && !profileUser.getAvatarUri().isEmpty()) {
            Glide.with(this).load(Uri.parse(profileUser.getAvatarUri())).into(ivAvatar);
        } else {
            ivAvatar.setImageResource(android.R.drawable.ic_menu_report_image);
        }
        if (profileUser.getCoverPhotoUri() != null && !profileUser.getCoverPhotoUri().isEmpty()) {
            Glide.with(this).load(Uri.parse(profileUser.getCoverPhotoUri())).into(ivCover);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        builder.setView(view);

        EditText etEditName = view.findViewById(R.id.etEditName);
        EditText etEditBio = view.findViewById(R.id.etEditBio);
        EditText etEditDob = view.findViewById(R.id.etEditDob);
        EditText etEditLocation = view.findViewById(R.id.etEditLocation);
        Button btnChangeAvatar = view.findViewById(R.id.btnChangeAvatar);
        Button btnChangeCover = view.findViewById(R.id.btnChangeCover);
        Button btnChangePassword = view.findViewById(R.id.btnChangePassword);

        etEditName.setText(profileUser.getFullName());
        etEditBio.setText(profileUser.getBio());
        etEditDob.setText(profileUser.getDob());
        etEditLocation.setText(profileUser.getLocation());

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            profileUser.setFullName(etEditName.getText().toString());
            profileUser.setBio(etEditBio.getText().toString());
            profileUser.setDob(etEditDob.getText().toString());
            profileUser.setLocation(etEditLocation.getText().toString());

            db.collection("users").document(profileUserId).set(profileUser);
            Toast.makeText(this, "Đã cập nhật hồ sơ", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Hủy", null);

        AlertDialog dialog = builder.create();

        btnChangeAvatar.setOnClickListener(v -> {
            isChangingAvatar = true;
            pickImageLauncher.launch("image/*");
        });

        btnChangeCover.setOnClickListener(v -> {
            isChangingAvatar = false;
            pickImageLauncher.launch("image/*");
        });

        btnChangePassword.setOnClickListener(v -> {
            showChangePasswordDialog();
        });

        dialog.show();
    }

    private void showChangePasswordDialog() {
        EditText newPassword = new EditText(this);
        newPassword.setHint("Nhập mật khẩu mới");
        newPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("Đổi mật khẩu")
                .setMessage("Nhập mật khẩu mới cho tài khoản của bạn:")
                .setView(newPassword)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    String password = newPassword.getText().toString().trim();
                    if (password.length() < 6) {
                        Toast.makeText(this, "Mật khẩu phải ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
                    } else if (mAuth.getCurrentUser() != null) {
                        mAuth.getCurrentUser().updatePassword(password)
                                .addOnSuccessListener(aVoid -> Toast.makeText(ProfileActivity.this, "Đã đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onCommentClick(Post post) {}

    @Override
    public void onShareClick(Post post) {}
}
