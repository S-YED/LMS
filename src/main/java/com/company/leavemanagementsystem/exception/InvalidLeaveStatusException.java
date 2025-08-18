package com.company.leavemanagementsystem.exception;

import com.company.leavemanagementsystem.entity.LeaveStatus;

/**
 * Exception thrown when operations are attempted on leave requests
 * with invalid status transitions or inappropriate status.
 */
public class InvalidLeaveStatusException extends RuntimeException {
    
    private final String requestId;
    private final LeaveStatus currentStatus;
    private final LeaveStatus attemptedStatus;
    
    public InvalidLeaveStatusException(String message) {
        super(message);
        this.requestId = null;
        this.currentStatus = null;
        this.attemptedStatus = null;
    }
    
    public InvalidLeaveStatusException(String requestId, LeaveStatus currentStatus, LeaveStatus attemptedStatus) {
        super(String.format("Invalid status transition for leave request %s: cannot change from %s to %s", 
                          requestId, currentStatus.name(), attemptedStatus.name()));
        this.requestId = requestId;
        this.currentStatus = currentStatus;
        this.attemptedStatus = attemptedStatus;
    }
    
    public InvalidLeaveStatusException(String message, Throwable cause) {
        super(message, cause);
        this.requestId = null;
        this.currentStatus = null;
        this.attemptedStatus = null;
    }
    
    /**
     * Creates an exception for already processed leave request
     */
    public static InvalidLeaveStatusException alreadyProcessed(String requestId, LeaveStatus currentStatus) {
        return new InvalidLeaveStatusException(
            String.format("Leave request %s has already been processed with status: %s", 
                         requestId, currentStatus.name()));
    }
    
    /**
     * Creates an exception for invalid status transition
     */
    public static InvalidLeaveStatusException invalidTransition(String requestId, LeaveStatus currentStatus, 
                                                              LeaveStatus attemptedStatus) {
        return new InvalidLeaveStatusException(requestId, currentStatus, attemptedStatus);
    }
    
    /**
     * Creates an exception for operations not allowed on cancelled requests
     */
    public static InvalidLeaveStatusException operationOnCancelledRequest(String requestId) {
        return new InvalidLeaveStatusException(
            "Cannot perform operation on cancelled leave request: " + requestId);
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public LeaveStatus getCurrentStatus() {
        return currentStatus;
    }
    
    public LeaveStatus getAttemptedStatus() {
        return attemptedStatus;
    }
}