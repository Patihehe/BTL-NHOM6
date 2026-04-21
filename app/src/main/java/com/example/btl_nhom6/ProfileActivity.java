package com.example.btl_nhom6;

import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private ImageView ivCover, ivAvatar;
    private TextView tvFullName, tvBio, tvLocation, tvDob;
    private Button btnEditProfile;
    private RecyclerView rvUserPosts;
    private PostAdapter postAdapter;
    private List<Post> userPostList;
    private AppDatabase db;
    private User currentUser;
    private int currentUserId;

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
                        currentUser.setAvatarUri(uri.toString());
                        Glide.with(this).load(uri).into(ivAvatar);
                    } else {
                        currentUser.setCoverPhotoUri(uri.toString());
                        Glide.with(this).load(uri).into(ivCover);
                    }
                    db.userDao().updateUser(currentUser);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = AppDatabase.getInstance(this);
        SharedPreferences pref = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        currentUserId = pref.getInt("current_user_id", -1);

        if (currentUserId == -1) {
            finish();
            return;
        }

        currentUser = db.userDao().getUserById(currentUserId);

        ivCover = findViewById(R.id.ivCover);
        ivAvatar = findViewById(R.id.ivAvatar);
        tvFullName = findViewById(R.id.tvFullName);
        tvBio = findViewById(R.id.tvBio);
        tvLocation = findViewById(R.id.tvLocation);
        tvDob = findViewById(R.id.tvDob);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        rvUserPosts = findViewById(R.id.rvUserPosts);

        displayUserInfo();

        userPostList = new ArrayList<>();
        postAdapter = new PostAdapter(userPostList, post -> {
            new AlertDialog.Builder(this)
                    .setTitle("Xóa bài viết")
                    .setMessage("Bạn có chắc chắn muốn xóa bài viết này không?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        db.postDao().deletePost(post);
                        loadUserPosts();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
        rvUserPosts.setLayoutManager(new LinearLayoutManager(this));
        rvUserPosts.setAdapter(postAdapter);

        loadUserPosts();

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
    }

    private void displayUserInfo() {
        tvFullName.setText(currentUser.getFullName());
        tvBio.setText(currentUser.getBio() != null && !currentUser.getBio().isEmpty() ? currentUser.getBio() : "Chưa có tiểu sử");
        tvLocation.setText("Sống tại " + (currentUser.getLocation() != null && !currentUser.getLocation().isEmpty() ? currentUser.getLocation() : "..."));
        tvDob.setText("Ngày sinh: " + (currentUser.getDob() != null && !currentUser.getDob().isEmpty() ? currentUser.getDob() : "..."));

        if (currentUser.getAvatarUri() != null) {
            Glide.with(this).load(Uri.parse(currentUser.getAvatarUri())).into(ivAvatar);
        }
        if (currentUser.getCoverPhotoUri() != null) {
            Glide.with(this).load(Uri.parse(currentUser.getCoverPhotoUri())).into(ivCover);
        }
    }

    private void loadUserPosts() {
        userPostList.clear();
        userPostList.addAll(db.postDao().getPostsByUserId(currentUserId));
        postAdapter.notifyDataSetChanged();
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

        etEditName.setText(currentUser.getFullName());
        etEditBio.setText(currentUser.getBio());
        etEditDob.setText(currentUser.getDob());
        etEditLocation.setText(currentUser.getLocation());

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            currentUser.setFullName(etEditName.getText().toString());
            currentUser.setBio(etEditBio.getText().toString());
            currentUser.setDob(etEditDob.getText().toString());
            currentUser.setLocation(etEditLocation.getText().toString());

            db.userDao().updateUser(currentUser);
            displayUserInfo();
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

        dialog.show();
    }
}
