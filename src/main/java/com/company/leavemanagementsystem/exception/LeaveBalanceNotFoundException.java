package com.company.leavemanagementsystem.exception;

import com.company.leavemanagementsystem.entity.LeaveType;

/**
 * Exception thrown when leave balance is not found for an employee and leave type.
 * This typically occurs when balance records are not properly initialized.
 */
public class LeaveBalanceNotFoundException extends RuntimeException {
    
    private final String employeeId;
    private final LeaveType leaveType;
    private final Integer year;
    
    public LeaveBalanceNotFoundException(String message) {
        super(message);
        this.employeeId = null;
        this.leaveType = null;
        this.year = null;
    }
    
    public LeaveBalanceNotFoundException(String employeeId, LeaveType leaveType, Integer year) {
        super(String.format("Leave balance not found for employee %s, leave type %s, year %d", 
                          employeeId, leaveType.name(), year));
        this.employeeId = employeeId;
        this.leaveType = leaveType;
        this.year = year;
    }
    
    public LeaveBalanceNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.employeeId = null;
        this.leaveType = null;
        this.year = null;
    }
    
    /**
     * Creates an exception for missing leave balance
     */
    public static LeaveBalanceNotFoundException create(String employeeId, LeaveType leaveType, Integer year) {
        return new LeaveBalanceNotFoundException(employeeId, leaveType, year);
    }
    
    /**
     * Creates an exception for uninitialized leave balance
     */
    public static LeaveBalanceNotFoundException uninitialized(String employeeId) {
        return new LeaveBalanceNotFoundException(
            "Leave balance not initialized for employee: " + employeeId + 
            ". Please initialize leave balance before applying for leave.");
    }
    
    public String getEmployeeId() {
        return employeeId;
    }
    
    public LeaveType getLeaveType() {
        return leaveType;
    }
    
    public Integer getYear() {
        return year;
    }
}