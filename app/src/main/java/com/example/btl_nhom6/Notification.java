package com.example.btl_nhom6;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notifications")
public class Notification {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String userEmail; // Người nhận thông báo
    private String content;   // Nội dung (vd: "A đã thích bài viết của bạn")
    private long timestamp;
    private boolean isRead;

    public Notification(String userEmail, String content, long timestamp) {
        this.userEmail = userEmail;
        this.content = content;
        this.timestamp = timestamp;
        this.isRead = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
