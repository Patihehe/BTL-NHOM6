package com.example.btl_nhom6;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

public class RecentChatAdapter extends RecyclerView.Adapter<RecentChatAdapter.RecentChatViewHolder> {

    private List<User> chatUsers;
    private OnChatUserClickListener listener;
    private String currentUserEmail;
    private FirebaseFirestore db;

    public interface OnChatUserClickListener {
        void onUserClick(User user);
    }

    public RecentChatAdapter(List<User> chatUsers, String currentUserEmail, OnChatUserClickListener listener) {
        this.chatUsers = chatUsers;
        this.currentUserEmail = currentUserEmail;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public RecentChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_chat, parent, false);
        return new RecentChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentChatViewHolder holder, int position) {
        User user = chatUsers.get(position);
        holder.tvUserName.setText(user.getFullName());
        holder.tvLastMessage.setText(user.getEmail());

        // KIỂM TRA TIN NHẮN CHƯA ĐỌC TỪ NGƯỜI NÀY
        db.collection("messages")
                .whereEqualTo("senderEmail", user.getEmail())
                .whereEqualTo("receiverEmail", currentUserEmail)
                .whereEqualTo("isRead", false)
                .addSnapshotListener((value, error) -> {
                    if (value != null && value.size() > 0) {
                        holder.vUnreadDot.setVisibility(View.VISIBLE);
                    } else {
                        holder.vUnreadDot.setVisibility(View.GONE);
                    }
                });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatUsers.size();
    }

    static class RecentChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvLastMessage;
        View vUnreadDot;

        public RecentChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            vUnreadDot = itemView.findViewById(R.id.vUnreadDot);
        }
    }
}
