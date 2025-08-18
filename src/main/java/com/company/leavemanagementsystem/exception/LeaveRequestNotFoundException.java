package com.company.leavemanagementsystem.exception;

/**
 * Exception thrown when a leave request is not found in the system.
 * This exception is used when operations are attempted on non-existent leave requests.
 */
public class LeaveRequestNotFoundException extends RuntimeException {
    
    private final String requestId;
    
    public LeaveRequestNotFoundException(String message) {
        super(message);
        this.requestId = null;
    }
    
    public LeaveRequestNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.requestId = null;
    }
    
    /**
     * Creates an exception for leave request not found by request ID
     */
    public static LeaveRequestNotFoundException byRequestId(String requestId) {
        LeaveRequestNotFoundException exception = new LeaveRequestNotFoundException("Leave request not found with ID: " + requestId);
        return exception;
    }
    
    /**
     * Creates an exception for leave request not found by database ID
     */
    public static LeaveRequestNotFoundException byId(Long id) {
        return new LeaveRequestNotFoundException("Leave request not found with ID: " + id);
    }
    
    public String getRequestId() {
        return requestId;
    }
}