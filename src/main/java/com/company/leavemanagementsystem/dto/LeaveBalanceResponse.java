package com.company.leavemanagementsystem.dto;

import com.company.leavemanagementsystem.entity.LeaveType;
import java.time.LocalDateTime;

/**
 * DTO for leave balance response with fractional day support.
 * Contains detailed balance information for a specific leave type and year.
 */
public class LeaveBalanceResponse {

    private Long id;
    private EmployeeInfo employee;
    private LeaveType leaveType;
    private Double totalDays;
    private Double usedDays;
    private Double availableDays;
    private Integer year;
    private Double utilizationPercentage;
    private Boolean isRunningLow;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Default constructor
    public LeaveBalanceResponse() {}

    // Constructor with all fields
    public LeaveBalanceResponse(Long id, EmployeeInfo employee, LeaveType leaveType, Double totalDays,
                              Double usedDays, Double availableDays, Integer year, Double utilizationPercentage,
                              Boolean isRunningLow, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.employee = employee;
        this.leaveType = leaveType;
        this.totalDays = totalDays;
        this.usedDays = usedDays;
        this.availableDays = availableDays;
        this.year = year;
        this.utilizationPercentage = utilizationPercentage;
        this.isRunningLow = isRunningLow;
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

    public EmployeeInfo getEmployee() {
        return employee;
    }

    public void setEmployee(EmployeeInfo employee) {
        this.employee = employee;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public Double getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(Double totalDays) {
        this.totalDays = totalDays;
    }

    public Double getUsedDays() {
        return usedDays;
    }

    public void setUsedDays(Double usedDays) {
        this.usedDays = usedDays;
    }

    public Double getAvailableDays() {
        return availableDays;
    }

    public void setAvailableDays(Double availableDays) {
        this.availableDays = availableDays;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Double getUtilizationPercentage() {
        return utilizationPercentage;
    }

    public void setUtilizationPercentage(Double utilizationPercentage) {
        this.utilizationPercentage = utilizationPercentage;
    }

    public Boolean getIsRunningLow() {
        return isRunningLow;
    }

    public void setIsRunningLow(Boolean isRunningLow) {
        this.isRunningLow = isRunningLow;
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

    // Convenience methods for backward compatibility with tests
    public void setEmployeeId(String employeeId) {
        if (this.employee == null) {
            this.employee = new EmployeeInfo();
        }
        this.employee.setEmployeeId(employeeId);
    }

    public void setEmployeeName(String employeeName) {
        if (this.employee == null) {
            this.employee = new EmployeeInfo();
        }
        this.employee.setName(employeeName);
    }

    public String getEmployeeId() {
        return this.employee != null ? this.employee.getEmployeeId() : null;
    }

    public String getEmployeeName() {
        return this.employee != null ? this.employee.getName() : null;
    }

    /**
     * Nested class for employee information
     */
    public static class EmployeeInfo {
        private String employeeId;
        private String name;
        private String department;

        public EmployeeInfo() {}

        public EmployeeInfo(String employeeId, String name, String department) {
            this.employeeId = employeeId;
            this.name = name;
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

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }

        @Override
        public String toString() {
            return "EmployeeInfo{" +
                    "employeeId='" + employeeId + '\'' +
                    ", name='" + name + '\'' +
                    ", department='" + department + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "LeaveBalanceResponse{" +
                "id=" + id +
                ", employee=" + employee +
                ", leaveType=" + leaveType +
                ", totalDays=" + totalDays +
                ", usedDays=" + usedDays +
                ", availableDays=" + availableDays +
                ", year=" + year +
                ", utilizationPercentage=" + utilizationPercentage +
                ", isRunningLow=" + isRunningLow +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}