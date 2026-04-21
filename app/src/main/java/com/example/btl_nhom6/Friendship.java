package com.example.btl_nhom6;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "friendships")
public class Friendship {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String userId; // Đã đổi sang String
    private String friendId; // Đã đổi sang String
    private String status; // "PENDING", "ACCEPTED", "DECLINED"

    public Friendship(String userId, String friendId, String status) {
        this.userId = userId;
        this.friendId = friendId;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFriendId() { return friendId; }
    public void setFriendId(String friendId) { this.friendId = friendId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
