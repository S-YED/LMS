package com.company.leavemanagementsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * DTO for creating a new employee in the system.
 * Contains validation annotations to ensure data integrity.
 */
@Schema(description = "Request object for creating a new employee")
public class CreateEmployeeRequest {

    @Schema(description = "Unique employee identifier", 
            example = "EMP001", 
            required = true,
            pattern = "^[A-Z0-9]+$",
            maxLength = 20)
    @NotBlank(message = "Employee ID is required")
    @Size(max = 20, message = "Employee ID cannot exceed 20 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Employee ID must contain only uppercase letters and numbers")
    private String employeeId;

    @Schema(description = "Full name of the employee", 
            example = "John Doe", 
            required = true,
            maxLength = 100)
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @Schema(description = "Email address of the employee", 
            example = "john.doe@company.com", 
            required = true,
            format = "email",
            maxLength = 100)
    @Email(message = "Valid email is required")
    @NotBlank(message = "Email is required")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @Schema(description = "Department where the employee works", 
            example = "Engineering", 
            required = true,
            maxLength = 50)
    @NotBlank(message = "Department is required")
    @Size(max = 50, message = "Department cannot exceed 50 characters")
    private String department;

    @Schema(description = "Date when the employee joined the company", 
            example = "2023-01-15", 
            required = true,
            format = "date")
    @NotNull(message = "Joining date is required")
    @PastOrPresent(message = "Joining date cannot be in the future")
    private LocalDate joiningDate;

    @Schema(description = "Job position/title of the employee", 
            example = "Software Engineer", 
            required = false,
            maxLength = 100)
    @Size(max = 100, message = "Position cannot exceed 100 characters")
    private String position;

    @Schema(description = "Employee ID of the direct manager (optional)", 
            example = "MGR001",
            maxLength = 20)
    @Size(max = 20, message = "Manager ID cannot exceed 20 characters")
    private String managerId;

    // Default constructor
    public CreateEmployeeRequest() {}

    // Constructor with all fields
    public CreateEmployeeRequest(String employeeId, String name, String email, String department, 
                               String position, LocalDate joiningDate, String managerId) {
        this.employeeId = employeeId;
        this.name = name;
        this.email = email;
        this.department = department;
        this.position = position;
        this.joiningDate = joiningDate;
        this.managerId = managerId;
    }

    // Getters and Setters
    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public String getManagerId() {
        return managerId;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }

    @Override
    public String toString() {
        return "CreateEmployeeRequest{" +
                "employeeId='" + employeeId + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", department='" + department + '\'' +
                ", position='" + position + '\'' +
                ", joiningDate=" + joiningDate +
                ", managerId='" + managerId + '\'' +
                '}';
    }
}