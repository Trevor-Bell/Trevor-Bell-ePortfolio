package com.example.trevor_bell_cs_360_final_project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

public class EntryAdapter extends BaseAdapter {

    private Context context;
    private List<WeightEntry> entries;

    // Listener for edit and delete actions
    private OnEntryActionListener actionListener;

    // Unified interface for both delete and edit actions
    public interface OnEntryActionListener {
        void onDeleteClick(int entryId);
        void onEditClick(WeightEntry entry);
    }

    // Constructor receives context, entry list, and action listener
    public EntryAdapter(Context context, List<WeightEntry> entries, OnEntryActionListener listener) {
        this.context = context;
        this.entries = entries;
        this.actionListener = listener;
    }

    @Override
    public int getCount() {
        return entries.size();
    }

    @Override
    public Object getItem(int position) {
        return entries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return entries.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Inflate a new view for each list item
        View view = LayoutInflater.from(context).inflate(R.layout.entry_item, parent, false);

        // Get references to UI elements in the item layout
        TextView entryText = view.findViewById(R.id.entryText);
        Button deleteButton = view.findViewById(R.id.deleteButton);
        Button editButton = view.findViewById(R.id.editButton);

        // Get the current entry and format its display text
        WeightEntry entry = entries.get(position);
        String display = "Weight: " + entry.getWeight() + " lbs\nNotes: " + entry.getNotes();
        entryText.setText(display);

        // Handle delete button click
        deleteButton.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDeleteClick(entry.getId());
            }
        });

        // Handle edit button click
        editButton.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onEditClick(entry);
            }
        });

        return view;
    }
}