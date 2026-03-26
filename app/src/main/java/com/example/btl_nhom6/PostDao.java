package com.example.btl_nhom6;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface PostDao {
    @Insert
    void insertPost(Post post);

    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    List<Post> getAllPosts();

    @Delete
    void deletePost(Post post);
}
