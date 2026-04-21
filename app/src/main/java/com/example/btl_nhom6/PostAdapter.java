package com.example.btl_nhom6;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> postList;
    private OnPostLongClickListener longClickListener;
    private OnPostActionListener actionListener;
    private AppDatabase db;
    private int currentUserId;

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
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        db = AppDatabase.getInstance(parent.getContext());
        SharedPreferences pref = parent.getContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        currentUserId = pref.getInt("current_user_id", -1);
        
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

        // Xử lý hình ảnh
        if (post.getImageUri() != null && !post.getImageUri().isEmpty()) {
            holder.ivPostImage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(post.getImageUri())
                    .into(holder.ivPostImage);
        } else {
            holder.ivPostImage.setVisibility(View.GONE);
        }

        // Cập nhật số lượng Like và Comment
        int likeCount = db.likeDao().getLikeCount(post.getId());
        int commentCount = db.commentDao().getCommentCount(post.getId());
        holder.tvLikeCount.setText(likeCount + " Lượt thích");
        holder.tvCommentCount.setText(commentCount + " Bình luận");

        // Kiểm tra xem user hiện tại đã like chưa
        Like userLike = db.likeDao().getLike(post.getId(), currentUserId);
        if (userLike != null) {
            holder.btnLike.setTextColor(Color.parseColor("#1877F2"));
            holder.btnLike.setText("Đã thích");
        } else {
            holder.btnLike.setTextColor(Color.parseColor("#606770"));
            holder.btnLike.setText("Thích");
        }

        // Xử lý nút Like
        holder.btnLike.setOnClickListener(v -> {
            if (userLike != null) {
                db.likeDao().deleteLike(post.getId(), currentUserId);
            } else {
                db.likeDao().insertLike(new Like(post.getId(), currentUserId));
            }
            notifyItemChanged(position);
        });

        // Xử lý nút Comment
        holder.btnComment.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onCommentClick(post);
            }
        });

        // Xử lý nút Share
        holder.btnShare.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onShareClick(post);
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (longClickListener != null) {
                    longClickListener.onPostLongClick(post);
                }
                return true;
            }
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
