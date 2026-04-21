package com.example.btl_nhom6;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface CommentDao {
    @Insert
    void insertComment(Comment comment);

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    List<Comment> getCommentsByPostId(int postId);

    @Query("SELECT COUNT(*) FROM comments WHERE postId = :postId")
    int getCommentCount(int postId);
}
