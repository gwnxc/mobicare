package com.example.mobicare;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ConsultationAdapter extends RecyclerView.Adapter<ConsultationAdapter.ConsultationViewHolder> {

    private List<Consultation> consultationList;
    private OnItemClickListener listener;

    // Interface for handling clicks to go to "Details"
    public interface OnItemClickListener {
        void onItemClick(Consultation consultation);
    }

    public ConsultationAdapter(List<Consultation> list, OnItemClickListener listener) {
        this.consultationList = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConsultationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_consultation, parent, false);
        return new ConsultationViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ConsultationViewHolder holder, int position) {
        Consultation item = consultationList.get(position);

        holder.tvName.setText(item.patientName);
        holder.tvReason.setText(item.reason);
        holder.tvDate.setText(item.date);
        holder.tvTime.setText(item.time);
        holder.tvStatus.setText(item.status);

        // UI Logic: Change colors based on status
        if ("completed".equalsIgnoreCase(item.status)) {
            holder.ivIcon.setBackgroundResource(R.drawable.circle_light_green);
            holder.ivIcon.setImageResource(R.drawable.ic_completed);
            holder.ivIcon.setColorFilter(Color.parseColor("#4CAF50"));
            holder.tvStatus.setBackgroundResource(R.drawable.badge_green);
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            // Default Scheduled (Blue)
            holder.ivIcon.setBackgroundResource(R.drawable.circle_light_blue);
            holder.ivIcon.setImageResource(R.drawable.ic_consultation);
            holder.ivIcon.setColorFilter(Color.parseColor("#1B75BC"));
            holder.tvStatus.setBackgroundResource(R.drawable.badge_blue);
            holder.tvStatus.setTextColor(Color.parseColor("#1B75BC"));
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return consultationList.size();
    }

    static class ConsultationViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvReason, tvDate, tvTime, tvStatus;
        ImageView ivIcon;

        ConsultationViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvConsultationName);
            tvReason = itemView.findViewById(R.id.tvConsultationReason);
            tvDate = itemView.findViewById(R.id.tvConsultationDate);
            tvTime = itemView.findViewById(R.id.tvConsultationTime);
            tvStatus = itemView.findViewById(R.id.tvConsultationStatus);
            ivIcon = itemView.findViewById(R.id.ivConsultationIcon);
        }
    }
}