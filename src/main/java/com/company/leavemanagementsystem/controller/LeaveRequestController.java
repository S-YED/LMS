package com.company.leavemanagementsystem.controller;

import com.company.leavemanagementsystem.dto.ApprovalRequest;
import com.company.leavemanagementsystem.dto.LeaveApplicationRequest;
import com.company.leavemanagementsystem.dto.LeaveRequestResponse;
import com.company.leavemanagementsystem.dto.RejectionRequest;
import com.company.leavemanagementsystem.entity.LeaveStatus;
import com.company.leavemanagementsystem.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for Leave Request management operations.
 * Provides endpoints for leave applications, approvals, and history tracking.
 */
@RestController
@RequestMapping("/api/leave-requests")
@Tag(name = "Leave Request Management", description = "APIs for managing leave requests and approvals")
public class LeaveRequestController {

    private final LeaveService leaveService;

    @Autowired
    public LeaveRequestController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    /**
     * Apply for leave.
     * 
     * @param request Leave application request containing leave details
     * @return Created leave request response with HTTP 201 status
     */
    @PostMapping
    @Operation(summary = "Apply for leave", description = "Creates a new leave request application")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Leave request created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data or business rule violation"),
        @ApiResponse(responseCode = "404", description = "Employee not found"),
        @ApiResponse(responseCode = "409", description = "Leave request conflicts with existing approved leave")
    })
    public ResponseEntity<LeaveRequestResponse> applyForLeave(
            @Valid @RequestBody LeaveApplicationRequest request) {
        
        LeaveRequestResponse response = leaveService.applyForLeave(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get leave requests for a specific employee.
     * 
     * @param employeeId Employee ID
     * @param page Page number (0-based, default: 0)
     * @param size Page size (default: 20)
     * @param status Filter by leave status (optional)
     * @param startDate Filter by start date from (optional)
     * @param endDate Filter by start date to (optional)
     * @return Paginated list of employee's leave requests
     */
    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get employee leave history", 
               description = "Retrieves paginated leave request history for a specific employee")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Leave requests retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<Page<LeaveRequestResponse>> getEmployeeLeaveRequests(
            @Parameter(description = "Employee ID", required = true)
            @PathVariable String employeeId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by leave status")
            @RequestParam(required = false) LeaveStatus status,
            @Parameter(description = "Filter from start date (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Filter to start date (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LeaveRequestResponse> leaveRequests = leaveService.getEmployeeLeaveHistory(employeeId, pageable);
        
        return ResponseEntity.ok(leaveRequests);
    }

    /**
     * Get all leave requests for a specific employee (no pagination).
     * 
     * @param employeeId Employee ID
     * @return List of all employee's leave requests
     */
    @GetMapping("/employee/{employeeId}/all")
    @Operation(summary = "Get all employee leave requests", 
               description = "Retrieves all leave requests for a specific employee without pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Leave requests retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<List<LeaveRequestResponse>> getAllEmployeeLeaveRequests(
            @Parameter(description = "Employee ID", required = true)
            @PathVariable String employeeId) {
        
        List<LeaveRequestResponse> leaveRequests = leaveService.getEmployeeLeaveHistory(employeeId);
        return ResponseEntity.ok(leaveRequests);
    }

    /**
     * Get pending leave requests for manager approval.
     * 
     * @param managerId Manager's employee ID
     * @param page Page number (0-based, default: 0)
     * @param size Page size (default: 20)
     * @return Paginated list of pending leave requests for the manager
     */
    @GetMapping("/pending")
    @Operation(summary = "Get pending leave requests for manager", 
               description = "Retrieves paginated list of pending leave requests for manager approval")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pending requests retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Manager not found")
    })
    public ResponseEntity<Page<LeaveRequestResponse>> getPendingRequests(
            @Parameter(description = "Manager's employee ID", required = true)
            @RequestParam String managerId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<LeaveRequestResponse> pendingRequests = leaveService.getPendingRequestsForManager(
            managerId, pageable);
        
        return ResponseEntity.ok(pendingRequests);
    }

    /**
     * Get all pending leave requests for manager (no pagination).
     * 
     * @param managerId Manager's employee ID
     * @return List of all pending leave requests for the manager
     */
    @GetMapping("/pending/all")
    @Operation(summary = "Get all pending leave requests for manager", 
               description = "Retrieves all pending leave requests for manager approval without pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pending requests retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Manager not found")
    })
    public ResponseEntity<List<LeaveRequestResponse>> getAllPendingRequests(
            @Parameter(description = "Manager's employee ID", required = true)
            @RequestParam String managerId) {
        
        List<LeaveRequestResponse> pendingRequests = leaveService.getPendingRequestsForManager(managerId);
        return ResponseEntity.ok(pendingRequests);
    }

    /**
     * Approve a leave request.
     * 
     * @param requestId Leave request ID
     * @param request Approval request containing approver details
     * @return Updated leave request response with HTTP 200 status
     */
    @PutMapping("/{requestId}/approve")
    @Operation(summary = "Approve leave request", description = "Approves a pending leave request")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Leave request approved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or business rule violation"),
        @ApiResponse(responseCode = "404", description = "Leave request not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized to approve this request"),
        @ApiResponse(responseCode = "409", description = "Leave request is not in pending status")
    })
    public ResponseEntity<LeaveRequestResponse> approveLeaveRequest(
            @Parameter(description = "Leave request ID", required = true)
            @PathVariable String requestId,
            @Valid @RequestBody ApprovalRequest request) {
        
        LeaveRequestResponse response = leaveService.approveLeaveRequest(requestId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Reject a leave request.
     * 
     * @param requestId Leave request ID
     * @param request Rejection request containing approver details and reason
     * @return Updated leave request response with HTTP 200 status
     */
    @PutMapping("/{requestId}/reject")
    @Operation(summary = "Reject leave request", description = "Rejects a pending leave request")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Leave request rejected successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or missing rejection reason"),
        @ApiResponse(responseCode = "404", description = "Leave request not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized to reject this request"),
        @ApiResponse(responseCode = "409", description = "Leave request is not in pending status")
    })
    public ResponseEntity<LeaveRequestResponse> rejectLeaveRequest(
            @Parameter(description = "Leave request ID", required = true)
            @PathVariable String requestId,
            @Valid @RequestBody RejectionRequest request) {
        
        LeaveRequestResponse response = leaveService.rejectLeaveRequest(requestId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific leave request by ID.
     * 
     * @param requestId Leave request ID
     * @return Leave request details
     */
    @GetMapping("/{requestId}")
    @Operation(summary = "Get leave request by ID", description = "Retrieves a specific leave request by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Leave request found"),
        @ApiResponse(responseCode = "404", description = "Leave request not found")
    })
    public ResponseEntity<LeaveRequestResponse> getLeaveRequest(
            @Parameter(description = "Leave request ID", required = true)
            @PathVariable String requestId) {
        
        LeaveRequestResponse response = leaveService.getLeaveRequest(requestId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all leave requests with filtering and pagination.
     * 
     * @param page Page number (0-based, default: 0)
     * @param size Page size (default: 20)
     * @param status Filter by leave status (optional)
     * @param department Filter by department (optional)
     * @param startDate Filter by start date from (optional)
     * @param endDate Filter by start date to (optional)
     * @return Paginated list of leave requests
     */
    @GetMapping
    @Operation(summary = "Get all leave requests", 
               description = "Retrieves paginated list of all leave requests with filtering options")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Leave requests retrieved successfully")
    })
    public ResponseEntity<Page<LeaveRequestResponse>> getAllLeaveRequests(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by leave status")
            @RequestParam(required = false) LeaveStatus status,
            @Parameter(description = "Filter by department")
            @RequestParam(required = false) String department,
            @Parameter(description = "Filter from start date (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Filter to start date (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LeaveRequestResponse> leaveRequests = leaveService.getLeaveRequestsByStatus(status, pageable);
        
        return ResponseEntity.ok(leaveRequests);
    }

    /**
     * Cancel a pending leave request.
     * Only the employee who created the request can cancel it, and only if it's still pending.
     * 
     * @param requestId Leave request ID
     * @param employeeId Employee ID (for authorization)
     * @return Updated leave request response
     */
    @PutMapping("/{requestId}/cancel")
    @Operation(summary = "Cancel leave request", 
               description = "Cancels a pending leave request (only by the requesting employee)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Leave request cancelled successfully"),
        @ApiResponse(responseCode = "404", description = "Leave request not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized to cancel this request"),
        @ApiResponse(responseCode = "409", description = "Leave request cannot be cancelled (not pending)")
    })
    public ResponseEntity<LeaveRequestResponse> cancelLeaveRequest(
            @Parameter(description = "Leave request ID", required = true)
            @PathVariable String requestId,
            @Parameter(description = "Employee ID for authorization", required = true)
            @RequestParam String employeeId) {
        
        LeaveRequestResponse response = leaveService.cancelLeaveRequest(requestId, employeeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get leave requests that overlap with a specific date range.
     * Useful for checking team availability.
     * 
     * @param startDate Start date of the range
     * @param endDate End date of the range
     * @param department Filter by department (optional)
     * @return List of overlapping leave requests
     */
    @GetMapping("/overlapping")
    @Operation(summary = "Get overlapping leave requests", 
               description = "Retrieves leave requests that overlap with a specific date range")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Overlapping requests retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid date range")
    })
    public ResponseEntity<List<LeaveRequestResponse>> getOverlappingLeaveRequests(
            @Parameter(description = "Start date (yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Filter by department")
            @RequestParam(required = false) String department) {
        
        List<LeaveRequestResponse> overlappingRequests = leaveService.getLeaveRequestsInDateRange(startDate, endDate);
        
        return ResponseEntity.ok(overlappingRequests);
    }
}