package com.company.leavemanagementsystem.exception;

/**
 * Exception thrown when attempting to create an employee with a duplicate employee ID or email.
 */
public class DuplicateEmployeeException extends RuntimeException {
    
    private final String employeeId;
    private final String email;
    private final String duplicateField;
    
    public DuplicateEmployeeException(String message) {
        super(message);
        this.employeeId = null;
        this.email = null;
        this.duplicateField = null;
    }
    
    public DuplicateEmployeeException(String duplicateField, String value) {
        super(String.format("Employee with %s '%s' already exists", duplicateField, value));
        this.duplicateField = duplicateField;
        if ("employeeId".equals(duplicateField)) {
            this.employeeId = value;
            this.email = null;
        } else if ("email".equals(duplicateField)) {
            this.email = value;
            this.employeeId = null;
        } else {
            this.employeeId = null;
            this.email = null;
        }
    }
    
    public DuplicateEmployeeException(String message, Throwable cause) {
        super(message, cause);
        this.employeeId = null;
        this.email = null;
        this.duplicateField = null;
    }
    
    /**
     * Creates an exception for duplicate employee ID
     */
    public static DuplicateEmployeeException byEmployeeId(String employeeId) {
        return new DuplicateEmployeeException("employeeId", employeeId);
    }
    
    /**
     * Creates an exception for duplicate email
     */
    public static DuplicateEmployeeException byEmail(String email) {
        return new DuplicateEmployeeException("email", email);
    }
    
    public String getEmployeeId() {
        return employeeId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getDuplicateField() {
        return duplicateField;
    }
}