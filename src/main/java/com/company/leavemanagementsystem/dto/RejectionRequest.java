package com.company.leavemanagementsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for leave request rejection.
 * Contains approver information and mandatory rejection reason.
 */
public class RejectionRequest {

    @NotBlank(message = "Approver ID is required")
    @Size(max = 20, message = "Approver ID cannot exceed 20 characters")
    private String approverId;

    @NotBlank(message = "Rejection reason is required")
    @Size(max = 500, message = "Rejection reason cannot exceed 500 characters")
    private String rejectionReason;

    @Size(max = 500, message = "Comments cannot exceed 500 characters")
    private String comments;

    // Default constructor
    public RejectionRequest() {}

    // Constructor with required fields
    public RejectionRequest(String approverId, String rejectionReason) {
        this.approverId = approverId;
        this.rejectionReason = rejectionReason;
    }

    // Constructor with all fields
    public RejectionRequest(String approverId, String rejectionReason, String comments) {
        this.approverId = approverId;
        this.rejectionReason = rejectionReason;
        this.comments = comments;
    }

    // Getters and Setters
    public String getApproverId() {
        return approverId;
    }

    public void setApproverId(String approverId) {
        this.approverId = approverId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @Override
    public String toString() {
        return "RejectionRequest{" +
                "approverId='" + approverId + '\'' +
                ", rejectionReason='" + rejectionReason + '\'' +
                ", comments='" + comments + '\'' +
                '}';
    }
}