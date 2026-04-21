package com.example.btl_nhom6;

public class User {
    private String id;
    private String fullName;
    private String email;
    private String password;
    private String avatarUri;
    private String coverPhotoUri;
    private String bio;
    private String location;
    private String dob;

    public User() {} // Cần cho Firebase

    public User(String id, String fullName, String email, String password) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getAvatarUri() { return avatarUri; }
    public void setAvatarUri(String avatarUri) { this.avatarUri = avatarUri; }
    public String getCoverPhotoUri() { return coverPhotoUri; }
    public void setCoverPhotoUri(String coverPhotoUri) { this.coverPhotoUri = coverPhotoUri; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }
}
