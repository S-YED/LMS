package com.company.leavemanagementsystem.service;

import com.company.leavemanagementsystem.dto.*;
import com.company.leavemanagementsystem.entity.*;
import com.company.leavemanagementsystem.repository.EmployeeRepository;
import com.company.leavemanagementsystem.repository.LeaveBalanceRepository;
import com.company.leavemanagementsystem.repository.LeaveRequestRepository;
import com.company.leavemanagementsystem.service.LeaveValidationService.ValidationResult;
import com.company.leavemanagementsystem.service.ApprovalDelegationService.AuthorizationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for comprehensive leave management operations.
 * Handles leave application, approval/rejection workflows, emergency leave auto-approval,
 * leave history retrieval, and backdated leave regularization.
 */
@Service
@Transactional
public class LeaveService {

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveValidationService leaveValidationService;
    private final ApprovalDelegationService approvalDelegationService;

    @Autowired
    public LeaveService(EmployeeRepository employeeRepository,
                       LeaveRequestRepository leaveRequestRepository,
                       LeaveBalanceRepository leaveBalanceRepository,
                       LeaveValidationService leaveValidationService,
                       ApprovalDelegationService approvalDelegationService) {
        this.employeeRepository = employeeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveValidationService = leaveValidationService;
        this.approvalDelegationService = approvalDelegationService;
    }

    /**
     * Apply for leave with comprehensive validation and auto-approval logic
     * @param request the leave application request
     * @return LeaveRequestResponse containing the created leave request details
     * @throws IllegalArgumentException if validation fails
     */
    public LeaveRequestResponse applyForLeave(LeaveApplicationRequest request) {
        // 1. Validate the leave request
        ValidationResult validation = leaveValidationService.validateLeaveRequest(
            request.getEmployeeId(),
            request.getLeaveType(),
            request.getStartDate(),
            request.getEndDate(),
            request.getDuration(),
            request.getIsEmergencyLeave() != null ? request.getIsEmergencyLeave() : false
        );

        if (!validation.isValid()) {
            throw new IllegalArgumentException("Leave request validation failed: " + validation.getErrorMessage());
        }

        // 2. Get employee
        Employee employee = employeeRepository.findByEmployeeId(request.getEmployeeId())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + request.getEmployeeId()));

        // 3. Create leave request entity
        LeaveRequest leaveRequest = new LeaveRequest(
            employee,
            request.getLeaveType(),
            request.getStartDate(),
            request.getEndDate(),
            request.getDuration(),
            request.getReason()
        );

        leaveRequest.setComments(request.getComments());
        leaveRequest.setIsEmergencyLeave(request.getIsEmergencyLeave() != null ? request.getIsEmergencyLeave() : false);

        // Set backdated justification if provided
        if (request.isBackdated() && request.getBackdatedJustification() != null) {
            leaveRequest.setComments(
                (leaveRequest.getComments() != null ? leaveRequest.getComments() + "\n" : "") +
                "Backdated Justification: " + request.getBackdatedJustification()
            );
        }

        // 4. Check for auto-approval (emergency leave <= 2 days)
        if (approvalDelegationService.canAutoApprove(leaveRequest)) {
            leaveRequest.setStatus(LeaveStatus.AUTO_APPROVED);
            leaveRequest.setApprovedAt(LocalDateTime.now());
            // For auto-approved requests, set system as approver or find appropriate manager
            Employee approver = approvalDelegationService.getAppropriatApprover(leaveRequest);
            if (approver != null) {
                leaveRequest.setApprovedBy(approver);
            }
        }

        // 5. Save the leave request
        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);

        // 6. Update leave balance if approved
        if (savedRequest.getStatus() == LeaveStatus.AUTO_APPROVED) {
            updateLeaveBalance(savedRequest, true);
        }

        // 7. Convert to response DTO
        return convertToLeaveRequestResponse(savedRequest);
    }

    /**
     * Approve a leave request
     * @param requestId the leave request ID
     * @param approvalRequest the approval request containing approver details
     * @return LeaveRequestResponse containing updated leave request details
     * @throws IllegalArgumentException if validation fails
     */
    public LeaveRequestResponse approveLeaveRequest(String requestId, ApprovalRequest approvalRequest) {
        // 1. Find the leave request
        LeaveRequest leaveRequest = leaveRequestRepository.findByRequestId(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Leave request not found: " + requestId));

        // 2. Validate that request can be processed
        ApprovalDelegationService.ValidationResult processValidation = 
            approvalDelegationService.validateRequestProcessable(leaveRequest);
        if (!processValidation.isValid()) {
            throw new IllegalArgumentException("Cannot process request: " + processValidation.getErrorMessage());
        }

        // 3. Validate authorization
        AuthorizationResult authResult = approvalDelegationService.validateApprovalAuthorization(
            approvalRequest.getApproverId(), leaveRequest);
        if (!authResult.isAuthorized()) {
            throw new IllegalArgumentException("Authorization failed: " + authResult.getErrorMessage());
        }

        // 4. Approve the request
        Employee approver = authResult.getApprover();
        leaveRequest.approve(approver);
        
        // Add approval comments if provided
        if (approvalRequest.getComments() != null && !approvalRequest.getComments().trim().isEmpty()) {
            String existingComments = leaveRequest.getComments();
            String approvalComments = "Approval Comments: " + approvalRequest.getComments();
            leaveRequest.setComments(
                existingComments != null ? existingComments + "\n" + approvalComments : approvalComments
            );
        }

        // 5. Save the updated request
        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);

        // 6. Update leave balance
        updateLeaveBalance(savedRequest, true);

        // 7. Convert to response DTO
        return convertToLeaveRequestResponse(savedRequest);
    }

    /**
     * Reject a leave request
     * @param requestId the leave request ID
     * @param rejectionRequest the rejection request containing approver details and reason
     * @return LeaveRequestResponse containing updated leave request details
     * @throws IllegalArgumentException if validation fails
     */
    public LeaveRequestResponse rejectLeaveRequest(String requestId, RejectionRequest rejectionRequest) {
        // 1. Find the leave request
        LeaveRequest leaveRequest = leaveRequestRepository.findByRequestId(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Leave request not found: " + requestId));

        // 2. Validate that request can be processed
        ApprovalDelegationService.ValidationResult processValidation = 
            approvalDelegationService.validateRequestProcessable(leaveRequest);
        if (!processValidation.isValid()) {
            throw new IllegalArgumentException("Cannot process request: " + processValidation.getErrorMessage());
        }

        // 3. Validate authorization
        AuthorizationResult authResult = approvalDelegationService.validateApprovalAuthorization(
            rejectionRequest.getApproverId(), leaveRequest);
        if (!authResult.isAuthorized()) {
            throw new IllegalArgumentException("Authorization failed: " + authResult.getErrorMessage());
        }

        // 4. Reject the request
        Employee approver = authResult.getApprover();
        leaveRequest.reject(approver, rejectionRequest.getRejectionReason());
        
        // Add rejection comments if provided
        if (rejectionRequest.getComments() != null && !rejectionRequest.getComments().trim().isEmpty()) {
            String existingComments = leaveRequest.getComments();
            String rejectionComments = "Rejection Comments: " + rejectionRequest.getComments();
            leaveRequest.setComments(
                existingComments != null ? existingComments + "\n" + rejectionComments : rejectionComments
            );
        }

        // 5. Save the updated request
        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);

        // 6. Convert to response DTO
        return convertToLeaveRequestResponse(savedRequest);
    }

    /**
     * Get leave request by request ID
     * @param requestId the leave request ID
     * @return LeaveRequestResponse containing leave request details
     * @throws IllegalArgumentException if request not found
     */
    @Transactional(readOnly = true)
    public LeaveRequestResponse getLeaveRequest(String requestId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findByRequestId(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Leave request not found: " + requestId));
        
        return convertToLeaveRequestResponse(leaveRequest);
    }

    /**
     * Get all leave requests for an employee
     * @param employeeId the employee ID
     * @return List of LeaveRequestResponse containing employee's leave history
     */
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getEmployeeLeaveHistory(String employeeId) {
        // Validate employee exists
        if (!employeeRepository.existsByEmployeeId(employeeId)) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }

        List<LeaveRequest> leaveRequests = leaveRequestRepository.findByEmployeeId(employeeId);
        return leaveRequests.stream()
            .map(this::convertToLeaveRequestResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get leave requests for an employee with pagination
     * @param employeeId the employee ID
     * @param pageable pagination information
     * @return Page of LeaveRequestResponse containing employee's leave history
     */
    @Transactional(readOnly = true)
    public Page<LeaveRequestResponse> getEmployeeLeaveHistory(String employeeId, Pageable pageable) {
        // Validate employee exists
        if (!employeeRepository.existsByEmployeeId(employeeId)) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }

        Page<LeaveRequest> leaveRequests = leaveRequestRepository.findByEmployeeId(employeeId, pageable);
        return leaveRequests.map(this::convertToLeaveRequestResponse);
    }

    /**
     * Get pending leave requests for a manager
     * @param managerId the manager's employee ID
     * @return List of LeaveRequestResponse containing pending requests for approval
     */
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getPendingRequestsForManager(String managerId) {
        // Validate manager exists
        if (!employeeRepository.existsByEmployeeId(managerId)) {
            throw new IllegalArgumentException("Manager not found: " + managerId);
        }

        List<LeaveRequest> pendingRequests = approvalDelegationService.getPendingRequestsForManager(managerId);
        return pendingRequests.stream()
            .map(this::convertToLeaveRequestResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get pending leave requests for a manager with pagination
     * @param managerId the manager's employee ID
     * @param pageable pagination information
     * @return Page of LeaveRequestResponse containing pending requests for approval
     */
    @Transactional(readOnly = true)
    public Page<LeaveRequestResponse> getPendingRequestsForManager(String managerId, Pageable pageable) {
        // Validate manager exists
        if (!employeeRepository.existsByEmployeeId(managerId)) {
            throw new IllegalArgumentException("Manager not found: " + managerId);
        }

        return leaveRequestRepository.findPendingRequestsByManager(managerId, pageable)
            .map(this::convertToLeaveRequestResponse);
    }

    /**
     * Get leave requests by status
     * @param status the leave status
     * @return List of LeaveRequestResponse with the specified status
     */
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getLeaveRequestsByStatus(LeaveStatus status) {
        List<LeaveRequest> leaveRequests = leaveRequestRepository.findByStatus(status);
        return leaveRequests.stream()
            .map(this::convertToLeaveRequestResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get leave requests by status with pagination
     * @param status the leave status
     * @param pageable pagination information
     * @return Page of LeaveRequestResponse with the specified status
     */
    @Transactional(readOnly = true)
    public Page<LeaveRequestResponse> getLeaveRequestsByStatus(LeaveStatus status, Pageable pageable) {
        return leaveRequestRepository.findByStatus(status, pageable)
            .map(this::convertToLeaveRequestResponse);
    }

    /**
     * Get leave requests within a date range
     * @param startDate the range start date
     * @param endDate the range end date
     * @return List of LeaveRequestResponse within the date range
     */
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getLeaveRequestsInDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Invalid date range");
        }

        List<LeaveRequest> leaveRequests = leaveRequestRepository.findLeaveRequestsInDateRange(startDate, endDate);
        return leaveRequests.stream()
            .map(this::convertToLeaveRequestResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get emergency leave requests
     * @return List of LeaveRequestResponse for emergency leaves
     */
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getEmergencyLeaveRequests() {
        List<LeaveRequest> emergencyRequests = leaveRequestRepository.findEmergencyLeaveRequests();
        return emergencyRequests.stream()
            .map(this::convertToLeaveRequestResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get backdated leave requests
     * @return List of LeaveRequestResponse for backdated requests
     */
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getBackdatedLeaveRequests() {
        List<LeaveRequest> backdatedRequests = leaveRequestRepository.findBackdatedLeaveRequests();
        return backdatedRequests.stream()
            .map(this::convertToLeaveRequestResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get upcoming leave requests (starting within next N days)
     * @param days number of days to look ahead
     * @return List of LeaveRequestResponse for upcoming leaves
     */
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getUpcomingLeaveRequests(int days) {
        LocalDate endDate = LocalDate.now().plusDays(days);
        List<LeaveRequest> upcomingRequests = leaveRequestRepository.findUpcomingLeaveRequests(endDate);
        return upcomingRequests.stream()
            .map(this::convertToLeaveRequestResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get currently active leave requests
     * @return List of LeaveRequestResponse for active leaves
     */
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getActiveLeaveRequests() {
        List<LeaveRequest> activeRequests = leaveRequestRepository.findActiveLeaveRequests(LocalDate.now());
        return activeRequests.stream()
            .map(this::convertToLeaveRequestResponse)
            .collect(Collectors.toList());
    }

    /**
     * Cancel a leave request (only if in pending status)
     * @param requestId the leave request ID
     * @param employeeId the employee ID (for authorization)
     * @return LeaveRequestResponse containing updated leave request details
     * @throws IllegalArgumentException if validation fails
     */
    public LeaveRequestResponse cancelLeaveRequest(String requestId, String employeeId) {
        // 1. Find the leave request
        LeaveRequest leaveRequest = leaveRequestRepository.findByRequestId(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Leave request not found: " + requestId));

        // 2. Validate that the employee owns this request
        if (!leaveRequest.getEmployee().getEmployeeId().equals(employeeId)) {
            throw new IllegalArgumentException("You can only cancel your own leave requests");
        }

        // 3. Check if request can be cancelled
        if (!leaveRequest.getStatus().isModifiable()) {
            throw new IllegalArgumentException("Cannot cancel leave request in status: " + 
                leaveRequest.getStatus().getDisplayName());
        }

        // 4. Cancel the request
        leaveRequest.cancel();

        // 5. Save the updated request
        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);

        // 6. Convert to response DTO
        return convertToLeaveRequestResponse(savedRequest);
    }

    /**
     * Get count of pending requests for a manager
     * @param managerId the manager's employee ID
     * @return number of pending requests
     */
    @Transactional(readOnly = true)
    public long getPendingApprovalCount(String managerId) {
        return approvalDelegationService.getPendingApprovalCount(managerId);
    }

    /**
     * Regularize backdated leave request (for HR/Admin use)
     * @param requestId the leave request ID
     * @param approverId the approver ID
     * @param comments regularization comments
     * @return LeaveRequestResponse containing updated leave request details
     */
    public LeaveRequestResponse regularizeBackdatedLeave(String requestId, String approverId, String comments) {
        // Create approval request for backdated regularization
        ApprovalRequest approvalRequest = new ApprovalRequest(approverId, 
            "Backdated Leave Regularization: " + (comments != null ? comments : "Approved by HR"));
        
        return approveLeaveRequest(requestId, approvalRequest);
    }

    /**
     * Update leave balance when a request is approved or cancelled
     * @param leaveRequest the leave request
     * @param isApproval true if approving, false if cancelling
     */
    private void updateLeaveBalance(LeaveRequest leaveRequest, boolean isApproval) {
        String employeeId = leaveRequest.getEmployee().getEmployeeId();
        LeaveType leaveType = leaveRequest.getLeaveType();
        int year = leaveRequest.getStartDate().getYear();
        double days = leaveRequest.getTotalDays();

        Optional<LeaveBalance> balanceOpt = leaveBalanceRepository
            .findByEmployeeIdAndLeaveTypeAndYear(employeeId, leaveType, year);

        if (balanceOpt.isPresent()) {
            LeaveBalance balance = balanceOpt.get();
            if (isApproval) {
                balance.deductDays(days);
            } else {
                balance.addDays(days);
            }
            leaveBalanceRepository.save(balance);
        }
    }

    /**
     * Convert LeaveRequest entity to LeaveRequestResponse DTO
     * @param leaveRequest the leave request entity
     * @return LeaveRequestResponse DTO
     */
    private LeaveRequestResponse convertToLeaveRequestResponse(LeaveRequest leaveRequest) {
        LeaveRequestResponse response = new LeaveRequestResponse();
        
        response.setId(leaveRequest.getId());
        response.setRequestId(leaveRequest.getRequestId());
        response.setLeaveType(leaveRequest.getLeaveType());
        response.setStartDate(leaveRequest.getStartDate());
        response.setEndDate(leaveRequest.getEndDate());
        response.setDuration(leaveRequest.getDuration());
        response.setTotalDays(leaveRequest.getTotalDays());
        response.setReason(leaveRequest.getReason());
        response.setComments(leaveRequest.getComments());
        response.setStatus(leaveRequest.getStatus());
        response.setIsEmergencyLeave(leaveRequest.getIsEmergencyLeave());
        response.setIsBackdated(leaveRequest.getIsBackdated());
        response.setApprovedAt(leaveRequest.getApprovedAt());
        response.setRejectionReason(leaveRequest.getRejectionReason());
        response.setCreatedAt(leaveRequest.getCreatedAt());
        response.setUpdatedAt(leaveRequest.getUpdatedAt());

        // Set employee info
        Employee employee = leaveRequest.getEmployee();
        LeaveRequestResponse.EmployeeInfo employeeInfo = new LeaveRequestResponse.EmployeeInfo(
            employee.getEmployeeId(),
            employee.getName(),
            employee.getEmail(),
            employee.getDepartment()
        );
        response.setEmployee(employeeInfo);

        // Set approver info if available
        if (leaveRequest.getApprovedBy() != null) {
            Employee approver = leaveRequest.getApprovedBy();
            LeaveRequestResponse.ApproverInfo approverInfo = new LeaveRequestResponse.ApproverInfo(
                approver.getEmployeeId(),
                approver.getName(),
                approver.getEmail()
            );
            response.setApprovedBy(approverInfo);
        }

        return response;
    }
}
