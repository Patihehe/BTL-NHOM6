package com.example.btl_nhom6;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvChatHistory;
    private EditText etMessageInput;
    private ImageButton btnSendMessage;
    private ChatAdapter chatAdapter;
    private List<Message> messageList;
    private AppDatabase db;
    private String currentUserEmail;
    private String receiverEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = AppDatabase.getInstance(this);

        // Lấy email người dùng hiện tại
        SharedPreferences pref = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        // Lưu ý: Bạn cần đảm bảo đã lưu email vào AppPrefs lúc đăng nhập
        currentUserEmail = pref.getString("current_user_email", ""); 

        // Lấy email người nhận từ Intent
        receiverEmail = getIntent().getStringExtra("receiver_email");
        String receiverName = getIntent().getStringExtra("receiver_name");

        Toolbar toolbar = findViewById(R.id.chatToolbar);
        toolbar.setTitle(receiverName != null ? receiverName : "Chat");
        setSupportActionBar(toolbar);

        rvChatHistory = findViewById(R.id.rvChatHistory);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSendMessage = findViewById(R.id.btnSendMessage);

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList, currentUserEmail);
        rvChatHistory.setLayoutManager(new LinearLayoutManager(this));
        rvChatHistory.setAdapter(chatAdapter);

        loadChatHistory();

        btnSendMessage.setOnClickListener(v -> {
            String content = etMessageInput.getText().toString().trim();
            if (!content.isEmpty()) {
                Message message = new Message(currentUserEmail, receiverEmail, content, System.currentTimeMillis());
                db.messageDao().sendMessage(message);
                etMessageInput.setText("");
                loadChatHistory();
                rvChatHistory.scrollToPosition(messageList.size() - 1);
            }
        });
    }

    private void loadChatHistory() {
        messageList.clear();
        messageList.addAll(db.messageDao().getChatHistory(currentUserEmail, receiverEmail));
        chatAdapter.notifyDataSetChanged();
    }
}
