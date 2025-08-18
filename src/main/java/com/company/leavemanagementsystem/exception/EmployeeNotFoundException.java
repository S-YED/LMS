package com.company.leavemanagementsystem.exception;

/**
 * Exception thrown when an employee is not found in the system.
 * This exception is used when operations are attempted on non-existent employees.
 */
public class EmployeeNotFoundException extends RuntimeException {
    
    public EmployeeNotFoundException(String message) {
        super(message);
    }
    
    public EmployeeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Creates an exception for employee not found by employee ID
     */
    public static EmployeeNotFoundException byEmployeeId(String employeeId) {
        return new EmployeeNotFoundException("Employee not found with ID: " + employeeId);
    }
    
    /**
     * Creates an exception for employee not found by database ID
     */
    public static EmployeeNotFoundException byId(Long id) {
        return new EmployeeNotFoundException("Employee not found with ID: " + id);
    }
}