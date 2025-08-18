package com.company.leavemanagementsystem.dto;

import com.company.leavemanagementsystem.entity.LeaveDuration;
import com.company.leavemanagementsystem.entity.LeaveStatus;
import com.company.leavemanagementsystem.entity.LeaveType;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for leave request responses in API calls.
 * Contains all leave request information for client consumption.
 */
public class LeaveRequestResponse {

    private Long id;
    private String requestId;
    private EmployeeInfo employee;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private LeaveDuration duration;
    private Double totalDays;
    private String reason;
    private String comments;
    private LeaveStatus status;
    private Boolean isEmergencyLeave;
    private Boolean isBackdated;
    private String backdatedJustification;
    private ApproverInfo approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Default constructor
    public LeaveRequestResponse() {}

    // Constructor with all fields
    public LeaveRequestResponse(Long id, String requestId, EmployeeInfo employee, LeaveType leaveType,
                              LocalDate startDate, LocalDate endDate, LeaveDuration duration, Double totalDays,
                              String reason, String comments, LeaveStatus status, Boolean isEmergencyLeave,
                              Boolean isBackdated, String backdatedJustification, ApproverInfo approvedBy,
                              LocalDateTime approvedAt, String rejectionReason, LocalDateTime createdAt,
                              LocalDateTime updatedAt) {
        this.id = id;
        this.requestId = requestId;
        this.employee = employee;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.duration = duration;
        this.totalDays = totalDays;
        this.reason = reason;
        this.comments = comments;
        this.status = status;
        this.isEmergencyLeave = isEmergencyLeave;
        this.isBackdated = isBackdated;
        this.backdatedJustification = backdatedJustification;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
        this.rejectionReason = rejectionReason;
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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LeaveDuration getDuration() {
        return duration;
    }

    public void setDuration(LeaveDuration duration) {
        this.duration = duration;
    }

    public Double getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(Double totalDays) {
        this.totalDays = totalDays;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveStatus status) {
        this.status = status;
    }

    public Boolean getIsEmergencyLeave() {
        return isEmergencyLeave;
    }

    public void setIsEmergencyLeave(Boolean isEmergencyLeave) {
        this.isEmergencyLeave = isEmergencyLeave;
    }

    public Boolean getIsBackdated() {
        return isBackdated;
    }

    public void setIsBackdated(Boolean isBackdated) {
        this.isBackdated = isBackdated;
    }

    public String getBackdatedJustification() {
        return backdatedJustification;
    }

    public void setBackdatedJustification(String backdatedJustification) {
        this.backdatedJustification = backdatedJustification;
    }

    public ApproverInfo getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(ApproverInfo approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
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
     * Nested class for employee information
     */
    public static class EmployeeInfo {
        private String employeeId;
        private String name;
        private String email;
        private String department;

        public EmployeeInfo() {}

        public EmployeeInfo(String employeeId, String name, String email, String department) {
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
            return "EmployeeInfo{" +
                    "employeeId='" + employeeId + '\'' +
                    ", name='" + name + '\'' +
                    ", email='" + email + '\'' +
                    ", department='" + department + '\'' +
                    '}';
        }
    }

    /**
     * Nested class for approver information
     */
    public static class ApproverInfo {
        private String employeeId;
        private String name;
        private String email;

        public ApproverInfo() {}

        public ApproverInfo(String employeeId, String name, String email) {
            this.employeeId = employeeId;
            this.name = name;
            this.email = email;
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

        @Override
        public String toString() {
            return "ApproverInfo{" +
                    "employeeId='" + employeeId + '\'' +
                    ", name='" + name + '\'' +
                    ", email='" + email + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "LeaveRequestResponse{" +
                "id=" + id +
                ", requestId='" + requestId + '\'' +
                ", employee=" + employee +
                ", leaveType=" + leaveType +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", duration=" + duration +
                ", totalDays=" + totalDays +
                ", status=" + status +
                ", isEmergencyLeave=" + isEmergencyLeave +
                ", isBackdated=" + isBackdated +
                ", approvedBy=" + approvedBy +
                ", approvedAt=" + approvedAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}