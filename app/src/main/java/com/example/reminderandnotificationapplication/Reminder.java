package com.example.reminderandnotificationapplication;

import java.io.Serializable;

/**
 * Model class representing a reminder task.
 * Implements Serializable for SharedPreferences storage.
 */
public class Reminder implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String description;
    private long reminderTime; // in milliseconds
    private boolean isActive;

    public Reminder() {
    }

    public Reminder(String id, String title, String description, long reminderTime) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.reminderTime = reminderTime;
        this.isActive = true;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(long reminderTime) {
        this.reminderTime = reminderTime;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public String toString() {
        return "Reminder{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", reminderTime=" + reminderTime +
                ", isActive=" + isActive +
                '}';
    }
}

