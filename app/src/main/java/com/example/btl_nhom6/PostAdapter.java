package com.example.btl_nhom6;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
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

    public interface OnPostLongClickListener {
        void onPostLongClick(Post post);
    }

    public PostAdapter(List<Post> postList, OnPostLongClickListener longClickListener) {
        this.postList = postList;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
        }
    }
}
