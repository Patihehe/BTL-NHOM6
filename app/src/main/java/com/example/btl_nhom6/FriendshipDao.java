package com.example.btl_nhom6;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import java.util.List;

@Dao
public interface FriendshipDao {
    @Insert
    void sendFriendRequest(Friendship friendship);

    @Update
    void updateFriendshipStatus(Friendship friendship);

    @Delete
    void deleteFriendship(Friendship friendship);

    // Get relationship between two users
    @Query("SELECT * FROM friendships WHERE (userId = :userId AND friendId = :friendId) OR (userId = :friendId AND friendId = :userId) LIMIT 1")
    Friendship getFriendship(int userId, int friendId);

    // List of friends (Accepted)
    @Query("SELECT * FROM users WHERE id IN (SELECT friendId FROM friendships WHERE userId = :userId AND status = 'ACCEPTED' UNION SELECT userId FROM friendships WHERE friendId = :userId AND status = 'ACCEPTED')")
    List<User> getFriends(int userId);

    // List of pending requests sent TO the user
    @Query("SELECT * FROM users WHERE id IN (SELECT userId FROM friendships WHERE friendId = :userId AND status = 'PENDING')")
    List<User> getPendingRequests(int userId);

    // Search users by name or email, excluding the current user
    @Query("SELECT * FROM users WHERE (fullName LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%') AND id != :currentUserId")
    List<User> searchUsers(String query, int currentUserId);
}
