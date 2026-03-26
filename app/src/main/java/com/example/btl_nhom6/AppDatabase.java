package com.example.btl_nhom6;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {User.class, Post.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract UserDao userDao();
    public abstract PostDao postDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "facebook_db")
                    .fallbackToDestructiveMigration() // Xóa dữ liệu cũ khi đổi version (chỉ dùng khi đang dev)
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }
}
