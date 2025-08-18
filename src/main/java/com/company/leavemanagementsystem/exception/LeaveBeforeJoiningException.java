package com.company.leavemanagementsystem.exception;

import java.time.LocalDate;

/**
 * Exception thrown when an employee attempts to apply for leave
 * before their joining date.
 */
public class LeaveBeforeJoiningException extends RuntimeException {
    
    private final String employeeId;
    private final LocalDate joiningDate;
    private final LocalDate leaveStartDate;
    
    public LeaveBeforeJoiningException(String message) {
        super(message);
        this.employeeId = null;
        this.joiningDate = null;
        this.leaveStartDate = null;
    }
    
    public LeaveBeforeJoiningException(String employeeId, LocalDate joiningDate, LocalDate leaveStartDate) {
        super(String.format("Cannot apply for leave before joining date. Employee %s joined on %s, " +
                          "but leave start date is %s", employeeId, joiningDate, leaveStartDate));
        this.employeeId = employeeId;
        this.joiningDate = joiningDate;
        this.leaveStartDate = leaveStartDate;
    }
    
    public LeaveBeforeJoiningException(String message, Throwable cause) {
        super(message, cause);
        this.employeeId = null;
        this.joiningDate = null;
        this.leaveStartDate = null;
    }
    
    /**
     * Creates an exception for leave before joining date
     */
    public static LeaveBeforeJoiningException create(String employeeId, LocalDate joiningDate, 
                                                   LocalDate leaveStartDate) {
        return new LeaveBeforeJoiningException(employeeId, joiningDate, leaveStartDate);
    }
    
    public String getEmployeeId() {
        return employeeId;
    }
    
    public LocalDate getJoiningDate() {
        return joiningDate;
    }
    
    public LocalDate getLeaveStartDate() {
        return leaveStartDate;
    }
}