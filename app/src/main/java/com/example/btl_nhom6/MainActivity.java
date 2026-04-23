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

import com.bumptech.glide.Glide;
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
    private ImageView ivUserAvatarMain;
    private TextView tvMessengerBadge; // Badge cho tin nhắn
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
        tvMessengerBadge = findViewById(R.id.tvMessengerBadge); // Ánh xạ badge tin nhắn
        ivUserAvatarMain = findViewById(R.id.ivUserAvatarMain);
        recyclerViewPosts = findViewById(R.id.recyclerViewPosts);
        spinnerPrivacy = findViewById(R.id.spinnerPrivacy);
        
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList, this::showDeleteConfirmDialog, this);
        recyclerViewPosts.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPosts.setAdapter(postAdapter);

        listenToUserInfo();
        loadFriendsAndListenPosts();
        listenForNotifications();
        listenForUnreadMessages(); // Bắt đầu lắng nghe tin nhắn chưa đọc

        if(btnPickImage != null) btnPickImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        if(btnClearImage != null) btnClearImage.setOnClickListener(v -> {
            selectedImageUri = null;
            ImageView ivPreview = findViewById(R.id.ivSelectedPreview);
            if(ivPreview != null) ivPreview.setVisibility(View.GONE);
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
                            ImageView ivPreview = findViewById(R.id.ivSelectedPreview);
                            if(ivPreview != null) ivPreview.setVisibility(View.GONE);
                            if(btnClearImage != null) btnClearImage.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Đã đăng bài!", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        if(btnMessenger != null) btnMessenger.setOnClickListener(v -> {
            // Khi nhấn vào messenger, xóa badge (số đỏ)
            if(tvMessengerBadge != null) tvMessengerBadge.setVisibility(View.GONE);
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
                    showMenuDialog();
                    return true;
                } else if (itemId == R.id.nav_notifications) {
                     BadgeDrawable badge = bottomNavigation.getBadge(R.id.nav_notifications);
                     if (badge != null) {
                         badge.setVisible(false);
                         badge.clearNumber();
                     }
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

    private void listenToUserInfo() {
        db.collection("users").document(currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null && value.exists()) {
                        User user = value.toObject(User.class);
                        if (user != null) {
                            currentUserName = user.getFullName();
                            if (user.getAvatarUri() != null && !user.getAvatarUri().isEmpty()) {
                                Glide.with(MainActivity.this)
                                        .load(user.getAvatarUri())
                                        .placeholder(R.drawable.ic_launcher_background)
                                        .into(ivUserAvatarMain);
                            }
                        }
                    }
                });
    }

    private void listenForUnreadMessages() {
        if (tvMessengerBadge == null) return;
        db.collection("messages")
                .whereEqualTo("receiverEmail", currentUserEmail)
                .whereEqualTo("isRead", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        int count = value.size();
                        if (count > 0) {
                            tvMessengerBadge.setText(String.valueOf(count));
                            tvMessengerBadge.setVisibility(View.VISIBLE);
                        } else {
                            tvMessengerBadge.setVisibility(View.GONE);
                        }
                    }
                });
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
                        BadgeDrawable badge = bottomNavigation.getBadge(R.id.nav_notifications);
                        if (badge != null) {
                            badge.setVisible(false);
                            badge.clearNumber();
                        }
                    }
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
        if (post.getUserId().equals(currentUserId)) {
            new AlertDialog.Builder(this)
                    .setTitle("Xóa bài viết")
                    .setMessage("Bạn có chắc chắn muốn xóa bài viết này không?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        db.collection("posts").document(post.getPostId()).delete();
                        Toast.makeText(MainActivity.this, "Đã xóa bài viết", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        } else {
            Toast.makeText(this, "Bạn không có quyền xóa bài viết của người khác!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCommentClick(Post post) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_comments, null);
        RecyclerView rvComments = dialogView.findViewById(R.id.rvComments);
        EditText etCommentInput = dialogView.findViewById(R.id.etCommentInput);
        ImageButton btnSendComment = dialogView.findViewById(R.id.btnSendComment);

        List<Comment> commentList = new ArrayList<>();
        CommentAdapter commentAdapter = new CommentAdapter(commentList);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);

        db.collection("comments")
                .whereEqualTo("postId", post.getPostId())
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        commentList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            commentList.add(doc.toObject(Comment.class));
                        }
                        commentAdapter.notifyDataSetChanged();
                        if(!commentList.isEmpty()) rvComments.scrollToPosition(commentList.size()-1);
                    }
                });

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();

        btnSendComment.setOnClickListener(v -> {
            String content = etCommentInput.getText().toString().trim();
            if (!content.isEmpty()) {
                Comment comment = new Comment(post.getPostId(), currentUserId, currentUserName, content, System.currentTimeMillis());
                db.collection("comments").add(comment).addOnSuccessListener(doc -> {
                    doc.update("commentId", doc.getId());
                    etCommentInput.setText("");
                    
                    if (!post.getUserId().equals(currentUserId)) {
                        db.collection("users").document(post.getUserId()).get().addOnSuccessListener(userDoc -> {
                            String ownerEmail = userDoc.getString("email");
                            if (ownerEmail != null) {
                                Notification notif = new Notification(ownerEmail, currentUserName + " đã bình luận bài viết của bạn", System.currentTimeMillis());
                                db.collection("notifications").add(notif);
                            }
                        });
                    }
                });
            }
        });
        dialog.show();
    }

    @Override
    public void onShareClick(Post post) {
        new AlertDialog.Builder(this)
                .setTitle("Chia sẻ")
                .setMessage("Bạn có muốn chia sẻ bài viết này?")
                .setPositiveButton("Chia sẻ", (dialog, which) -> {
                    String sharedContent = "[Shared from " + post.getUserName() + "]: " + post.getContent();
                    Post sharedPost = new Post(currentUserId, currentUserName, sharedContent, post.getImageUri(), System.currentTimeMillis());
                    db.collection("posts").add(sharedPost).addOnSuccessListener(doc -> {
                        doc.update("postId", doc.getId());
                        Toast.makeText(this, "Đã chia sẻ!", Toast.LENGTH_SHORT).show();
                        
                        if (!post.getUserId().equals(currentUserId)) {
                            db.collection("users").document(post.getUserId()).get().addOnSuccessListener(userDoc -> {
                                String ownerEmail = userDoc.getString("email");
                                if (ownerEmail != null) {
                                    Notification notif = new Notification(ownerEmail, currentUserName + " đã chia sẻ bài viết của bạn", System.currentTimeMillis());
                                    db.collection("notifications").add(notif);
                                }
                            });
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
