package com.company.leavemanagementsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for employee response in API calls.
 * Contains all employee information for client consumption.
 */
@Schema(description = "Employee information response")
public class EmployeeResponse {

    @Schema(description = "Database ID of the employee", example = "1")
    private Long id;
    
    @Schema(description = "Unique employee identifier", example = "EMP001")
    private String employeeId;
    
    @Schema(description = "Full name of the employee", example = "John Doe")
    private String name;
    
    @Schema(description = "Email address of the employee", example = "john.doe@company.com")
    private String email;
    
    @Schema(description = "Department where the employee works", example = "Engineering")
    private String department;
    
    @Schema(description = "Date when the employee joined the company", example = "2023-01-15")
    private LocalDate joiningDate;
    
    @Schema(description = "Manager information (if assigned)")
    private ManagerInfo manager;
    
    @Schema(description = "Timestamp when the employee record was created", example = "2023-01-15T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "Timestamp when the employee record was last updated", example = "2023-01-15T10:30:00")
    private LocalDateTime updatedAt;

    // Default constructor
    public EmployeeResponse() {}

    // Constructor with all fields
    public EmployeeResponse(Long id, String employeeId, String name, String email, String department,
                          LocalDate joiningDate, ManagerInfo manager, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.name = name;
        this.email = email;
        this.department = department;
        this.joiningDate = joiningDate;
        this.manager = manager;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public ManagerInfo getManager() {
        return manager;
    }

    public void setManager(ManagerInfo manager) {
        this.manager = manager;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Nested class for manager information to avoid circular references
     */
    @Schema(description = "Manager information")
    public static class ManagerInfo {
        @Schema(description = "Manager's employee ID", example = "MGR001")
        private String employeeId;
        
        @Schema(description = "Manager's full name", example = "Jane Smith")
        private String name;
        
        @Schema(description = "Manager's email address", example = "jane.smith@company.com")
        private String email;
        
        @Schema(description = "Manager's department", example = "Engineering")
        private String department;

        public ManagerInfo() {}

        public ManagerInfo(String employeeId, String name, String email, String department) {
            this.employeeId = employeeId;
            this.name = name;
            this.email = email;
            this.department = department;
        }

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

        @Override
        public String toString() {
            return "ManagerInfo{" +
                    "employeeId='" + employeeId + '\'' +
                    ", name='" + name + '\'' +
                    ", email='" + email + '\'' +
                    ", department='" + department + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "EmployeeResponse{" +
                "id=" + id +
                ", employeeId='" + employeeId + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", department='" + department + '\'' +
                ", joiningDate=" + joiningDate +
                ", manager=" + manager +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}