package com.company.leavemanagementsystem.exception;

/**
 * Exception thrown when an employee attempts to approve or reject a leave request
 * without proper authorization (e.g., not the manager, self-approval attempt).
 */
public class UnauthorizedApprovalException extends RuntimeException {
    
    private final String employeeId;
    private final String requestId;
    private final String attemptedBy;
    
    public UnauthorizedApprovalException(String message) {
        super(message);
        this.employeeId = null;
        this.requestId = null;
        this.attemptedBy = null;
    }
    
    public UnauthorizedApprovalException(String employeeId, String requestId, String attemptedBy) {
        super(String.format("Employee %s is not authorized to approve/reject leave request %s for employee %s", 
                          attemptedBy, requestId, employeeId));
        this.employeeId = employeeId;
        this.requestId = requestId;
        this.attemptedBy = attemptedBy;
    }
    
    public UnauthorizedApprovalException(String message, Throwable cause) {
        super(message, cause);
        this.employeeId = null;
        this.requestId = null;
        this.attemptedBy = null;
    }
    
    /**
     * Creates an exception for self-approval attempt
     */
    public static UnauthorizedApprovalException selfApproval(String employeeId, String requestId) {
        return new UnauthorizedApprovalException(
            String.format("Employee %s cannot approve their own leave request %s", employeeId, requestId));
    }
    
    /**
     * Creates an exception for non-manager approval attempt
     */
    public static UnauthorizedApprovalException notManager(String employeeId, String requestId, String attemptedBy) {
        return new UnauthorizedApprovalException(employeeId, requestId, attemptedBy);
    }
    
    public String getEmployeeId() {
        return employeeId;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public String getAttemptedBy() {
        return attemptedBy;
    }
}