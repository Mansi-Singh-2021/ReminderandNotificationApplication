package com.example.reminderandnotificationapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for managing reminders using SharedPreferences.
 * Provides methods to store, retrieve, and delete reminders.
 */
public class SharedPreferencesHelper {
    private static final String TAG = "SharedPreferencesHelper";
    private static final String PREF_NAME = "ReminderPrefs";
    private static final String REMINDERS_KEY = "reminders_list";

    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    public SharedPreferencesHelper(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new GsonBuilder().create();
    }

    /**
     * Save a reminder to SharedPreferences
     */
    public void saveReminder(Reminder reminder) {
        try {
            List<Reminder> reminders = getAllReminders();
            reminders.add(reminder);
            String json = gson.toJson(reminders);
            sharedPreferences.edit().putString(REMINDERS_KEY, json).apply();
            Log.d(TAG, "Reminder saved: " + reminder.getTitle());
        } catch (Exception e) {
            Log.e(TAG, "Error saving reminder: ", e);
        }
    }

    /**
     * Get all reminders from SharedPreferences
     */
    public List<Reminder> getAllReminders() {
        try {
            String json = sharedPreferences.getString(REMINDERS_KEY, null);

            if (json == null || json.isEmpty() || json.equals("[]")) {
                return new ArrayList<>();
            }

            Type type = new TypeToken<List<Reminder>>() {}.getType();
            List<Reminder> reminders = gson.fromJson(json, type);
            return reminders != null ? reminders : new ArrayList<>();
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Error parsing JSON from SharedPreferences: ", e);
            // Clear corrupted data
            sharedPreferences.edit().remove(REMINDERS_KEY).apply();
            return new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving reminders: ", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get a reminder by ID
     */
    public Reminder getReminderById(String id) {
        List<Reminder> reminders = getAllReminders();
        for (Reminder reminder : reminders) {
            if (reminder.getId().equals(id)) {
                return reminder;
            }
        }
        return null;
    }

    /**
     * Delete a reminder by ID
     */
    public void deleteReminder(String id) {
        try {
            List<Reminder> reminders = getAllReminders();
            reminders.removeIf(reminder -> reminder.getId().equals(id));
            String json = gson.toJson(reminders);
            sharedPreferences.edit().putString(REMINDERS_KEY, json).apply();
            Log.d(TAG, "Reminder deleted: " + id);
        } catch (Exception e) {
            Log.e(TAG, "Error deleting reminder: ", e);
        }
    }

    /**
     * Update a reminder
     */
    public void updateReminder(Reminder reminder) {
        deleteReminder(reminder.getId());
        saveReminder(reminder);
    }

    /**
     * Clear all reminders
     */
    public void clearAllReminders() {
        try {
            sharedPreferences.edit().remove(REMINDERS_KEY).apply();
            Log.d(TAG, "All reminders cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing reminders: ", e);
        }
    }
}


