package com.example.mobicare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder> implements Filterable {

    private List<Object> listFull; // For searching
    private List<Object> listDisplay; // What is currently on screen

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
        Object item = listDisplay.get(position);

        if (item instanceof Mother) {
            Mother m = (Mother) item;
            // Use the GETTER method here
            holder.tvName.setText(m.getFullName());
            holder.tvType.setText("Guardian");
        } else if (item instanceof Child) {
            Child c = (Child) item;
            // USE THIS NAME since you kept it as placeOfBirth
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
                    // Check for Mother/Guardian
                    if (item instanceof Mother) {
                        Mother m = (Mother) item;
                        // This matches the name OR the "Guardian" chip
                        if (m.getFullName().toLowerCase().contains(pattern) || "guardian".contains(pattern)) {
                            filteredList.add(item);
                        }
                    }
                    // Check for Child
                    else if (item instanceof Child) {
                        Child c = (Child) item;
                        String fullName = (c.firstName + " " + c.lastName).toLowerCase();
                        // This matches the name OR the "Child" chip
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