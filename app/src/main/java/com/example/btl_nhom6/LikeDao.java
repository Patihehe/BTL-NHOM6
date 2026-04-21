package com.example.btl_nhom6;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface LikeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertLike(Like like);

    @Delete
    void deleteLike(Like like);

    @Query("SELECT * FROM likes WHERE postId = :postId AND userId = :userId LIMIT 1")
    Like getLike(int postId, int userId);

    @Query("SELECT COUNT(*) FROM likes WHERE postId = :postId")
    int getLikeCount(int postId);

    @Query("DELETE FROM likes WHERE postId = :postId AND userId = :userId")
    void deleteLike(int postId, int userId);
}
