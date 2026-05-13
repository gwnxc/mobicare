package com.example.mobicare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.FirebaseDatabase;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<Notification> notifications;

    public NotificationAdapter(List<Notification> notifications) {
        this.notifications = notifications;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification n = notifications.get(position);

        holder.tvTitle.setText(n.title);
        holder.tvMessage.setText(n.message);
        holder.tvDate.setText(n.date);

        // 1. Safe Unread Status Dot Logic
        // Use Boolean.TRUE.equals to safely check the Boolean Object
        boolean isRead = Boolean.TRUE.equals(n.isRead);

        holder.ivUnreadDot.setVisibility(isRead ? View.GONE : View.VISIBLE);

        // 2. Perspective Icon Logic
        if (isRead) {
            holder.ivIcon.setImageResource(R.drawable.ic_notifications);
            holder.ivIcon.setAlpha(0.6f);
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_notifications_active);
            holder.ivIcon.setAlpha(1.0f);
        }

        // 3. Mark as Read on Click
        holder.itemView.setOnClickListener(v -> {
            // Safe check for ID and Read status
            if (!isRead && n.id != null) {
                FirebaseDatabase.getInstance().getReference("Notifications")
                        .child(n.id)
                        .child("isRead").setValue(true);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvDate;
        ImageView ivIcon, ivUnreadDot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvMessage = itemView.findViewById(R.id.tvNotifMessage);
            tvDate = itemView.findViewById(R.id.tvNotifDate);
            ivIcon = itemView.findViewById(R.id.ivNotifIcon);
            ivUnreadDot = itemView.findViewById(R.id.ivUnreadDot);
        }
    }
}