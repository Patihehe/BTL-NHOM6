package com.example.btl_nhom6;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private OnUserActionListener actionListener;
    private String currentUserId;
    private FirebaseFirestore db; // Đổi từ AppDatabase sang FirebaseFirestore

    public interface OnUserActionListener {
        void onAction(User user);
        void onDecline(User user);
        void onUserClick(User user);
    }

    public UserAdapter(List<User> userList, String currentUserId, FirebaseFirestore db, OnUserActionListener actionListener) {
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

        // Logic kiểm tra trạng thái bạn bè trên Firebase
        holder.btnAction.setText("Thêm bạn");
        holder.btnDecline.setVisibility(View.GONE);
        holder.btnAction.setEnabled(true);

        db.collection("friendships")
                .whereIn("userId", java.util.Arrays.asList(currentUserId, user.getId()))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String uid = doc.getString("userId");
                        String fid = doc.getString("friendId");
                        String status = doc.getString("status");

                        if ((uid.equals(currentUserId) && fid.equals(user.getId())) ||
                            (uid.equals(user.getId()) && fid.equals(currentUserId))) {
                            
                            if (status.equals("PENDING")) {
                                if (uid.equals(currentUserId)) {
                                    holder.btnAction.setText("Đã gửi");
                                    holder.btnAction.setEnabled(false);
                                } else {
                                    holder.btnAction.setText("Chấp nhận");
                                    holder.btnDecline.setVisibility(View.VISIBLE);
                                }
                            } else if (status.equals("ACCEPTED")) {
                                holder.btnAction.setText("Hủy kết bạn");
                            }
                            break;
                        }
                    }
                });

        holder.btnAction.setOnClickListener(v -> actionListener.onAction(user));
        holder.btnDecline.setOnClickListener(v -> actionListener.onDecline(user));
        holder.itemView.setOnClickListener(v -> actionListener.onUserClick(user));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserAvatar;
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
