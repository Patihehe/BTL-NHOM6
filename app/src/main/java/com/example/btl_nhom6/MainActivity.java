package com.example.btl_nhom6;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText etPostInput;
    private Button btnPost;
    private RecyclerView recyclerViewPosts;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private AppDatabase db;
    private String currentUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getInstance(this);

        // LẤY TÊN NGƯỜI DÙNG TỪ SHAREDPREFERENCES
        SharedPreferences pref = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        currentUserName = pref.getString("current_user_name", "Anonymous");

        // Khởi tạo View
        etPostInput = findViewById(R.id.etPostInput);
        btnPost = findViewById(R.id.btnPost);
        recyclerViewPosts = findViewById(R.id.recyclerViewPosts);

        // Thiết lập RecyclerView
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList);
        recyclerViewPosts.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPosts.setAdapter(postAdapter);

        // Load bài viết cũ từ Database
        loadPosts();

        // Xử lý sự kiện đăng bài
        btnPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = etPostInput.getText().toString().trim();
                if (!content.isEmpty()) {
                    // Dùng tên thật để đăng bài
                    Post post = new Post(currentUserName, content, System.currentTimeMillis());
                    db.postDao().insertPost(post);
                    
                    etPostInput.setText("");
                    loadPosts(); // Refresh danh sách
                    Toast.makeText(MainActivity.this, "Đã đăng bài!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Bạn đang nghĩ gì?", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadPosts() {
        postList.clear();
        postList.addAll(db.postDao().getAllPosts());
        postAdapter.notifyDataSetChanged();
    }
}
