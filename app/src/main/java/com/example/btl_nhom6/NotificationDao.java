package com.example.btl_nhom6;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface NotificationDao {
    @Insert
    void insertNotification(Notification notification);

    @Query("SELECT * FROM notifications WHERE userEmail = :userEmail ORDER BY timestamp DESC")
    List<Notification> getNotificationsForUser(String userEmail);

    @Update
    void updateNotification(Notification notification);
}
