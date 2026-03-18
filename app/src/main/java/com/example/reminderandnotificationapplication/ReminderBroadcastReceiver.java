package com.example.reminderandnotificationapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * BroadcastReceiver that listens for alarm triggers from AlarmManager.
 * When an alarm is triggered, it displays a notification with the reminder details.
 */
public class ReminderBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderBroadcastReceiver";
    public static final String ACTION_REMINDER_ALARM = "com.example.reminderandnotificationapplication.REMINDER_ALARM";
    public static final String EXTRA_REMINDER_ID = "reminder_id";
    public static final String EXTRA_REMINDER_TITLE = "reminder_title";
    public static final String EXTRA_REMINDER_DESCRIPTION = "reminder_description";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (intent != null && ACTION_REMINDER_ALARM.equals(intent.getAction())) {
                String reminderId = intent.getStringExtra(EXTRA_REMINDER_ID);
                String title = intent.getStringExtra(EXTRA_REMINDER_TITLE);
                String description = intent.getStringExtra(EXTRA_REMINDER_DESCRIPTION);

                Log.d(TAG, "Reminder alarm triggered for: " + title);

                // Display the notification
                NotificationHelper notificationHelper = new NotificationHelper(context);
                notificationHelper.showNotification(title, description, reminderId);

                // Log the event (optional: can be extended to update reminder status)
                Log.d(TAG, "Notification displayed for reminder ID: " + reminderId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling alarm broadcast: ", e);
        }
    }
}

