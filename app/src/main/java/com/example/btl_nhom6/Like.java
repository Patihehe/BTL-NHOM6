package com.example.btl_nhom6;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "likes", 
        indices = {@Index(value = {"postId", "userId"}, unique = true)})
public class Like {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int postId;
    private int userId;

    public Like(int postId, int userId) {
        this.postId = postId;
        this.userId = userId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
}
