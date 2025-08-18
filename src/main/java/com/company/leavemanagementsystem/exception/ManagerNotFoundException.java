package com.company.leavemanagementsystem.exception;

/**
 * Exception thrown when a manager is not found or when manager relationships are invalid.
 * This includes cases of circular manager relationships or missing manager assignments.
 */
public class ManagerNotFoundException extends RuntimeException {
    
    private final String managerId;
    private final String employeeId;
    
    public ManagerNotFoundException(String message) {
        super(message);
        this.managerId = null;
        this.employeeId = null;
    }
    
    public ManagerNotFoundException(String managerId, String employeeId) {
        super(String.format("Manager with ID %s not found for employee %s", managerId, employeeId));
        this.managerId = managerId;
        this.employeeId = employeeId;
    }
    
    public ManagerNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.managerId = null;
        this.employeeId = null;
    }
    
    /**
     * Creates an exception for manager not found
     */
    public static ManagerNotFoundException create(String managerId, String employeeId) {
        return new ManagerNotFoundException(managerId, employeeId);
    }
    
    /**
     * Creates an exception for circular manager relationship
     */
    public static ManagerNotFoundException circularRelationship(String employeeId, String managerId) {
        return new ManagerNotFoundException(
            String.format("Circular manager relationship detected: employee %s cannot be manager of %s", 
                         employeeId, managerId));
    }
    
    /**
     * Creates an exception for missing manager assignment
     */
    public static ManagerNotFoundException noManagerAssigned(String employeeId) {
        return new ManagerNotFoundException(
            "No manager assigned for employee: " + employeeId + 
            ". Cannot process leave request without manager assignment.");
    }
    
    public String getManagerId() {
        return managerId;
    }
    
    public String getEmployeeId() {
        return employeeId;
    }
}