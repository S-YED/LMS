package com.company.leavemanagementsystem.entity;

/**
 * Enum representing the status of a leave request throughout its lifecycle.
 * Includes AUTO_APPROVED status for emergency leave scenarios.
 */
public enum LeaveStatus {
    PENDING("Pending Approval"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    AUTO_APPROVED("Auto Approved"),
    CANCELLED("Cancelled"),
    WITHDRAWN("Withdrawn");

    private final String displayName;

    LeaveStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if the status represents an approved state
     */
    public boolean isApproved() {
        return this == APPROVED || this == AUTO_APPROVED;
    }

    /**
     * Check if the status allows modification
     */
    public boolean isModifiable() {
        return this == PENDING;
    }
}