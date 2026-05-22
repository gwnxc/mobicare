package com.example.mobicare;

import android.graphics.Color;
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
        // Make sure your XML file is actually named item_notification
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification n = notifications.get(position);

        holder.tvTitle.setText(n.title != null ? n.title : "No Title");
        holder.tvMessage.setText(n.message != null ? n.message : "");
        holder.tvDate.setText(n.getDisplayDate());

        boolean isRead = Boolean.TRUE.equals(n.isRead);
        holder.ivUnreadDot.setVisibility(isRead ? View.GONE : View.VISIBLE);

        // --- Custom Icon & Color Logic for the Live Feed ---
        if (n.title != null) {
            String t = n.title.toLowerCase();
            if (t.contains("upcoming") || t.contains("schedule")) {
                holder.ivIcon.setImageResource(android.R.drawable.ic_menu_today);
                holder.ivIcon.setColorFilter(Color.parseColor("#1976D2")); // Blue
            } else if (t.contains("cancel")) {
                holder.ivIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                holder.ivIcon.setColorFilter(Color.parseColor("#F44336")); // Red
            } else if (t.contains("stock") || t.contains("expir")) {
                holder.ivIcon.setImageResource(android.R.drawable.ic_dialog_alert);
                holder.ivIcon.setColorFilter(Color.parseColor("#FF9800")); // Orange
            } else {
                holder.ivIcon.setImageResource(R.drawable.ic_notifications_active);
                holder.ivIcon.setColorFilter(Color.parseColor("#4CAF50")); // Green
            }
        }

        // Dim the icon slightly if it has been read
        holder.ivIcon.setAlpha(isRead ? 0.5f : 1.0f);

        // --- The Safe Click Listener ---
        holder.itemView.setOnClickListener(v -> {
            if (!Boolean.TRUE.equals(n.isRead) && n.id != null) {

                // If it's a REAL notification (no underscore in the generated push ID)
                if (!n.id.contains("_")) {
                    FirebaseDatabase.getInstance().getReference("Notifications")
                            .child(n.id)
                            .child("isRead").setValue(true)
                            .addOnSuccessListener(aVoid -> {
                                n.isRead = true;
                                notifyItemChanged(position);
                            });
                }
                // If it's a VIRTUAL feed alert (from inventory/consultations)
                else {
                    n.isRead = true;
                    notifyItemChanged(position);
                }
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
            // Ensure these IDs exactly match what is inside your item_notification.xml file!
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvMessage = itemView.findViewById(R.id.tvNotifMessage);
            tvDate = itemView.findViewById(R.id.tvNotifDate);
            ivIcon = itemView.findViewById(R.id.ivNotifIcon);
            ivUnreadDot = itemView.findViewById(R.id.ivUnreadDot);
        }
    }
}