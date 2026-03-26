package com.example.btl_nhom6;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "posts")
public class Post {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String userName;
    private String content;
    private String imageUri; // Lưu URI của hình ảnh
    private long timestamp;

    public Post(String userName, String content, String imageUri, long timestamp) {
        this.userName = userName;
        this.content = content;
        this.imageUri = imageUri;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
