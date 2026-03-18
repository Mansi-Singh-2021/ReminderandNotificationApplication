package com.example.reminderandnotificationapplication;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

/**
 * Helper class for managing notifications.
 * Creates notification channels (Android 8+) and displays notifications.
 */
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "reminder_notifications";

    private final Context context;
    private final NotificationManager notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    /**
     * Create notification channel for Android 8 and above.
     * Required for posting notifications on Android 8+.
     */
    private void createNotificationChannel() {
        CharSequence name = context.getString(R.string.notification_channel_name);
        String description = context.getString(R.string.notification_channel_description);
        int importance = NotificationManager.IMPORTANCE_HIGH;

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        channel.enableVibration(true);
        channel.setShowBadge(true);

        notificationManager.createNotificationChannel(channel);
        Log.d(TAG, "Notification channel created");
    }

    /**
     * Display a notification for a reminder.
     * Creates a pending intent to launch MainActivity when notification is tapped.
     * Checks for POST_NOTIFICATIONS permission on Android 13+.
     */
    @SuppressLint("MissingPermission")
    public void showNotification(String title, String message, String reminderId) {
        try {
            String safeReminderId = reminderId != null ? reminderId : String.valueOf(System.currentTimeMillis());
            String safeTitle = title != null ? title : context.getString(R.string.app_name);
            String safeMessage = message != null ? message : safeTitle;

            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("reminder_id", safeReminderId);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    Math.abs(safeReminderId.hashCode()),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(safeTitle)
                    .setContentText(safeMessage)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(safeMessage));

            int notificationId = Math.abs(safeReminderId.hashCode());

            // Check permission for Android 13 and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                        == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notifyWithPermission(builder, notificationId);
                    Log.d(TAG, "Notification displayed: " + safeTitle);
                } else {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted");
                }
            } else {
                // Pre-Android 13: no permission required for notifications
                notifyPreAndroid13(builder, notificationId, safeTitle);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification: ", e);
        }
    }

    /**
     * Helper method to post notification with proper permission handling
     * Using @SuppressLint since we already checked permissions before calling this
     */
    @SuppressLint("MissingPermission")
    private void notifyWithPermission(NotificationCompat.Builder builder, int notificationId) {
        notificationManager.notify(notificationId, builder.build());
    }

    /**
     * Helper method to post notification on pre-Android 13 devices
     * No permission required for notifications on Android < 13
     */
    @SuppressLint("MissingPermission")
    private void notifyPreAndroid13(NotificationCompat.Builder builder, int notificationId, String title) {
        notificationManager.notify(notificationId, builder.build());
        Log.d(TAG, "Notification displayed: " + title);
    }

}






