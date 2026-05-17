package com.example.mobicare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder> implements Filterable {

    private List<Object> listFull;
    private List<Object> listDisplay;

    public RecordAdapter(List<Object> list) {
        this.listDisplay = list;
        this.listFull = new ArrayList<>(list);
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record, parent, false);
        return new RecordViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        // FIX 1: Use listDisplay instead of the non-existent dataList variable
        Object patientItem = listDisplay.get(position);

        holder.itemView.setOnClickListener(v -> {
            Bundle args = new Bundle();

            if (patientItem instanceof Mother) {
                Mother mother = (Mother) patientItem;
                String momName = mother.getFullName();

                args.putString("selectedChildId", mother.getLinkedUid());
                args.putString("selectedChildName", momName);

                // FIXED ID: Changed to match your exact nav_graph action ID
                Navigation.findNavController(v).navigate(R.id.action_viewRecords_to_myRecord, args);

            } else if (patientItem instanceof Child) {
                Child child = (Child) patientItem;
                String childName = child.firstName + " " + child.lastName;

                args.putString("selectedChildId", child.getChildId());
                args.putString("selectedChildName", childName);

                // FIXED ID: Changed to match your exact nav_graph action ID
                Navigation.findNavController(v).navigate(R.id.action_viewRecords_to_healthRecords, args);
            }
        });

        // --- FIXED BINDING LOGIC (Using patientItem consistently) ---
        if (patientItem instanceof Mother) {
            Mother m = (Mother) patientItem;
            holder.tvName.setText(m.getFullName());
            holder.tvType.setText("Guardian");
        } else if (patientItem instanceof Child) {
            Child c = (Child) patientItem;
            holder.tvName.setText(c.firstName + " " + c.lastName);
            holder.tvType.setText("Child");
        }
    }

    @Override
    public int getItemCount() {
        return listDisplay.size();
    }

    public void updateList(List<Object> newList) {
        this.listDisplay = newList;
        this.listFull = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return recordFilter;
    }

    private Filter recordFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Object> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(listFull);
            } else {
                String pattern = constraint.toString().toLowerCase().trim();
                for (Object item : listFull) {
                    if (item instanceof Mother) {
                        Mother m = (Mother) item;
                        if (m.getFullName().toLowerCase().contains(pattern) || "guardian".contains(pattern)) {
                            filteredList.add(item);
                        }
                    }
                    else if (item instanceof Child) {
                        Child c = (Child) item;
                        String fullName = (c.firstName + " " + c.lastName).toLowerCase();
                        if (fullName.contains(pattern) || "child".contains(pattern)) {
                            filteredList.add(item);
                        }
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            listDisplay.clear();
            if (results.values != null) {
                listDisplay.addAll((List) results.values);
            }
            notifyDataSetChanged();
        }
    };

    static class RecordViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvType;
        RecordViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvRecordName);
            tvType = itemView.findViewById(R.id.tvRecordType);
        }
    }
}