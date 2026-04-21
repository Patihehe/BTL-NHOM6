package com.example.btl_nhom6;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private List<Message> messageList;
    private String currentUserEmail;

    public ChatAdapter(List<Message> messageList, String currentUserEmail) {
        this.messageList = messageList;
        this.currentUserEmail = currentUserEmail;
    }

    @Override
    public int getItemViewType(int position) {
        if (messageList.get(position).getSenderEmail().equals(currentUserEmail)) {
            return TYPE_SENT;
        } else {
            return TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).tvMessage.setText(message.getContent());
        } else {
            ((ReceivedMessageViewHolder) holder).tvMessage.setText(message.getContent());
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        SentMessageViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessageSent);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        ReceivedMessageViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessageReceived);
        }
    }
}
