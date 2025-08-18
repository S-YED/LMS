package com.company.leavemanagementsystem.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * DTO for updating an existing employee in the system.
 * Contains validation annotations to ensure data integrity.
 * Employee ID cannot be updated once created.
 */
public class UpdateEmployeeRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @Email(message = "Valid email is required")
    @NotBlank(message = "Email is required")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @NotBlank(message = "Department is required")
    @Size(max = 50, message = "Department cannot exceed 50 characters")
    private String department;

    @NotNull(message = "Joining date is required")
    @PastOrPresent(message = "Joining date cannot be in the future")
    private LocalDate joiningDate;

    @Size(max = 20, message = "Manager ID cannot exceed 20 characters")
    private String managerId;

    // Default constructor
    public UpdateEmployeeRequest() {}

    // Constructor with all fields
    public UpdateEmployeeRequest(String name, String email, String department, 
                               LocalDate joiningDate, String managerId) {
        this.name = name;
        this.email = email;
        this.department = department;
        this.joiningDate = joiningDate;
        this.managerId = managerId;
    }

    // Getters and Setters
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

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }

    @Override
    public String toString() {
        return "UpdateEmployeeRequest{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", department='" + department + '\'' +
                ", joiningDate=" + joiningDate +
                ", managerId='" + managerId + '\'' +
                '}';
    }
}