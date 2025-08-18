package com.company.leavemanagementsystem.dto;

import com.company.leavemanagementsystem.entity.LeaveDuration;
import com.company.leavemanagementsystem.entity.LeaveType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * DTO for leave application requests with enhanced validation.
 * Supports emergency leave and backdated request handling.
 */
@Schema(description = "Request object for applying for leave")
public class LeaveApplicationRequest {

    @Schema(description = "Employee ID of the person applying for leave", 
            example = "EMP001", 
            required = true,
            maxLength = 20)
    @NotBlank(message = "Employee ID is required")
    @Size(max = 20, message = "Employee ID cannot exceed 20 characters")
    private String employeeId;

    @Schema(description = "Type of leave being requested", 
            example = "VACATION", 
            required = true,
            allowableValues = {"VACATION", "SICK", "PERSONAL", "EMERGENCY", "MATERNITY", "PATERNITY", "BEREAVEMENT", "COMPENSATORY"})
    @NotNull(message = "Leave type is required")
    private LeaveType leaveType;

    @Schema(description = "Start date of the leave", 
            example = "2024-03-15", 
            required = true,
            format = "date")
    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @Schema(description = "End date of the leave", 
            example = "2024-03-17", 
            required = true,
            format = "date")
    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Schema(description = "Duration of leave per day", 
            example = "FULL_DAY", 
            required = true,
            allowableValues = {"FULL_DAY", "HALF_DAY_MORNING", "HALF_DAY_AFTERNOON"})
    @NotNull(message = "Leave duration is required")
    private LeaveDuration duration = LeaveDuration.FULL_DAY;

    @Schema(description = "Reason for taking leave", 
            example = "Family vacation", 
            required = true,
            maxLength = 500)
    @NotBlank(message = "Reason is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    @Schema(description = "Additional comments or details", 
            example = "Will be available for urgent matters via phone",
            maxLength = 1000)
    @Size(max = 1000, message = "Comments cannot exceed 1000 characters")
    private String comments;

    @Schema(description = "Whether this is an emergency leave request (auto-approved up to 2 days)", 
            example = "false",
            defaultValue = "false")
    private Boolean isEmergencyLeave = false;

    @Schema(description = "Justification for backdated leave request (required if start date is in the past)", 
            example = "Forgot to apply due to medical emergency",
            maxLength = 500)
    @Size(max = 500, message = "Backdated justification cannot exceed 500 characters")
    private String backdatedJustification;

    // Default constructor
    public LeaveApplicationRequest() {}

    // Constructor with required fields
    public LeaveApplicationRequest(String employeeId, LeaveType leaveType, LocalDate startDate, 
                                 LocalDate endDate, LeaveDuration duration, String reason) {
        this.employeeId = employeeId;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.duration = duration;
        this.reason = reason;
    }

    // Constructor with all fields
    public LeaveApplicationRequest(String employeeId, LeaveType leaveType, LocalDate startDate, 
                                 LocalDate endDate, LeaveDuration duration, String reason, 
                                 String comments, Boolean isEmergencyLeave, String backdatedJustification) {
        this.employeeId = employeeId;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.duration = duration;
        this.reason = reason;
        this.comments = comments;
        this.isEmergencyLeave = isEmergencyLeave;
        this.backdatedJustification = backdatedJustification;
    }

    // Getters and Setters
    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
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

    public Boolean getIsEmergencyLeave() {
        return isEmergencyLeave;
    }

    public void setIsEmergencyLeave(Boolean isEmergencyLeave) {
        this.isEmergencyLeave = isEmergencyLeave;
    }

    public String getBackdatedJustification() {
        return backdatedJustification;
    }

    public void setBackdatedJustification(String backdatedJustification) {
        this.backdatedJustification = backdatedJustification;
    }

    // Helper methods for validation
    public boolean isBackdated() {
        return startDate != null && startDate.isBefore(LocalDate.now());
    }

    public boolean isValidDateRange() {
        return startDate != null && endDate != null && !endDate.isBefore(startDate);
    }

    public boolean requiresBackdatedJustification() {
        return isBackdated() && (backdatedJustification == null || backdatedJustification.trim().isEmpty());
    }

    @Override
    public String toString() {
        return "LeaveApplicationRequest{" +
                "employeeId='" + employeeId + '\'' +
                ", leaveType=" + leaveType +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", duration=" + duration +
                ", reason='" + reason + '\'' +
                ", comments='" + comments + '\'' +
                ", isEmergencyLeave=" + isEmergencyLeave +
                ", backdatedJustification='" + backdatedJustification + '\'' +
                '}';
    }
}