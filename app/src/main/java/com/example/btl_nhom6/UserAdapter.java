package com.example.btl_nhom6;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private OnUserActionListener actionListener;
    private int currentUserId;
    private AppDatabase db;

    public interface OnUserActionListener {
        void onAction(User user);
        void onDecline(User user);
        void onUserClick(User user);
    }

    public UserAdapter(List<User> userList, int currentUserId, AppDatabase db, OnUserActionListener actionListener) {
        this.userList = userList;
        this.currentUserId = currentUserId;
        this.db = db;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.tvUserName.setText(user.getFullName());
        holder.tvUserEmail.setText(user.getEmail());

        if (user.getAvatarUri() != null && !user.getAvatarUri().isEmpty()) {
            Glide.with(holder.itemView.getContext()).load(user.getAvatarUri()).into(holder.ivUserAvatar);
        } else {
            holder.ivUserAvatar.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        // Determine relationship status and update button
        Friendship friendship = db.friendshipDao().getFriendship(currentUserId, user.getId());

        holder.btnDecline.setVisibility(View.GONE);
        holder.btnAction.setEnabled(true);
        if (friendship == null) {
            holder.btnAction.setText("Thêm bạn");
        } else if (friendship.getStatus().equals("PENDING")) {
            if (friendship.getUserId() == currentUserId) {
                holder.btnAction.setText("Đã gửi");
                holder.btnAction.setEnabled(false);
            } else {
                holder.btnAction.setText("Chấp nhận");
                holder.btnDecline.setVisibility(View.VISIBLE);
            }
        } else if (friendship.getStatus().equals("ACCEPTED")) {
            holder.btnAction.setText("Hủy kết bạn");
        }

        holder.btnAction.setOnClickListener(v -> actionListener.onAction(user));
        holder.btnDecline.setOnClickListener(v -> actionListener.onDecline(user));
        holder.itemView.setOnClickListener(v -> actionListener.onUserClick(user));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivUserAvatar;
        TextView tvUserName, tvUserEmail;
        Button btnAction;
        ImageButton btnDecline;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            btnAction = itemView.findViewById(R.id.btnAction);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }
}
