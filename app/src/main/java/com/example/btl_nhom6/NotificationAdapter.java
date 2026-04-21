package com.example.btl_nhom6;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notificationList;

    public NotificationAdapter(List<Notification> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);
        holder.tvContent.setText(notification.getContent());
        
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
        holder.tvTime.setText(sdf.format(new Date(notification.getTimestamp())));

        if (notification.isRead()) {
            holder.unreadDot.setVisibility(View.GONE);
            holder.itemView.setAlpha(0.6f);
        } else {
            holder.unreadDot.setVisibility(View.VISIBLE);
            holder.itemView.setAlpha(1.0f);
        }
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvTime;
        View unreadDot;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tvNotifContent);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
            unreadDot = itemView.findViewById(R.id.vUnreadDot);
        }
    }
}
