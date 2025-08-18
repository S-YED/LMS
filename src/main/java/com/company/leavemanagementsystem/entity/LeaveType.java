package com.company.leavemanagementsystem.entity;

/**
 * Enum representing different types of leave available in the system.
 * Supports various leave categories as per organizational policies.
 */
public enum LeaveType {
    VACATION("Vacation Leave"),
    SICK("Sick Leave"),
    PERSONAL("Personal Leave"),
    EMERGENCY("Emergency Leave"),
    MATERNITY("Maternity Leave"),
    PATERNITY("Paternity Leave"),
    BEREAVEMENT("Bereavement Leave"),
    COMPENSATORY("Compensatory Off"),
    UNPAID("Unpaid Leave");

    private final String displayName;

    LeaveType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}