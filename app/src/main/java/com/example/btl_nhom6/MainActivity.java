package com.example.btl_nhom6;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
                    ivSelectedPreview.setImageURI(uri);
                    ivSelectedPreview.setVisibility(View.VISIBLE);
                    btnClearImage.setVisibility(View.VISIBLE);
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
        
        // Load Dark Mode state
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
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        currentUserId = mAuth.getCurrentUser().getUid();
        currentUserName = pref.getString("current_user_name", "Anonymous");

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

        btnPickImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnClearImage.setOnClickListener(v -> {
            selectedImageUri = null;
            ivSelectedPreview.setVisibility(View.GONE);
            btnClearImage.setVisibility(View.GONE);
        });

        btnPost.setOnClickListener(v -> {
            String content = etPostInput.getText().toString().trim();
            if (!content.isEmpty() || selectedImageUri != null) {
                String uriString = (selectedImageUri != null) ? selectedImageUri.toString() : "";
                
                String privacy = "public";
                int selectedPrivacyIndex = spinnerPrivacy.getSelectedItemPosition();
                if (selectedPrivacyIndex == 1) privacy = "friends";
                else if (selectedPrivacyIndex == 2) privacy = "private";

                Post post = new Post(currentUserId, currentUserName, content, uriString, System.currentTimeMillis(), privacy);
                db.collection("posts").add(post)
                        .addOnSuccessListener(documentReference -> {
                            String id = documentReference.getId();
                            db.collection("posts").document(id).update("postId", id);
                            etPostInput.setText("");
                            selectedImageUri = null;
                            ivSelectedPreview.setVisibility(View.GONE);
                            btnClearImage.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Đã đăng bài!", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        btnMessenger.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatListActivity.class);
            startActivity(intent);
        });
        
        // Swipe to Refresh logic
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadFriendsAndListenPosts();
            swipeRefreshLayout.setRefreshing(false);
        });
        
        // Bottom Navigation logic
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
                showMenuDialog();
                return true;
            } else if (itemId == R.id.nav_notifications) {
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
    
    private void showMenuDialog() {
        String[] options = {"Chế độ tối", "Đăng xuất"};
        new AlertDialog.Builder(this)
                .setTitle("Menu")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        toggleDarkMode();
                    } else if (which == 1) {
                        logoutUser();
                    }
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
        SharedPreferences.Editor editor = pref.edit();
        
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            editor.putBoolean("dark_mode", false);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            editor.putBoolean("dark_mode", true);
        }
        editor.apply();
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

    @Override
    public void onCommentClick(Post post) {
        // Implement comment logic if needed
    }

    @Override
    public void onShareClick(Post post) {
        // Implement share logic if needed
    }
}
