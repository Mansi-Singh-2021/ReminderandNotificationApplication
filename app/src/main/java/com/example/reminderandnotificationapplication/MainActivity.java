package com.example.reminderandnotificationapplication;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final long EXACT_ALARM_PROMPT_COOLDOWN_MS = 60_000;

    // UI Components
    private EditText taskTitleInput;
    private TextView selectedDateText;

    // Helper classes
    private SharedPreferencesHelper preferencesHelper;
    private AlarmManager alarmManager;
    private long lastExactAlarmPromptAt;

    // Calendar for date/time selection
    private Calendar selectedDateTime = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    // Reminders adapter
    private ArrayAdapter<String> remindersAdapter;
    private List<String> remindersDisplayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize components
        initializeUI();
        initializeHelpers();
        requestPermissions();
        loadReminders();
    }

    /**
     * Initialize all UI components
     */
    private void initializeUI() {
        taskTitleInput = findViewById(R.id.taskTitleInput);
        Button selectDateButton = findViewById(R.id.selectDateButton);
        Button selectTimeButton = findViewById(R.id.selectTimeButton);
        Button setReminderButton = findViewById(R.id.setReminderButton);
        selectedDateText = findViewById(R.id.selectedDateText);
        ListView remindersList = findViewById(R.id.remindersList);

        // Initialize reminders list adapter
        remindersDisplayList = new ArrayList<>();
        remindersAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, remindersDisplayList);
        remindersList.setAdapter(remindersAdapter);

        // Set click listeners
        selectDateButton.setOnClickListener(v -> showDatePicker());
        selectTimeButton.setOnClickListener(v -> showTimePicker());
        setReminderButton.setOnClickListener(v -> scheduleReminder());

        // Long click to delete reminder
        remindersList.setOnItemLongClickListener((parent, view, position, id) -> {
            deleteReminder(position);
            return true;
        });

        // Update initial date/time display
        updateDateTimeDisplay();
    }

    /**
     * Initialize helper classes
     */
    private void initializeHelpers() {
        preferencesHelper = new SharedPreferencesHelper(this);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * Request necessary permissions for Android 13+
     */
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE
                );
            }
        }

        // Ask for exact alarm special access early so first scheduled reminder can be exact.
        promptForExactAlarmAccess();
    }

    /**
     * Show DatePicker dialog for selecting reminder date
     */
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateTimeDisplay();
                    Log.d(TAG, "Date selected: " + dateFormat.format(selectedDateTime.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    /**
     * Show TimePicker dialog for selecting reminder time
     */
    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDateTime.set(Calendar.MINUTE, minute);
                    selectedDateTime.set(Calendar.SECOND, 0);
                    updateDateTimeDisplay();
                    Log.d(TAG, "Time selected: " + timeFormat.format(selectedDateTime.getTime()));
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    /**
     * Update the displayed date and time text
     */
    private void updateDateTimeDisplay() {
        selectedDateText.setText(String.format(getString(R.string.selected_date), dateFormat.format(selectedDateTime.getTime())));
        TextView selectedTimeText = findViewById(R.id.selectedTimeText);
        selectedTimeText.setText(String.format(getString(R.string.selected_time), timeFormat.format(selectedDateTime.getTime())));
    }

    /**
     * Schedule a reminder using AlarmManager
     */
    private void scheduleReminder() {
        String title = taskTitleInput.getText().toString().trim();
        EditText taskDescriptionInput = findViewById(R.id.taskDescriptionInput);
        String description = taskDescriptionInput.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a task title", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDateTime.getTimeInMillis() <= System.currentTimeMillis()) {
            Toast.makeText(this, "Please select a future date and time", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create unique reminder ID
            String reminderId = String.valueOf(System.currentTimeMillis());

            // Create reminder object
            Reminder reminder = new Reminder(
                    reminderId,
                    title,
                    description.isEmpty() ? title : description,
                    selectedDateTime.getTimeInMillis()
            );

            // Save reminder to SharedPreferences
            preferencesHelper.saveReminder(reminder);

            // Schedule alarm
            scheduleAlarm(reminder);

            // Clear input fields
            clearInputs();

            // Reload reminders list
            loadReminders();

            Toast.makeText(this, getString(R.string.reminder_set_success), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Reminder scheduled successfully: " + title);
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling reminder: ", e);
            Toast.makeText(this, getString(R.string.reminder_error), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Schedule alarm using AlarmManager
     */
    private void scheduleAlarm(Reminder reminder) {
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is not available");
            return;
        }

        Intent intent = new Intent(this, ReminderBroadcastReceiver.class);
        intent.setAction(ReminderBroadcastReceiver.ACTION_REMINDER_ALARM);
        intent.putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.getId());
        intent.putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_TITLE, reminder.getTitle());
        intent.putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_DESCRIPTION, reminder.getDescription());

        // Use stable ID-derived request code to avoid collisions and support cancellation.
        int requestCode = Math.abs(reminder.getId().hashCode());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminder.getReminderTime(),
                            pendingIntent
                    );
                } else {
                    promptForExactAlarmAccess();
                    Toast.makeText(this, "Enable exact alarms for on-time reminders", Toast.LENGTH_SHORT).show();
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminder.getReminderTime(),
                            pendingIntent
                    );
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminder.getReminderTime(),
                        pendingIntent
                );
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        reminder.getReminderTime(),
                        pendingIntent
                );
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        reminder.getReminderTime(),
                        pendingIntent
                );
            }
            Log.d(TAG, "Alarm scheduled for: " + reminder.getTitle());
        } catch (Exception e) {
            Log.e(TAG, "Error setting alarm: ", e);
        }
    }

    /**
     * Load and display all reminders
     */
    private void loadReminders() {
        List<Reminder> reminders = preferencesHelper.getAllReminders();
        remindersDisplayList.clear();

        for (Reminder reminder : reminders) {
            String displayText = reminder.getTitle() + " - " + dateFormat.format(reminder.getReminderTime());
            remindersDisplayList.add(displayText);
        }

        remindersAdapter.notifyDataSetChanged();
        Log.d(TAG, "Reminders loaded: " + reminders.size());
    }

    /**
     * Delete a reminder
     */
    private void deleteReminder(int position) {
        List<Reminder> reminders = preferencesHelper.getAllReminders();
        if (position >= 0 && position < reminders.size()) {
            Reminder reminder = reminders.get(position);

            // Cancel the alarm
            cancelAlarm(reminder);

            // Delete from preferences
            preferencesHelper.deleteReminder(reminder.getId());

            // Reload list
            loadReminders();

            Toast.makeText(this, "Reminder deleted", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Reminder deleted: " + reminder.getTitle());
        }
    }

    /**
     * Cancel a scheduled alarm
     */
    private void cancelAlarm(Reminder reminder) {
        Intent intent = new Intent(this, ReminderBroadcastReceiver.class);
        intent.setAction(ReminderBroadcastReceiver.ACTION_REMINDER_ALARM);
        int requestCode = Math.abs(reminder.getId().hashCode());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Alarm cancelled for: " + reminder.getTitle());
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling alarm: ", e);
        }
    }

    /**
     * Opens exact alarm settings on Android 12+ if app does not currently have access.
     */
    private void promptForExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager == null) {
            return;
        }
        if (alarmManager.canScheduleExactAlarms()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastExactAlarmPromptAt < EXACT_ALARM_PROMPT_COOLDOWN_MS) {
            return;
        }
        lastExactAlarmPromptAt = now;

        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "Unable to open exact alarm settings", e);
        }
    }

    /**
     * Clear all input fields and reset to current date/time
     */
    private void clearInputs() {
        taskTitleInput.setText("");
        EditText taskDescriptionInput = findViewById(R.id.taskDescriptionInput);
        taskDescriptionInput.setText("");
        selectedDateTime = Calendar.getInstance();
        updateDateTimeDisplay();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permissions required for reminder functionality", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}

