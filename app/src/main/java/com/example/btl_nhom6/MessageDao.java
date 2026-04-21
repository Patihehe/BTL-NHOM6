package com.example.btl_nhom6;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface MessageDao {
    @Insert
    void sendMessage(Message message);

    @Query("SELECT * FROM messages WHERE (senderEmail = :user1 AND receiverEmail = :user2) OR (senderEmail = :user2 AND receiverEmail = :user1) ORDER BY timestamp ASC")
    List<Message> getChatHistory(String user1, String user2);

    @Query("SELECT DISTINCT receiverEmail FROM messages WHERE senderEmail = :userEmail UNION SELECT DISTINCT senderEmail FROM messages WHERE receiverEmail = :userEmail")
    List<String> getRecentChatUsers(String userEmail);
}
