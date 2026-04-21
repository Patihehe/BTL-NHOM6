package com.example.btl_nhom6;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText etPostInput;
    private Button btnPost;
    private ImageButton btnPickImage, btnClearImage, btnLogout, btnGoToProfile;
    private ImageView ivSelectedPreview;
    private RecyclerView recyclerViewPosts;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private AppDatabase db;
    private String currentUserName;
    private int currentUserId;
    private Uri selectedImageUri = null;

    // Bộ chọn ảnh từ thư viện
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
        setContentView(R.layout.activity_main);

        db = AppDatabase.getInstance(this);

        SharedPreferences pref = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        currentUserId = pref.getInt("current_user_id", -1);
        currentUserName = pref.getString("current_user_name", "Anonymous");

        if (currentUserId == -1) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Khởi tạo View
        etPostInput = findViewById(R.id.etPostInput);
        btnPost = findViewById(R.id.btnPost);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnClearImage = findViewById(R.id.btnClearImage);
        btnLogout = findViewById(R.id.btnLogout);
        btnGoToProfile = findViewById(R.id.btnGoToProfile);
        ivSelectedPreview = findViewById(R.id.ivSelectedPreview);
        recyclerViewPosts = findViewById(R.id.recyclerViewPosts);

        // Thiết lập RecyclerView
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList, this::showDeleteConfirmDialog);
        recyclerViewPosts.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPosts.setAdapter(postAdapter);

        loadPosts();

        // Chọn ảnh
        btnPickImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // Hủy chọn ảnh
        btnClearImage.setOnClickListener(v -> {
            selectedImageUri = null;
            ivSelectedPreview.setVisibility(View.GONE);
            btnClearImage.setVisibility(View.GONE);
        });

        // Đăng bài
        btnPost.setOnClickListener(v -> {
            String content = etPostInput.getText().toString().trim();
            if (!content.isEmpty() || selectedImageUri != null) {
                String uriString = (selectedImageUri != null) ? selectedImageUri.toString() : "";
                // Cập nhật lại userName từ DB phòng trường hợp người dùng vừa đổi tên
                User user = db.userDao().getUserById(currentUserId);
                String name = (user != null) ? user.getFullName() : currentUserName;
                
                Post post = new Post(currentUserId, name, content, uriString, System.currentTimeMillis());
                db.postDao().insertPost(post);
                
                etPostInput.setText("");
                selectedImageUri = null;
                ivSelectedPreview.setVisibility(View.GONE);
                btnClearImage.setVisibility(View.GONE);
                
                loadPosts();
                Toast.makeText(MainActivity.this, "Đã đăng bài!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Vui lòng nhập nội dung hoặc chọn ảnh", Toast.LENGTH_SHORT).show();
            }
        });

        // Vào trang cá nhân
        btnGoToProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // Xử lý Đăng xuất
        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Đăng xuất")
                    .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                    .setPositiveButton("Đăng xuất", (dialog, which) -> {
                        SharedPreferences.Editor editor = pref.edit();
                        editor.clear();
                        editor.apply();

                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    private void showDeleteConfirmDialog(Post post) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bài viết")
                .setMessage("Bạn có chắc chắn muốn xóa bài viết này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    db.postDao().deletePost(post);
                    loadPosts();
                    Toast.makeText(MainActivity.this, "Đã xóa bài viết", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void loadPosts() {
        postList.clear();
        postList.addAll(db.postDao().getAllPosts());
        postAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPosts(); // Load lại posts nhỡ có đổi tên hiển thị
    }
}
