package com.company.leavemanagementsystem.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a leave request in the system.
 * Includes support for emergency leave, backdated requests, and comprehensive
 * tracking.
 */
@Entity
@Table(name = "leave_requests", indexes = {
        @Index(name = "idx_leave_employee_date", columnList = "employee_id, start_date, end_date"),
        @Index(name = "idx_leave_status_date", columnList = "status, created_at"),
        @Index(name = "idx_leave_request_id", columnList = "request_id"),
        @Index(name = "idx_leave_approved_by", columnList = "approved_by")
})
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", unique = true, nullable = false, length = 50)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 20)
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LeaveDuration duration = LeaveDuration.FULL_DAY;

    @Column(name = "total_days", nullable = false)
    private Double totalDays;

    @Column(length = 500)
    private String reason;

    @Column(length = 1000)
    private String comments;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveStatus status = LeaveStatus.PENDING;

    @Column(name = "is_emergency_leave", nullable = false)
    private Boolean isEmergencyLeave = false;

    @Column(name = "is_backdated", nullable = false)
    private Boolean isBackdated = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Employee approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Default constructor
    public LeaveRequest() {
        this.requestId = generateRequestId();
    }

    // Constructor with required fields
    public LeaveRequest(Employee employee, LeaveType leaveType, LocalDate startDate,
            LocalDate endDate, LeaveDuration duration, String reason) {
        this();
        this.employee = employee;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.duration = duration;
        this.reason = reason;
        this.totalDays = calculateTotalDays();
        this.isBackdated = isBackdatedRequest();
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

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
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
        this.totalDays = calculateTotalDays();
        this.isBackdated = isBackdatedRequest();
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        this.totalDays = calculateTotalDays();
    }

    public LeaveDuration getDuration() {
        return duration;
    }

    public void setDuration(LeaveDuration duration) {
        this.duration = duration;
        this.totalDays = calculateTotalDays();
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

    public Employee getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Employee approvedBy) {
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

    // Business logic methods

    /**
     * Generate a unique request ID
     */
    private String generateRequestId() {
        return "LR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Calculate total working days for the leave request
     * Excludes weekends (Saturday and Sunday)
     */
    private Double calculateTotalDays() {
        if (startDate == null || endDate == null || duration == null) {
            return 0.0;
        }

        // Calculate total calendar days (not used in working days calculation but kept
        // for reference)
        // long totalCalendarDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        long workingDays = 0;

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            // Skip weekends (Saturday = 6, Sunday = 7)
            if (current.getDayOfWeek().getValue() < 6) {
                workingDays++;
            }
            current = current.plusDays(1);
        }

        return workingDays * duration.getValue();
    }

    /**
     * Check if this is a same-day application
     */
    public boolean isSameDayApplication() {
        return startDate != null && startDate.equals(LocalDate.now());
    }

    /**
     * Check if this is a backdated request
     */
    public boolean isBackdatedRequest() {
        return startDate != null && startDate.isBefore(LocalDate.now());
    }

    /**
     * Check if the request can be auto-approved (emergency leave <= 2 days)
     */
    public boolean canBeAutoApproved() {
        return isEmergencyLeave && totalDays != null && totalDays <= 2.0;
    }

    /**
     * Approve the leave request
     */
    public void approve(Employee approver) {
        this.status = canBeAutoApproved() ? LeaveStatus.AUTO_APPROVED : LeaveStatus.APPROVED;
        this.approvedBy = approver;
        this.approvedAt = LocalDateTime.now();
        this.rejectionReason = null;
    }

    /**
     * Reject the leave request
     */
    public void reject(Employee approver, String reason) {
        this.status = LeaveStatus.REJECTED;
        this.approvedBy = approver;
        this.approvedAt = LocalDateTime.now();
        this.rejectionReason = reason;
    }

    /**
     * Cancel the leave request
     */
    public void cancel() {
        if (status.isModifiable()) {
            this.status = LeaveStatus.CANCELLED;
        } else {
            throw new IllegalStateException("Cannot cancel leave request in status: " + status);
        }
    }

    /**
     * Check if dates overlap with another leave request
     */
    public boolean overlapsWith(LeaveRequest other) {
        if (other == null || other.startDate == null || other.endDate == null) {
            return false;
        }
        return !(this.endDate.isBefore(other.startDate) || this.startDate.isAfter(other.endDate));
    }

    /**
     * Check if the request is in a final state
     */
    public boolean isFinalState() {
        return status == LeaveStatus.APPROVED ||
                status == LeaveStatus.AUTO_APPROVED ||
                status == LeaveStatus.REJECTED ||
                status == LeaveStatus.CANCELLED;
    }

    /**
     * Get the number of days until leave starts
     */
    public long getDaysUntilStart() {
        return ChronoUnit.DAYS.between(LocalDate.now(), startDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LeaveRequest that = (LeaveRequest) o;
        return Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId);
    }

    @Override
    public String toString() {
        return "LeaveRequest{" +
                "id=" + id +
                ", requestId='" + requestId + '\'' +
                ", employeeId=" + (employee != null ? employee.getEmployeeId() : null) +
                ", leaveType=" + leaveType +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", duration=" + duration +
                ", totalDays=" + totalDays +
                ", status=" + status +
                ", isEmergencyLeave=" + isEmergencyLeave +
                ", isBackdated=" + isBackdated +
                '}';
    }
}