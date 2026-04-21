package com.example.btl_nhom6;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> postList;
    private OnPostLongClickListener longClickListener;
    private OnPostActionListener actionListener;
    private FirebaseFirestore db;
    private String currentUserId;
    private String currentUserName;

    public interface OnPostLongClickListener {
        void onPostLongClick(Post post);
    }

    public interface OnPostActionListener {
        void onCommentClick(Post post);
        void onShareClick(Post post);
    }

    public PostAdapter(List<Post> postList, OnPostLongClickListener longClickListener, OnPostActionListener actionListener) {
        this.postList = postList;
        this.longClickListener = longClickListener;
        this.actionListener = actionListener;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SharedPreferences pref = parent.getContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        currentUserId = pref.getString("current_user_id", "");
        currentUserName = pref.getString("current_user_name", "Ai đó");
        
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        holder.tvUserName.setText(post.getUserName());
        holder.tvPostContent.setText(post.getContent());

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
        holder.tvTimestamp.setText(sdf.format(new Date(post.getTimestamp())));

        if (post.getImageUri() != null && !post.getImageUri().isEmpty()) {
            holder.ivPostImage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(post.getImageUri())
                    .into(holder.ivPostImage);
        } else {
            holder.ivPostImage.setVisibility(View.GONE);
        }

        // Lấy số lượng Like Realtime từ Firestore
        db.collection("likes")
                .whereEqualTo("postId", post.getPostId())
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        int count = value.size();
                        holder.tvLikeCount.setText(count + " Lượt thích");
                        
                        // Kiểm tra xem user hiện tại đã like chưa
                        boolean isLiked = false;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value) {
                            if (currentUserId.equals(doc.getString("userId"))) {
                                isLiked = true;
                                break;
                            }
                        }
                        
                        if (isLiked) {
                            holder.btnLike.setTextColor(Color.parseColor("#1877F2"));
                            holder.btnLike.setText("Đã thích");
                            holder.btnLike.setOnClickListener(v -> {
                                // Bỏ like
                                db.collection("likes")
                                        .whereEqualTo("postId", post.getPostId())
                                        .whereEqualTo("userId", currentUserId)
                                        .get()
                                        .addOnSuccessListener(queryDocumentSnapshots -> {
                                            for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                                                doc.getReference().delete();
                                            }
                                        });
                            });
                        } else {
                            holder.btnLike.setTextColor(Color.parseColor("#606770"));
                            holder.btnLike.setText("Thích");
                            holder.btnLike.setOnClickListener(v -> {
                                // Thêm like
                                Like like = new Like(post.getPostId(), currentUserId);
                                db.collection("likes").add(like);
                                
                                // Tạo thông báo
                                if (!post.getUserId().equals(currentUserId)) {
                                    db.collection("users").document(post.getUserId()).get()
                                            .addOnSuccessListener(userDoc -> {
                                                String email = userDoc.getString("email");
                                                if (email != null) {
                                                    Notification notif = new Notification(email, currentUserName + " đã thích bài viết của bạn", System.currentTimeMillis());
                                                    db.collection("notifications").add(notif);
                                                }
                                            });
                                }
                            });
                        }
                    }
                });

        // Lấy số lượng Comment Realtime
        db.collection("comments")
                .whereEqualTo("postId", post.getPostId())
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        holder.tvCommentCount.setText(value.size() + " Bình luận");
                    }
                });

        holder.btnComment.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onCommentClick(post);
            }
        });

        holder.btnShare.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onShareClick(post);
            }
        });

        holder.tvUserName.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), ProfileActivity.class);
            intent.putExtra("profile_user_id", post.getUserId());
            holder.itemView.getContext().startActivity(intent);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onPostLongClick(post);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvTimestamp, tvPostContent;
        ImageView ivPostImage;
        TextView tvLikeCount, tvCommentCount;
        TextView btnLike, btnComment, btnShare;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
            btnShare = itemView.findViewById(R.id.btnShare);
        }
    }
}
