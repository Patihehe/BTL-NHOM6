package com.example.btl_nhom6;

public class Post {
    private String postId; // ID từ Firebase
    private String userId;
    private String userName;
    private String content;
    private String imageUri;
    private long timestamp;

    // Cần constructor rỗng cho Firebase
    public Post() {}

    public Post(String userId, String userName, String content, String imageUri, long timestamp) {
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.imageUri = imageUri;
        this.timestamp = timestamp;
    }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
