package com.company.leavemanagementsystem.exception;

import com.company.leavemanagementsystem.entity.LeaveType;

/**
 * Exception thrown when an employee attempts to apply for leave
 * but has insufficient leave balance for the requested leave type.
 */
public class InsufficientLeaveBalanceException extends RuntimeException {
    
    private final String employeeId;
    private final LeaveType leaveType;
    private final Double requestedDays;
    private final Double availableDays;
    
    public InsufficientLeaveBalanceException(String message) {
        super(message);
        this.employeeId = null;
        this.leaveType = null;
        this.requestedDays = null;
        this.availableDays = null;
    }
    
    public InsufficientLeaveBalanceException(String employeeId, LeaveType leaveType, 
                                           Double requestedDays, Double availableDays) {
        super(String.format("Insufficient %s leave balance for employee %s. Requested: %.1f days, Available: %.1f days", 
                          leaveType.name(), employeeId, requestedDays, availableDays));
        this.employeeId = employeeId;
        this.leaveType = leaveType;
        this.requestedDays = requestedDays;
        this.availableDays = availableDays;
    }
    
    public InsufficientLeaveBalanceException(String message, Throwable cause) {
        super(message, cause);
        this.employeeId = null;
        this.leaveType = null;
        this.requestedDays = null;
        this.availableDays = null;
    }
    
    public String getEmployeeId() {
        return employeeId;
    }
    
    public LeaveType getLeaveType() {
        return leaveType;
    }
    
    public Double getRequestedDays() {
        return requestedDays;
    }
    
    public Double getAvailableDays() {
        return availableDays;
    }
}