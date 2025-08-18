package com.company.leavemanagementsystem.exception;

import java.time.LocalDate;

/**
 * Exception thrown when a leave request conflicts with existing approved leave requests.
 * This includes overlapping dates or other scheduling conflicts.
 */
public class LeaveRequestConflictException extends RuntimeException {
    
    private final String employeeId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String conflictingRequestId;
    
    public LeaveRequestConflictException(String message) {
        super(message);
        this.employeeId = null;
        this.startDate = null;
        this.endDate = null;
        this.conflictingRequestId = null;
    }
    
    public LeaveRequestConflictException(String employeeId, LocalDate startDate, LocalDate endDate, 
                                       String conflictingRequestId) {
        super(String.format("Leave request conflicts with existing approved leave for employee %s. " +
                          "Requested dates: %s to %s conflict with request ID: %s", 
                          employeeId, startDate, endDate, conflictingRequestId));
        this.employeeId = employeeId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.conflictingRequestId = conflictingRequestId;
    }
    
    public LeaveRequestConflictException(String message, Throwable cause) {
        super(message, cause);
        this.employeeId = null;
        this.startDate = null;
        this.endDate = null;
        this.conflictingRequestId = null;
    }
    
    /**
     * Creates an exception for overlapping leave requests
     */
    public static LeaveRequestConflictException overlappingLeave(String employeeId, LocalDate startDate, 
                                                               LocalDate endDate, String conflictingRequestId) {
        return new LeaveRequestConflictException(employeeId, startDate, endDate, conflictingRequestId);
    }
    
    public String getEmployeeId() {
        return employeeId;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public String getConflictingRequestId() {
        return conflictingRequestId;
    }
}