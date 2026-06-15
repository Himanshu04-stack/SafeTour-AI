package com.safetour.ai.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.safetour.ai.R;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final List<String> sessionTitles;
    private final OnSessionClickListener listener;

    public interface OnSessionClickListener {
        void onSessionClick(String sessionId);
    }

    public HistoryAdapter(List<String> sessionTitles, OnSessionClickListener listener) {
        this.sessionTitles = sessionTitles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_history_drawer, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        String title = sessionTitles.get(position);
        holder.tvTitle.setText(title);
        holder.itemView.setOnClickListener(v -> listener.onSessionClick(title));
    }

    @Override
    public int getItemCount() {
        return sessionTitles.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvHistoryTitle);
        }
    }
}
