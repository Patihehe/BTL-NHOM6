package com.example.btl_nhom6;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecentChatAdapter extends RecyclerView.Adapter<RecentChatAdapter.RecentChatViewHolder> {

    private List<User> chatUsers;
    private OnChatUserClickListener listener;

    public interface OnChatUserClickListener {
        void onUserClick(User user);
    }

    public RecentChatAdapter(List<User> chatUsers, OnChatUserClickListener listener) {
        this.chatUsers = chatUsers;
        this.listener = listener;
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
        holder.tvLastMessage.setText(user.getEmail()); // Hiển thị email hoặc bạn có thể query tin nhắn cuối cùng

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

        public RecentChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
        }
    }
}
