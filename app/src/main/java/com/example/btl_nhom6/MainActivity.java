package com.example.btl_nhom6;

import android.content.DialogInterface;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements PostAdapter.OnPostActionListener {

    private EditText etPostInput;
    private Button btnPost;
    private ImageButton btnPickImage, btnClearImage, btnMessenger;
    private Spinner spinnerPrivacy;
    private ImageView ivSelectedPreview;
    private RecyclerView recyclerViewPosts;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private FirebaseFirestore db;
    private String currentUserName;
    private String currentUserEmail;
    private String currentUserId; 
    private Uri selectedImageUri = null;
    private Set<String> friendsIds = new HashSet<>();
    
    private SwipeRefreshLayout swipeRefreshLayout;
    private BottomNavigationView bottomNavigation;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ImageView ivPreview = findViewById(R.id.ivSelectedPreview);
                    if(ivPreview != null) {
                        ivPreview.setImageURI(uri);
                        ivPreview.setVisibility(View.VISIBLE);
                    }
                    if(btnClearImage != null) btnClearImage.setVisibility(View.VISIBLE);
                    try {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SharedPreferences pref = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isDarkMode = pref.getBoolean("dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        currentUserId = mAuth.getCurrentUser().getUid();
        currentUserName = pref.getString("current_user_name", "Anonymous");
        currentUserEmail = pref.getString("current_user_email", "");

        etPostInput = findViewById(R.id.etPostInput);
        btnPost = findViewById(R.id.btnPost);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnClearImage = findViewById(R.id.btnClearImage);
        btnMessenger = findViewById(R.id.btnMessenger);
        ivSelectedPreview = findViewById(R.id.ivSelectedPreview);
        recyclerViewPosts = findViewById(R.id.recyclerViewPosts);
        spinnerPrivacy = findViewById(R.id.spinnerPrivacy);
        
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList, this::showDeleteConfirmDialog, this);
        recyclerViewPosts.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPosts.setAdapter(postAdapter);

        loadFriendsAndListenPosts();
        listenForNotifications();

        if(btnPickImage != null) btnPickImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        if(btnClearImage != null) btnClearImage.setOnClickListener(v -> {
            selectedImageUri = null;
            if(ivSelectedPreview != null) ivSelectedPreview.setVisibility(View.GONE);
            btnClearImage.setVisibility(View.GONE);
        });

        if(btnPost != null) btnPost.setOnClickListener(v -> {
            String content = (etPostInput != null) ? etPostInput.getText().toString().trim() : "";
            if (!content.isEmpty() || selectedImageUri != null) {
                String uriString = (selectedImageUri != null) ? selectedImageUri.toString() : "";
                
                String privacy = "public";
                if(spinnerPrivacy != null) {
                    int selectedPrivacyIndex = spinnerPrivacy.getSelectedItemPosition();
                    if (selectedPrivacyIndex == 1) privacy = "friends";
                    else if (selectedPrivacyIndex == 2) privacy = "private";
                }

                Post post = new Post(currentUserId, currentUserName, content, uriString, System.currentTimeMillis());
                post.setPrivacy(privacy);
                db.collection("posts").add(post)
                        .addOnSuccessListener(documentReference -> {
                            String id = documentReference.getId();
                            db.collection("posts").document(id).update("postId", id);
                            if(etPostInput != null) etPostInput.setText("");
                            selectedImageUri = null;
                            if(ivSelectedPreview != null) ivSelectedPreview.setVisibility(View.GONE);
                            if(btnClearImage != null) btnClearImage.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Đã đăng bài!", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        if(btnMessenger != null) btnMessenger.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatListActivity.class);
            startActivity(intent);
        });
        
        if(swipeRefreshLayout != null) swipeRefreshLayout.setOnRefreshListener(() -> {
            loadFriendsAndListenPosts();
            swipeRefreshLayout.setRefreshing(false);
        });
        
        if(bottomNavigation != null) {
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    recyclerViewPosts.smoothScrollToPosition(0);
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_menu) {
                    showMenuDialog(); // Mở menu ở MainActivity
                    return true;
                } else if (itemId == R.id.nav_notifications) {
                     markAllNotificationsAsRead();
                     Intent intent = new Intent(MainActivity.this, NotificationActivity.class);
                     startActivity(intent);
                     return true;
                } else if (itemId == R.id.nav_friends) {
                     Intent intent = new Intent(MainActivity.this, SocialActivity.class);
                     startActivity(intent);
                     return true;
                }
                return false;
            });
        }
    }
    
    private void markAllNotificationsAsRead() {
        db.collection("notifications")
                .whereEqualTo("userEmail", currentUserEmail)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().update("isRead", true);
                    }
                });
    }

    private void listenForNotifications() {
        if(bottomNavigation == null) return;
        db.collection("notifications")
                .whereEqualTo("userEmail", currentUserEmail)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    int unreadCount = 0;
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Boolean isRead = doc.getBoolean("isRead");
                            if (isRead != null && !isRead) unreadCount++;
                        }
                    }
                    
                    if (unreadCount > 0) {
                        BadgeDrawable badge = bottomNavigation.getOrCreateBadge(R.id.nav_notifications);
                        badge.setNumber(unreadCount);
                        badge.setVisible(true);
                    } else {
                        bottomNavigation.removeBadge(R.id.nav_notifications);
                    }
                });
    }
    
    private void showMenuDialog() {
        String[] options = {"Chế độ tối", "Đăng xuất"};
        new AlertDialog.Builder(this)
                .setTitle("Cài đặt")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) toggleDarkMode();
                    else if (which == 1) logoutUser();
                })
                .show();
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        SharedPreferences pref = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.clear();
        editor.apply();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
    
    private void toggleDarkMode() {
        SharedPreferences pref = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isDarkMode = pref.getBoolean("dark_mode", false);
        pref.edit().putBoolean("dark_mode", !isDarkMode).apply();
        recreate();
    }

    private void loadFriendsAndListenPosts() {
        db.collection("friendships")
                .whereEqualTo("status", "ACCEPTED")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    friendsIds.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            String uid = doc.getString("userId");
                            String fid = doc.getString("friendId");
                            if (currentUserId.equals(uid)) friendsIds.add(fid);
                            else if (currentUserId.equals(fid)) friendsIds.add(uid);
                        }
                    }
                    listenForPosts();
                });
    }

    private void listenForPosts() {
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    postList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Post post = doc.toObject(Post.class);
                            if (canViewPost(post)) {
                                postList.add(post);
                            }
                        }
                    }
                    postAdapter.notifyDataSetChanged();
                });
    }

    private boolean canViewPost(Post post) {
        String privacy = post.getPrivacy();
        if (privacy == null || privacy.equals("public")) return true;
        if (post.getUserId().equals(currentUserId)) return true;
        if (privacy.equals("friends")) return friendsIds.contains(post.getUserId());
        if (privacy.equals("private")) return post.getUserId().equals(currentUserId);
        return false;
    }

    private void showDeleteConfirmDialog(Post post) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bài viết")
                .setMessage("Bạn có chắc chắn muốn xóa bài viết này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    db.collection("posts").document(post.getPostId()).delete();
                    Toast.makeText(MainActivity.this, "Đã xóa bài viết", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override public void onCommentClick(Post post) {}
    @Override public void onShareClick(Post post) {}
}
