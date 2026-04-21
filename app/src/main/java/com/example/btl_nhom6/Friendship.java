package com.example.btl_nhom6;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "friendships")
public class Friendship {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private int userId; // The person who initiated/owns the relationship
    private int friendId; // The person on the other end
    private String status; // "PENDING", "ACCEPTED", "DECLINED"

    public Friendship(int userId, int friendId, String status) {
        this.userId = userId;
        this.friendId = friendId;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getFriendId() { return friendId; }
    public void setFriendId(int friendId) { this.friendId = friendId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
