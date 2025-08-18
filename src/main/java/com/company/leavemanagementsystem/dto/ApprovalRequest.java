package com.company.leavemanagementsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for leave request approval.
 * Contains approver information and optional comments.
 */
public class ApprovalRequest {

    @NotBlank(message = "Approver ID is required")
    @Size(max = 20, message = "Approver ID cannot exceed 20 characters")
    private String approverId;

    @Size(max = 500, message = "Comments cannot exceed 500 characters")
    private String comments;

    // Default constructor
    public ApprovalRequest() {}

    // Constructor with required fields
    public ApprovalRequest(String approverId) {
        this.approverId = approverId;
    }

    // Constructor with all fields
    public ApprovalRequest(String approverId, String comments) {
        this.approverId = approverId;
        this.comments = comments;
    }

    // Getters and Setters
    public String getApproverId() {
        return approverId;
    }

    public void setApproverId(String approverId) {
        this.approverId = approverId;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @Override
    public String toString() {
        return "ApprovalRequest{" +
                "approverId='" + approverId + '\'' +
                ", comments='" + comments + '\'' +
                '}';
    }
}