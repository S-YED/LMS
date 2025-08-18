package com.company.leavemanagementsystem.entity;

/**
 * Enum representing the duration of leave for a single day.
 * Supports full day and half day leave calculations.
 */
public enum LeaveDuration {
    FULL_DAY("Full Day", 1.0),
    HALF_DAY("Half Day", 0.5);

    private final String displayName;
    private final double value;

    LeaveDuration(String displayName, double value) {
        this.displayName = displayName;
        this.value = value;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the numeric value for leave calculation
     * @return 1.0 for full day, 0.5 for half day
     */
    public double getValue() {
        return value;
    }
}