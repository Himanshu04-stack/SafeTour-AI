package com.safetour.ai.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.safetour.ai.R;
import android.text.Html;
import android.text.Spanned;
import java.util.List;
import android.content.Context;
import com.bumptech.glide.Glide;
import com.safetour.ai.repository.ProfileRepository;
import com.google.android.material.imageview.ShapeableImageView;
import android.net.Uri;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI = 2;
    private final List<ChatMessage> messages;
    private final Context context;
    private final ProfileRepository profileRepo;

    public ChatAdapter(Context context, List<ChatMessage> messages) {
        this.context = context;
        this.profileRepo = new ProfileRepository(context);
        this.messages = new java.util.ArrayList<>(messages);
    }
    
    public void updateMessages(List<ChatMessage> newMessages) {
        this.messages.clear();
        this.messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (messages.get(position).isUser()) {
            return VIEW_TYPE_USER;
        } else {
            return VIEW_TYPE_AI;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_ai, parent, false);
            return new AiViewHolder(view);
        }
    }

    private Spanned formatMarkdown(String text) {
        String html = text.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
        html = html.replaceAll("\\*(.*?)\\*", "<i>$1</i>");
        html = html.replace("\n", "<br>");
        return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_USER) {
            UserViewHolder userHolder = (UserViewHolder) holder;
            userHolder.tvMessage.setText(message.getText());
            
            String uriString = profileRepo.getString("user_photo_uri", "");
            if (!uriString.isEmpty()) {
                Glide.with(context).load(Uri.parse(uriString)).centerCrop().into(userHolder.ivAvatar);
                userHolder.ivAvatar.setImageTintList(null);
                userHolder.ivAvatar.setPadding(0, 0, 0, 0);
            } else {
                userHolder.ivAvatar.setImageResource(R.drawable.ic_nav_profile);
                userHolder.ivAvatar.setImageTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.theme_icon_primary)));
            }
            
        } else {
            ((AiViewHolder) holder).tvMessage.setText(formatMarkdown(message.getText()));
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        ShapeableImageView ivAvatar;
        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
        }
    }

    static class AiViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        AiViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }
}
