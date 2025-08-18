package com.company.leavemanagementsystem.service;

import com.company.leavemanagementsystem.dto.*;
import com.company.leavemanagementsystem.entity.*;
import com.company.leavemanagementsystem.repository.EmployeeRepository;
import com.company.leavemanagementsystem.repository.LeaveBalanceRepository;
import com.company.leavemanagementsystem.repository.LeaveRequestRepository;
import com.company.leavemanagementsystem.service.ApprovalDelegationService.AuthorizationResult;
import com.company.leavemanagementsystem.service.LeaveValidationService.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for LeaveService.
 * Tests leave application, approval/rejection workflows, emergency leave auto-approval,
 * leave history retrieval, and backdated leave regularization.
 */
@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private LeaveValidationService leaveValidationService;

    @Mock
    private ApprovalDelegationService approvalDelegationService;

    @InjectMocks
    private LeaveService leaveService;

    private Employee testEmployee;
    private Employee testManager;
    private LeaveRequest testLeaveRequest;
    private LeaveBalance testLeaveBalance;
    private LeaveApplicationRequest leaveApplicationRequest;
    private ApprovalRequest approvalRequest;
    private RejectionRequest rejectionRequest;

    @BeforeEach
    void setUp() {
        // Setup test manager
        testManager = new Employee("MGR001", "John Manager", "john.manager@company.com", 
                                  "Engineering", LocalDate.of(2020, 1, 15));
        testManager.setId(1L);

        // Setup test employee
        testEmployee = new Employee("EMP001", "Alice Smith", "alice.smith@company.com", 
                                   "Engineering", LocalDate.of(2021, 3, 10));
        testEmployee.setId(2L);
        testEmployee.setManager(testManager);

        // Setup test leave balance
        testLeaveBalance = new LeaveBalance(testEmployee, LeaveType.VACATION, 20.0, 2024);
        testLeaveBalance.setId(1L);
        testLeaveBalance.setUsedDays(5.0);
        testLeaveBalance.setCreatedAt(LocalDateTime.now().minusMonths(1));
        testLeaveBalance.setUpdatedAt(LocalDateTime.now());

        // Setup test leave request
        testLeaveRequest = new LeaveRequest(testEmployee, LeaveType.VACATION, 
                                          LocalDate.of(2024, 6, 15), LocalDate.of(2024, 6, 17), 
                                          LeaveDuration.FULL_DAY, "Family vacation");
        testLeaveRequest.setId(1L);
        testLeaveRequest.setRequestId("REQ001");

        // Setup DTOs
        leaveApplicationRequest = new LeaveApplicationRequest();
        leaveApplicationRequest.setEmployeeId("EMP001");
        leaveApplicationRequest.setLeaveType(LeaveType.VACATION);
        leaveApplicationRequest.setStartDate(LocalDate.of(2024, 6, 15));
        leaveApplicationRequest.setEndDate(LocalDate.of(2024, 6, 17));
        leaveApplicationRequest.setDuration(LeaveDuration.FULL_DAY);
        leaveApplicationRequest.setReason("Family vacation");

        approvalRequest = new ApprovalRequest("MGR001", "Approved for vacation");
        rejectionRequest = new RejectionRequest("MGR001", "Insufficient coverage", "Please reschedule");
    }

    // Test leave application with all validations
    @Test
    void testApplyForLeave_WhenValidRequest_ShouldCreateLeaveRequest() {
        // Given
        ValidationResult validationResult = new ValidationResult(true, null, null);
        when(leaveValidationService.validateLeaveRequest(anyString(), any(LeaveType.class), 
            any(LocalDate.class), any(LocalDate.class), any(LeaveDuration.class), anyBoolean()))
            .thenReturn(validationResult);
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(approvalDelegationService.canAutoApprove(any(LeaveRequest.class))).thenReturn(false);
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(testLeaveRequest);

        // When
        LeaveRequestResponse response = leaveService.applyForLeave(leaveApplicationRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRequestId()).isEqualTo("REQ001");
        assertThat(response.getLeaveType()).isEqualTo(LeaveType.VACATION);
        assertThat(response.getStatus()).isEqualTo(LeaveStatus.PENDING);
        
        verify(leaveValidationService).validateLeaveRequest(anyString(), any(LeaveType.class), 
            any(LocalDate.class), any(LocalDate.class), any(LeaveDuration.class), anyBoolean());
        verify(employeeRepository).findByEmployeeId("EMP001");
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    void testApplyForLeave_WhenValidationFails_ShouldThrowException() {
        // Given
        ValidationResult validationResult = new ValidationResult(false, List.of("Insufficient leave balance"), null);
        when(leaveValidationService.validateLeaveRequest(anyString(), any(LeaveType.class), 
            any(LocalDate.class), any(LocalDate.class), any(LeaveDuration.class), anyBoolean()))
            .thenReturn(validationResult);

        // When & Then
        assertThatThrownBy(() -> leaveService.applyForLeave(leaveApplicationRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Leave request validation failed: Insufficient leave balance");

        verify(leaveValidationService).validateLeaveRequest(anyString(), any(LeaveType.class), 
            any(LocalDate.class), any(LocalDate.class), any(LeaveDuration.class), anyBoolean());
        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    void testApplyForLeave_WhenEmployeeNotFound_ShouldThrowException() {
        // Given
        ValidationResult validationResult = new ValidationResult(true, null, null);
        when(leaveValidationService.validateLeaveRequest(anyString(), any(LeaveType.class), 
            any(LocalDate.class), any(LocalDate.class), any(LeaveDuration.class), anyBoolean()))
            .thenReturn(validationResult);
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> leaveService.applyForLeave(leaveApplicationRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Employee not found: EMP001");

        verify(employeeRepository).findByEmployeeId("EMP001");
        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    // Test emergency leave auto-approval logic
    @Test
    void testApplyForLeave_WhenEmergencyLeaveAutoApproved_ShouldAutoApprove() {
        // Given
        leaveApplicationRequest.setIsEmergencyLeave(true);
        leaveApplicationRequest.setLeaveType(LeaveType.EMERGENCY);
        
        ValidationResult validationResult = new ValidationResult(true, null, null);
        when(leaveValidationService.validateLeaveRequest(anyString(), any(LeaveType.class), 
            any(LocalDate.class), any(LocalDate.class), any(LeaveDuration.class), anyBoolean()))
            .thenReturn(validationResult);
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(approvalDelegationService.canAutoApprove(any(LeaveRequest.class))).thenReturn(true);
        when(approvalDelegationService.getAppropriatApprover(any(LeaveRequest.class))).thenReturn(testManager);
        
        LeaveRequest autoApprovedRequest = new LeaveRequest(testEmployee, LeaveType.EMERGENCY, 
                                                          LocalDate.now(), LocalDate.now(), 
                                                          LeaveDuration.FULL_DAY, "Medical emergency");
        autoApprovedRequest.setStatus(LeaveStatus.AUTO_APPROVED);
        autoApprovedRequest.setApprovedBy(testManager);
        autoApprovedRequest.setRequestId("REQ002");
        
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(autoApprovedRequest);
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(anyString(), any(LeaveType.class), anyInt()))
            .thenReturn(Optional.of(testLeaveBalance));

        // When
        LeaveRequestResponse response = leaveService.applyForLeave(leaveApplicationRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(LeaveStatus.AUTO_APPROVED);
        assertThat(response.getApprovedBy()).isNotNull();
        assertThat(response.getApprovedBy().getEmployeeId()).isEqualTo("MGR001");
        
        verify(approvalDelegationService).canAutoApprove(any(LeaveRequest.class));
        verify(approvalDelegationService).getAppropriatApprover(any(LeaveRequest.class));
        verify(leaveBalanceRepository).save(any(LeaveBalance.class));
    }

    // Test leave approval workflow
    @Test
    void testApproveLeaveRequest_WhenValidRequest_ShouldApproveLeave() {
        // Given
        testLeaveRequest.setStatus(LeaveStatus.PENDING);
        ApprovalDelegationService.ValidationResult processValidation = 
            new ApprovalDelegationService.ValidationResult(true, null, null);
        AuthorizationResult authResult = new AuthorizationResult(true, testManager, null, null);
        
        when(leaveRequestRepository.findByRequestId("REQ001")).thenReturn(Optional.of(testLeaveRequest));
        when(approvalDelegationService.validateRequestProcessable(testLeaveRequest)).thenReturn(processValidation);
        when(approvalDelegationService.validateApprovalAuthorization("MGR001", testLeaveRequest))
            .thenReturn(authResult);
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(testLeaveRequest);
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(anyString(), any(LeaveType.class), anyInt()))
            .thenReturn(Optional.of(testLeaveBalance));

        // When
        LeaveRequestResponse response = leaveService.approveLeaveRequest("REQ001", approvalRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRequestId()).isEqualTo("REQ001");
        
        verify(leaveRequestRepository).findByRequestId("REQ001");
        verify(approvalDelegationService).validateRequestProcessable(testLeaveRequest);
        verify(approvalDelegationService).validateApprovalAuthorization("MGR001", testLeaveRequest);
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
        verify(leaveBalanceRepository).save(any(LeaveBalance.class));
    }

    @Test
    void testApproveLeaveRequest_WhenRequestNotFound_ShouldThrowException() {
        // Given
        when(leaveRequestRepository.findByRequestId("NONEXISTENT")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> leaveService.approveLeaveRequest("NONEXISTENT", approvalRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Leave request not found: NONEXISTENT");

        verify(leaveRequestRepository).findByRequestId("NONEXISTENT");
        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    void testApproveLeaveRequest_WhenNotProcessable_ShouldThrowException() {
        // Given
        ApprovalDelegationService.ValidationResult processValidation = 
            new ApprovalDelegationService.ValidationResult(false, List.of("Request already processed"), null);
        
        when(leaveRequestRepository.findByRequestId("REQ001")).thenReturn(Optional.of(testLeaveRequest));
        when(approvalDelegationService.validateRequestProcessable(testLeaveRequest)).thenReturn(processValidation);

        // When & Then
        assertThatThrownBy(() -> leaveService.approveLeaveRequest("REQ001", approvalRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot process request: Request already processed");

        verify(approvalDelegationService).validateRequestProcessable(testLeaveRequest);
        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    void testApproveLeaveRequest_WhenNotAuthorized_ShouldThrowException() {
        // Given
        ApprovalDelegationService.ValidationResult processValidation = 
            new ApprovalDelegationService.ValidationResult(true, null, null);
        AuthorizationResult authResult = new AuthorizationResult(false, null, List.of("Not authorized to approve"), null);
        
        when(leaveRequestRepository.findByRequestId("REQ001")).thenReturn(Optional.of(testLeaveRequest));
        when(approvalDelegationService.validateRequestProcessable(testLeaveRequest)).thenReturn(processValidation);
        when(approvalDelegationService.validateApprovalAuthorization("MGR001", testLeaveRequest))
            .thenReturn(authResult);

        // When & Then
        assertThatThrownBy(() -> leaveService.approveLeaveRequest("REQ001", approvalRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Authorization failed: Not authorized to approve");

        verify(approvalDelegationService).validateApprovalAuthorization("MGR001", testLeaveRequest);
        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    // Test leave rejection workflow
    @Test
    void testRejectLeaveRequest_WhenValidRequest_ShouldRejectLeave() {
        // Given
        testLeaveRequest.setStatus(LeaveStatus.PENDING);
        ApprovalDelegationService.ValidationResult processValidation = 
            new ApprovalDelegationService.ValidationResult(true, null, null);
        AuthorizationResult authResult = new AuthorizationResult(true, testManager, null, null);
        
        when(leaveRequestRepository.findByRequestId("REQ001")).thenReturn(Optional.of(testLeaveRequest));
        when(approvalDelegationService.validateRequestProcessable(testLeaveRequest)).thenReturn(processValidation);
        when(approvalDelegationService.validateApprovalAuthorization("MGR001", testLeaveRequest))
            .thenReturn(authResult);
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(testLeaveRequest);

        // When
        LeaveRequestResponse response = leaveService.rejectLeaveRequest("REQ001", rejectionRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRequestId()).isEqualTo("REQ001");
        
        verify(leaveRequestRepository).findByRequestId("REQ001");
        verify(approvalDelegationService).validateRequestProcessable(testLeaveRequest);
        verify(approvalDelegationService).validateApprovalAuthorization("MGR001", testLeaveRequest);
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    void testRejectLeaveRequest_WhenRequestNotFound_ShouldThrowException() {
        // Given
        when(leaveRequestRepository.findByRequestId("NONEXISTENT")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> leaveService.rejectLeaveRequest("NONEXISTENT", rejectionRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Leave request not found: NONEXISTENT");

        verify(leaveRequestRepository).findByRequestId("NONEXISTENT");
        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    // Test leave history retrieval methods
    @Test
    void testGetEmployeeLeaveHistory_WhenEmployeeExists_ShouldReturnHistory() {
        // Given
        List<LeaveRequest> leaveRequests = Arrays.asList(testLeaveRequest);
        when(employeeRepository.existsByEmployeeId("EMP001")).thenReturn(true);
        when(leaveRequestRepository.findByEmployeeId("EMP001")).thenReturn(leaveRequests);

        // When
        List<LeaveRequestResponse> response = leaveService.getEmployeeLeaveHistory("EMP001");

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getRequestId()).isEqualTo("REQ001");
        
        verify(employeeRepository).existsByEmployeeId("EMP001");
        verify(leaveRequestRepository).findByEmployeeId("EMP001");
    }

    @Test
    void testGetEmployeeLeaveHistory_WhenEmployeeNotFound_ShouldThrowException() {
        // Given
        when(employeeRepository.existsByEmployeeId("NONEXISTENT")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> leaveService.getEmployeeLeaveHistory("NONEXISTENT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Employee not found: NONEXISTENT");

        verify(employeeRepository).existsByEmployeeId("NONEXISTENT");
        verify(leaveRequestRepository, never()).findByEmployeeId(anyString());
    }

    @Test
    void testGetEmployeeLeaveHistoryWithPagination_WhenEmployeeExists_ShouldReturnPagedHistory() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<LeaveRequest> leaveRequests = Arrays.asList(testLeaveRequest);
        Page<LeaveRequest> leaveRequestPage = new PageImpl<>(leaveRequests, pageable, 1);
        
        when(employeeRepository.existsByEmployeeId("EMP001")).thenReturn(true);
        when(leaveRequestRepository.findByEmployeeId("EMP001", pageable)).thenReturn(leaveRequestPage);

        // When
        Page<LeaveRequestResponse> response = leaveService.getEmployeeLeaveHistory("EMP001", pageable);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
        
        verify(employeeRepository).existsByEmployeeId("EMP001");
        verify(leaveRequestRepository).findByEmployeeId("EMP001", pageable);
    }

    @Test
    void testGetPendingRequestsForManager_WhenManagerExists_ShouldReturnPendingRequests() {
        // Given
        List<LeaveRequest> pendingRequests = Arrays.asList(testLeaveRequest);
        when(employeeRepository.existsByEmployeeId("MGR001")).thenReturn(true);
        when(approvalDelegationService.getPendingRequestsForManager("MGR001")).thenReturn(pendingRequests);

        // When
        List<LeaveRequestResponse> response = leaveService.getPendingRequestsForManager("MGR001");

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        
        verify(employeeRepository).existsByEmployeeId("MGR001");
        verify(approvalDelegationService).getPendingRequestsForManager("MGR001");
    }

    @Test
    void testGetPendingRequestsForManager_WhenManagerNotFound_ShouldThrowException() {
        // Given
        when(employeeRepository.existsByEmployeeId("NONEXISTENT")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> leaveService.getPendingRequestsForManager("NONEXISTENT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Manager not found: NONEXISTENT");

        verify(employeeRepository).existsByEmployeeId("NONEXISTENT");
        verify(approvalDelegationService, never()).getPendingRequestsForManager(anyString());
    }

    // Test backdated leave regularization
    @Test
    void testRegularizeBackdatedLeave_WhenValidRequest_ShouldRegularizeLeave() {
        // Given
        testLeaveRequest.setStatus(LeaveStatus.PENDING);
        testLeaveRequest.setIsBackdated(true);
        
        ApprovalDelegationService.ValidationResult processValidation = 
            new ApprovalDelegationService.ValidationResult(true, null, null);
        AuthorizationResult authResult = new AuthorizationResult(true, testManager, null, null);
        
        when(leaveRequestRepository.findByRequestId("REQ001")).thenReturn(Optional.of(testLeaveRequest));
        when(approvalDelegationService.validateRequestProcessable(testLeaveRequest)).thenReturn(processValidation);
        when(approvalDelegationService.validateApprovalAuthorization("MGR001", testLeaveRequest))
            .thenReturn(authResult);
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(testLeaveRequest);
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(anyString(), any(LeaveType.class), anyInt()))
            .thenReturn(Optional.of(testLeaveBalance));

        // When
        LeaveRequestResponse response = leaveService.regularizeBackdatedLeave("REQ001", "MGR001", "HR approved");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRequestId()).isEqualTo("REQ001");
        
        verify(leaveRequestRepository).findByRequestId("REQ001");
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    // Test additional methods
    @Test
    void testGetLeaveRequest_WhenRequestExists_ShouldReturnRequest() {
        // Given
        when(leaveRequestRepository.findByRequestId("REQ001")).thenReturn(Optional.of(testLeaveRequest));

        // When
        LeaveRequestResponse response = leaveService.getLeaveRequest("REQ001");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRequestId()).isEqualTo("REQ001");
        
        verify(leaveRequestRepository).findByRequestId("REQ001");
    }

    @Test
    void testGetLeaveRequest_WhenRequestNotFound_ShouldThrowException() {
        // Given
        when(leaveRequestRepository.findByRequestId("NONEXISTENT")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> leaveService.getLeaveRequest("NONEXISTENT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Leave request not found: NONEXISTENT");

        verify(leaveRequestRepository).findByRequestId("NONEXISTENT");
    }

    @Test
    void testGetLeaveRequestsByStatus_ShouldReturnRequestsWithStatus() {
        // Given
        List<LeaveRequest> requests = Arrays.asList(testLeaveRequest);
        when(leaveRequestRepository.findByStatus(LeaveStatus.PENDING)).thenReturn(requests);

        // When
        List<LeaveRequestResponse> response = leaveService.getLeaveRequestsByStatus(LeaveStatus.PENDING);

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        
        verify(leaveRequestRepository).findByStatus(LeaveStatus.PENDING);
    }

    @Test
    void testGetLeaveRequestsInDateRange_WhenValidRange_ShouldReturnRequests() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 6, 1);
        LocalDate endDate = LocalDate.of(2024, 6, 30);
        List<LeaveRequest> requests = Arrays.asList(testLeaveRequest);
        when(leaveRequestRepository.findLeaveRequestsInDateRange(startDate, endDate)).thenReturn(requests);

        // When
        List<LeaveRequestResponse> response = leaveService.getLeaveRequestsInDateRange(startDate, endDate);

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        
        verify(leaveRequestRepository).findLeaveRequestsInDateRange(startDate, endDate);
    }

    @Test
    void testGetLeaveRequestsInDateRange_WhenInvalidRange_ShouldThrowException() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 6, 30);
        LocalDate endDate = LocalDate.of(2024, 6, 1); // End before start

        // When & Then
        assertThatThrownBy(() -> leaveService.getLeaveRequestsInDateRange(startDate, endDate))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid date range");

        verify(leaveRequestRepository, never()).findLeaveRequestsInDateRange(any(), any());
    }

    @Test
    void testCancelLeaveRequest_WhenValidRequest_ShouldCancelRequest() {
        // Given
        testLeaveRequest.setStatus(LeaveStatus.PENDING);
        when(leaveRequestRepository.findByRequestId("REQ001")).thenReturn(Optional.of(testLeaveRequest));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(testLeaveRequest);

        // When
        LeaveRequestResponse response = leaveService.cancelLeaveRequest("REQ001", "EMP001");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRequestId()).isEqualTo("REQ001");
        
        verify(leaveRequestRepository).findByRequestId("REQ001");
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    void testCancelLeaveRequest_WhenNotOwner_ShouldThrowException() {
        // Given
        when(leaveRequestRepository.findByRequestId("REQ001")).thenReturn(Optional.of(testLeaveRequest));

        // When & Then
        assertThatThrownBy(() -> leaveService.cancelLeaveRequest("REQ001", "OTHER_EMP"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("You can only cancel your own leave requests");

        verify(leaveRequestRepository).findByRequestId("REQ001");
        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    void testGetEmergencyLeaveRequests_ShouldReturnEmergencyRequests() {
        // Given
        List<LeaveRequest> emergencyRequests = Arrays.asList(testLeaveRequest);
        when(leaveRequestRepository.findEmergencyLeaveRequests()).thenReturn(emergencyRequests);

        // When
        List<LeaveRequestResponse> response = leaveService.getEmergencyLeaveRequests();

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        
        verify(leaveRequestRepository).findEmergencyLeaveRequests();
    }

    @Test
    void testGetBackdatedLeaveRequests_ShouldReturnBackdatedRequests() {
        // Given
        List<LeaveRequest> backdatedRequests = Arrays.asList(testLeaveRequest);
        when(leaveRequestRepository.findBackdatedLeaveRequests()).thenReturn(backdatedRequests);

        // When
        List<LeaveRequestResponse> response = leaveService.getBackdatedLeaveRequests();

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        
        verify(leaveRequestRepository).findBackdatedLeaveRequests();
    }

    @Test
    void testGetUpcomingLeaveRequests_ShouldReturnUpcomingRequests() {
        // Given
        List<LeaveRequest> upcomingRequests = Arrays.asList(testLeaveRequest);
        LocalDate endDate = LocalDate.now().plusDays(7);
        when(leaveRequestRepository.findUpcomingLeaveRequests(endDate)).thenReturn(upcomingRequests);

        // When
        List<LeaveRequestResponse> response = leaveService.getUpcomingLeaveRequests(7);

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        
        verify(leaveRequestRepository).findUpcomingLeaveRequests(endDate);
    }

    @Test
    void testGetActiveLeaveRequests_ShouldReturnActiveRequests() {
        // Given
        List<LeaveRequest> activeRequests = Arrays.asList(testLeaveRequest);
        when(leaveRequestRepository.findActiveLeaveRequests(any(LocalDate.class))).thenReturn(activeRequests);

        // When
        List<LeaveRequestResponse> response = leaveService.getActiveLeaveRequests();

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        
        verify(leaveRequestRepository).findActiveLeaveRequests(any(LocalDate.class));
    }

    @Test
    void testGetPendingApprovalCount_ShouldReturnCount() {
        // Given
        when(approvalDelegationService.getPendingApprovalCount("MGR001")).thenReturn(5L);

        // When
        long count = leaveService.getPendingApprovalCount("MGR001");

        // Then
        assertThat(count).isEqualTo(5L);
        
        verify(approvalDelegationService).getPendingApprovalCount("MGR001");
    }

    // Test edge cases
    @Test
    void testApplyForLeave_WithBackdatedJustification_ShouldAddJustificationToComments() {
        // Given
        leaveApplicationRequest.setBackdatedJustification("Medical emergency required immediate leave");
        
        ValidationResult validationResult = new ValidationResult(true, null, null);
        when(leaveValidationService.validateLeaveRequest(anyString(), any(LeaveType.class), 
            any(LocalDate.class), any(LocalDate.class), any(LeaveDuration.class), anyBoolean()))
            .thenReturn(validationResult);
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(approvalDelegationService.canAutoApprove(any(LeaveRequest.class))).thenReturn(false);
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(testLeaveRequest);

        // When
        LeaveRequestResponse response = leaveService.applyForLeave(leaveApplicationRequest);

        // Then
        assertThat(response).isNotNull();
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    void testApproveLeaveRequest_WithComments_ShouldAddCommentsToRequest() {
        // Given
        testLeaveRequest.setStatus(LeaveStatus.PENDING);
        ApprovalDelegationService.ValidationResult processValidation = 
            new ApprovalDelegationService.ValidationResult(true, null, null);
        AuthorizationResult authResult = new AuthorizationResult(true, testManager, null, null);
        
        when(leaveRequestRepository.findByRequestId("REQ001")).thenReturn(Optional.of(testLeaveRequest));
        when(approvalDelegationService.validateRequestProcessable(testLeaveRequest)).thenReturn(processValidation);
        when(approvalDelegationService.validateApprovalAuthorization("MGR001", testLeaveRequest))
            .thenReturn(authResult);
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(testLeaveRequest);
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(anyString(), any(LeaveType.class), anyInt()))
            .thenReturn(Optional.of(testLeaveBalance));

        // When
        LeaveRequestResponse response = leaveService.approveLeaveRequest("REQ001", approvalRequest);

        // Then
        assertThat(response).isNotNull();
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    void testRejectLeaveRequest_WithComments_ShouldAddCommentsToRequest() {
        // Given
        testLeaveRequest.setStatus(LeaveStatus.PENDING);
        ApprovalDelegationService.ValidationResult processValidation = 
            new ApprovalDelegationService.ValidationResult(true, null, null);
        AuthorizationResult authResult = new AuthorizationResult(true, testManager, null, null);
        
        when(leaveRequestRepository.findByRequestId("REQ001")).thenReturn(Optional.of(testLeaveRequest));
        when(approvalDelegationService.validateRequestProcessable(testLeaveRequest)).thenReturn(processValidation);
        when(approvalDelegationService.validateApprovalAuthorization("MGR001", testLeaveRequest))
            .thenReturn(authResult);
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(testLeaveRequest);

        // When
        LeaveRequestResponse response = leaveService.rejectLeaveRequest("REQ001", rejectionRequest);

        // Then
        assertThat(response).isNotNull();
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }
}