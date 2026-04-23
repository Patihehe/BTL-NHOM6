package com.example.btl_nhom6;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvChatHistory;
    private EditText etMessageInput;
    private ImageButton btnSendMessage;
    private ChatAdapter chatAdapter;
    private List<Message> messageList;
    private FirebaseFirestore db; 
    private String currentUserEmail;
    private String receiverEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = FirebaseFirestore.getInstance();

        SharedPreferences pref = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        currentUserEmail = pref.getString("current_user_email", ""); 

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

        listenForMessages(); 

        btnSendMessage.setOnClickListener(v -> {
            String content = etMessageInput.getText().toString().trim();
            if (!content.isEmpty()) {
                Message message = new Message(currentUserEmail, receiverEmail, content, System.currentTimeMillis());
                
                db.collection("messages").add(message)
                        .addOnSuccessListener(documentReference -> {
                            String id = documentReference.getId();
                            db.collection("messages").document(id).update("messageId", id);
                            etMessageInput.setText("");
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(ChatActivity.this, "Lỗi gửi tin nhắn", Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    private void listenForMessages() {
        db.collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        messageList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Message msg = doc.toObject(Message.class);
                            if ((msg.getSenderEmail().equals(currentUserEmail) && msg.getReceiverEmail().equals(receiverEmail)) ||
                                (msg.getSenderEmail().equals(receiverEmail) && msg.getReceiverEmail().equals(currentUserEmail))) {
                                
                                // LOGIC ĐÁNH DẤU ĐÃ ĐỌC:
                                // Nếu bạn là người nhận và tin nhắn chưa được đọc
                                if (msg.getReceiverEmail().equals(currentUserEmail) && !msg.isRead()) {
                                    markMessageAsRead(doc.getId());
                                }
                                
                                messageList.add(msg);
                            }
                        }
                        chatAdapter.notifyDataSetChanged();
                        if (!messageList.isEmpty()) {
                            rvChatHistory.scrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    private void markMessageAsRead(String messageId) {
        db.collection("messages").document(messageId).update("isRead", true);
    }
}
