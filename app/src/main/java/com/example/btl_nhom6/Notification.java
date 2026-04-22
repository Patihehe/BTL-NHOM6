package com.example.btl_nhom6;

import com.google.firebase.firestore.PropertyName;

public class Notification {
    private String id;
    private String userEmail; 
    private String content;   
    private long timestamp;
    private boolean read; // Đổi tên thành read để Firebase tự hiểu

    public Notification() {}

    public Notification(String userEmail, String content, long timestamp) {
        this.userEmail = userEmail;
        this.content = content;
        this.timestamp = timestamp;
        this.read = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @PropertyName("isRead") // Ép Firebase dùng đúng tên trường này
    public boolean isRead() { return read; }
    @PropertyName("isRead")
    public void setRead(boolean read) { this.read = read; }
}
