package com.example.reminderandnotificationapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {

    public interface OnReminderDeleteListener {
        void onDeleteClicked(Reminder reminder);
    }

    private final List<Reminder> reminders = new ArrayList<>();
    private final OnReminderDeleteListener deleteListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ReminderAdapter(OnReminderDeleteListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void submitList(List<Reminder> items) {
        reminders.clear();
        reminders.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.reminder_card_item, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        Reminder reminder = reminders.get(position);
        holder.titleText.setText(reminder.getTitle());
        holder.descriptionText.setText(reminder.getDescription());
        holder.dateText.setText(holder.itemView.getContext().getString(
                R.string.reminder_date_value,
                dateFormat.format(reminder.getReminderTime())
        ));
        holder.timeText.setText(holder.itemView.getContext().getString(
                R.string.reminder_time_value,
                timeFormat.format(reminder.getReminderTime())
        ));
        holder.deleteButton.setOnClickListener(v -> deleteListener.onDeleteClicked(reminder));
    }

    @Override
    public int getItemCount() {
        return reminders.size();
    }

    public static class ReminderViewHolder extends RecyclerView.ViewHolder {
        final TextView titleText;
        final TextView descriptionText;
        final TextView dateText;
        final TextView timeText;
        final ImageButton deleteButton;

        ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.reminderTitle);
            descriptionText = itemView.findViewById(R.id.reminderDescription);
            dateText = itemView.findViewById(R.id.reminderDate);
            timeText = itemView.findViewById(R.id.reminderTime);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
