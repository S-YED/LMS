package com.company.leavemanagementsystem.controller;

import com.company.leavemanagementsystem.dto.ApprovalRequest;
import com.company.leavemanagementsystem.dto.LeaveApplicationRequest;
import com.company.leavemanagementsystem.dto.LeaveRequestResponse;
import com.company.leavemanagementsystem.dto.RejectionRequest;
import com.company.leavemanagementsystem.entity.LeaveDuration;
import com.company.leavemanagementsystem.entity.LeaveStatus;
import com.company.leavemanagementsystem.entity.LeaveType;
import com.company.leavemanagementsystem.service.LeaveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive test suite for LeaveRequestController.
 * Tests all REST endpoints, request validation, error responses, and leave workflow scenarios.
 */
@WebMvcTest(LeaveRequestController.class)
class LeaveRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LeaveService leaveService;

    @Autowired
    private ObjectMapper objectMapper;

    private LeaveApplicationRequest leaveApplicationRequest;
    private LeaveRequestResponse leaveRequestResponse;
    private ApprovalRequest approvalRequest;
    private RejectionRequest rejectionRequest;

    @BeforeEach
    void setUp() {
        // Create test DTOs
        leaveApplicationRequest = new LeaveApplicationRequest();
        leaveApplicationRequest.setEmployeeId("EMP001");
        leaveApplicationRequest.setLeaveType(LeaveType.VACATION);
        leaveApplicationRequest.setStartDate(LocalDate.of(2024, 4, 15));
        leaveApplicationRequest.setEndDate(LocalDate.of(2024, 4, 17));
        leaveApplicationRequest.setDuration(LeaveDuration.FULL_DAY);
        leaveApplicationRequest.setReason("Family vacation");
        leaveApplicationRequest.setComments("Planning a trip with family");
        leaveApplicationRequest.setIsEmergencyLeave(false);

        // Create employee info
        LeaveRequestResponse.EmployeeInfo employeeInfo = new LeaveRequestResponse.EmployeeInfo(
            "EMP001", "Alice Smith", "alice.smith@company.com", "Engineering");

        // Create approver info
        LeaveRequestResponse.ApproverInfo approverInfo = new LeaveRequestResponse.ApproverInfo(
            "MGR001", "John Manager", "john.manager@company.com");

        leaveRequestResponse = new LeaveRequestResponse();
        leaveRequestResponse.setId(1L);
        leaveRequestResponse.setRequestId("LR-12345678");
        leaveRequestResponse.setLeaveType(LeaveType.VACATION);
        leaveRequestResponse.setStartDate(LocalDate.of(2024, 4, 15));
        leaveRequestResponse.setEndDate(LocalDate.of(2024, 4, 17));
        leaveRequestResponse.setDuration(LeaveDuration.FULL_DAY);
        leaveRequestResponse.setTotalDays(3.0);
        leaveRequestResponse.setReason("Family vacation");
        leaveRequestResponse.setComments("Planning a trip with family");
        leaveRequestResponse.setStatus(LeaveStatus.PENDING);
        leaveRequestResponse.setIsEmergencyLeave(false);
        leaveRequestResponse.setIsBackdated(false);
        leaveRequestResponse.setEmployee(employeeInfo);
        leaveRequestResponse.setCreatedAt(LocalDateTime.now());
        leaveRequestResponse.setUpdatedAt(LocalDateTime.now());

        approvalRequest = new ApprovalRequest();
        approvalRequest.setApproverId("MGR001");
        approvalRequest.setComments("Approved for family vacation");

        rejectionRequest = new RejectionRequest();
        rejectionRequest.setApproverId("MGR001");
        rejectionRequest.setRejectionReason("Insufficient staffing during requested period");
        rejectionRequest.setComments("Please consider alternative dates");
    }

    @Test
    void testApplyForLeave_WhenValidRequest_ShouldReturnCreated() throws Exception {
        // Given
        when(leaveService.applyForLeave(any(LeaveApplicationRequest.class))).thenReturn(leaveRequestResponse);

        // When & Then
        mockMvc.perform(post("/api/leave-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(leaveApplicationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").value("LR-12345678"))
                .andExpect(jsonPath("$.leaveType").value("VACATION"))
                .andExpect(jsonPath("$.startDate").value("2024-04-15"))
                .andExpect(jsonPath("$.endDate").value("2024-04-17"))
                .andExpect(jsonPath("$.totalDays").value(3.0))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.employee.employeeId").value("EMP001"))
                .andExpect(jsonPath("$.employee.name").value("Alice Smith"));

        verify(leaveService).applyForLeave(any(LeaveApplicationRequest.class));
    }

    @Test
    void testApplyForLeave_WhenInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given - Invalid request with missing required fields
        LeaveApplicationRequest invalidRequest = new LeaveApplicationRequest();
        invalidRequest.setEmployeeId(""); // Empty employee ID
        invalidRequest.setLeaveType(null); // Null leave type
        invalidRequest.setStartDate(null); // Null start date

        // When & Then
        mockMvc.perform(post("/api/leave-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(leaveService, never()).applyForLeave(any(LeaveApplicationRequest.class));
    }

    @Test
    void testApplyForLeave_WhenServiceThrowsException_ShouldReturnBadRequest() throws Exception {
        // Given
        when(leaveService.applyForLeave(any(LeaveApplicationRequest.class)))
            .thenThrow(new IllegalArgumentException("Insufficient leave balance"));

        // When & Then
        mockMvc.perform(post("/api/leave-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(leaveApplicationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient leave balance"));

        verify(leaveService).applyForLeave(any(LeaveApplicationRequest.class));
    }

    @Test
    void testApplyForEmergencyLeave_ShouldReturnAutoApproved() throws Exception {
        // Given - Emergency leave request
        leaveApplicationRequest.setLeaveType(LeaveType.EMERGENCY);
        leaveApplicationRequest.setStartDate(LocalDate.now());
        leaveApplicationRequest.setEndDate(LocalDate.now().plusDays(1));
        leaveApplicationRequest.setIsEmergencyLeave(true);
        leaveApplicationRequest.setReason("Family emergency");

        LeaveRequestResponse emergencyResponse = new LeaveRequestResponse();
        emergencyResponse.setRequestId("LR-EMERGENCY");
        emergencyResponse.setStatus(LeaveStatus.AUTO_APPROVED);
        emergencyResponse.setIsEmergencyLeave(true);
        emergencyResponse.setLeaveType(LeaveType.EMERGENCY);

        when(leaveService.applyForLeave(any(LeaveApplicationRequest.class))).thenReturn(emergencyResponse);

        // When & Then
        mockMvc.perform(post("/api/leave-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(leaveApplicationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AUTO_APPROVED"))
                .andExpect(jsonPath("$.isEmergencyLeave").value(true))
                .andExpect(jsonPath("$.leaveType").value("EMERGENCY"));

        verify(leaveService).applyForLeave(any(LeaveApplicationRequest.class));
    }

    @Test
    void testGetLeaveRequest_WhenRequestExists_ShouldReturnRequest() throws Exception {
        // Given
        when(leaveService.getLeaveRequest("LR-12345678")).thenReturn(leaveRequestResponse);

        // When & Then
        mockMvc.perform(get("/api/leave-requests/LR-12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("LR-12345678"))
                .andExpect(jsonPath("$.leaveType").value("VACATION"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(leaveService).getLeaveRequest("LR-12345678");
    }

    @Test
    void testGetLeaveRequest_WhenRequestNotFound_ShouldReturnNotFound() throws Exception {
        // Given
        when(leaveService.getLeaveRequest("NONEXISTENT"))
            .thenThrow(new IllegalArgumentException("Leave request not found: NONEXISTENT"));

        // When & Then
        mockMvc.perform(get("/api/leave-requests/NONEXISTENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Leave request not found: NONEXISTENT"));

        verify(leaveService).getLeaveRequest("NONEXISTENT");
    }

    @Test
    void testGetEmployeeLeaveHistory_ShouldReturnEmployeeRequests() throws Exception {
        // Given
        List<LeaveRequestResponse> leaveHistory = Arrays.asList(leaveRequestResponse);
        when(leaveService.getEmployeeLeaveHistory("EMP001")).thenReturn(leaveHistory);

        // When & Then
        mockMvc.perform(get("/api/leave-requests/employee/EMP001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].requestId").value("LR-12345678"))
                .andExpect(jsonPath("$[0].employee.employeeId").value("EMP001"));

        verify(leaveService).getEmployeeLeaveHistory("EMP001");
    }

    @Test
    void testGetEmployeeLeaveHistory_WithPagination_ShouldReturnPagedResults() throws Exception {
        // Given
        List<LeaveRequestResponse> leaveHistory = Arrays.asList(leaveRequestResponse);
        Page<LeaveRequestResponse> leavePage = new PageImpl<>(leaveHistory, PageRequest.of(0, 10), 1);
        when(leaveService.getEmployeeLeaveHistory(eq("EMP001"), any(Pageable.class))).thenReturn(leavePage);

        // When & Then
        mockMvc.perform(get("/api/leave-requests/employee/EMP001")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].requestId").value("LR-12345678"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(leaveService).getEmployeeLeaveHistory(eq("EMP001"), any(Pageable.class));
    }

    @Test
    void testGetEmployeeLeaveHistory_WhenEmployeeNotFound_ShouldReturnBadRequest() throws Exception {
        // Given
        when(leaveService.getEmployeeLeaveHistory("NONEXISTENT"))
            .thenThrow(new IllegalArgumentException("Employee not found: NONEXISTENT"));

        // When & Then
        mockMvc.perform(get("/api/leave-requests/employee/NONEXISTENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Employee not found: NONEXISTENT"));

        verify(leaveService).getEmployeeLeaveHistory("NONEXISTENT");
    }

    @Test
    void testGetPendingRequestsForManager_ShouldReturnPendingRequests() throws Exception {
        // Given
        List<LeaveRequestResponse> pendingRequests = Arrays.asList(leaveRequestResponse);
        when(leaveService.getPendingRequestsForManager("MGR001")).thenReturn(pendingRequests);

        // When & Then
        mockMvc.perform(get("/api/leave-requests/pending")
                .param("managerId", "MGR001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].employee.employeeId").value("EMP001"));

        verify(leaveService).getPendingRequestsForManager("MGR001");
    }

    @Test
    void testGetPendingRequestsForManager_WithPagination_ShouldReturnPagedResults() throws Exception {
        // Given
        List<LeaveRequestResponse> pendingRequests = Arrays.asList(leaveRequestResponse);
        Page<LeaveRequestResponse> pendingPage = new PageImpl<>(pendingRequests, PageRequest.of(0, 10), 1);
        when(leaveService.getPendingRequestsForManager(eq("MGR001"), any(Pageable.class))).thenReturn(pendingPage);

        // When & Then
        mockMvc.perform(get("/api/leave-requests/pending")
                .param("managerId", "MGR001")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(leaveService).getPendingRequestsForManager(eq("MGR001"), any(Pageable.class));
    }

    @Test
    void testGetPendingRequestsForManager_WhenManagerNotFound_ShouldReturnBadRequest() throws Exception {
        // Given
        when(leaveService.getPendingRequestsForManager("NONEXISTENT"))
            .thenThrow(new IllegalArgumentException("Manager not found: NONEXISTENT"));

        // When & Then
        mockMvc.perform(get("/api/leave-requests/pending")
                .param("managerId", "NONEXISTENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Manager not found: NONEXISTENT"));

        verify(leaveService).getPendingRequestsForManager("NONEXISTENT");
    }

    @Test
    void testApproveLeaveRequest_WhenValidRequest_ShouldReturnApprovedRequest() throws Exception {
        // Given
        LeaveRequestResponse approvedResponse = new LeaveRequestResponse();
        approvedResponse.setRequestId("LR-12345678");
        approvedResponse.setStatus(LeaveStatus.APPROVED);
        approvedResponse.setApprovedBy(new LeaveRequestResponse.ApproverInfo(
            "MGR001", "John Manager", "john.manager@company.com"));

        when(leaveService.approveLeaveRequest(eq("LR-12345678"), any(ApprovalRequest.class)))
            .thenReturn(approvedResponse);

        // When & Then
        mockMvc.perform(put("/api/leave-requests/LR-12345678/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("LR-12345678"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedBy.employeeId").value("MGR001"));

        verify(leaveService).approveLeaveRequest(eq("LR-12345678"), any(ApprovalRequest.class));
    }

    @Test
    void testApproveLeaveRequest_WhenInvalidApprover_ShouldReturnBadRequest() throws Exception {
        // Given
        when(leaveService.approveLeaveRequest(eq("LR-12345678"), any(ApprovalRequest.class)))
            .thenThrow(new IllegalArgumentException("Authorization failed: You are not authorized to approve this request"));

        // When & Then
        mockMvc.perform(put("/api/leave-requests/LR-12345678/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Authorization failed: You are not authorized to approve this request"));

        verify(leaveService).approveLeaveRequest(eq("LR-12345678"), any(ApprovalRequest.class));
    }

    @Test
    void testApproveLeaveRequest_WhenRequestNotPending_ShouldReturnBadRequest() throws Exception {
        // Given
        when(leaveService.approveLeaveRequest(eq("LR-12345678"), any(ApprovalRequest.class)))
            .thenThrow(new IllegalArgumentException("Cannot process request: Leave request is not in pending status"));

        // When & Then
        mockMvc.perform(put("/api/leave-requests/LR-12345678/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot process request: Leave request is not in pending status"));

        verify(leaveService).approveLeaveRequest(eq("LR-12345678"), any(ApprovalRequest.class));
    }

    @Test
    void testRejectLeaveRequest_WhenValidRequest_ShouldReturnRejectedRequest() throws Exception {
        // Given
        LeaveRequestResponse rejectedResponse = new LeaveRequestResponse();
        rejectedResponse.setRequestId("LR-12345678");
        rejectedResponse.setStatus(LeaveStatus.REJECTED);
        rejectedResponse.setRejectionReason("Insufficient staffing during requested period");
        rejectedResponse.setApprovedBy(new LeaveRequestResponse.ApproverInfo(
            "MGR001", "John Manager", "john.manager@company.com"));

        when(leaveService.rejectLeaveRequest(eq("LR-12345678"), any(RejectionRequest.class)))
            .thenReturn(rejectedResponse);

        // When & Then
        mockMvc.perform(put("/api/leave-requests/LR-12345678/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rejectionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("LR-12345678"))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Insufficient staffing during requested period"))
                .andExpect(jsonPath("$.approvedBy.employeeId").value("MGR001"));

        verify(leaveService).rejectLeaveRequest(eq("LR-12345678"), any(RejectionRequest.class));
    }

    @Test
    void testRejectLeaveRequest_WithoutRejectionReason_ShouldReturnBadRequest() throws Exception {
        // Given - Invalid request without rejection reason
        RejectionRequest invalidRequest = new RejectionRequest();
        invalidRequest.setApproverId("MGR001");
        invalidRequest.setRejectionReason(""); // Empty rejection reason

        // When & Then
        mockMvc.perform(put("/api/leave-requests/LR-12345678/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(leaveService, never()).rejectLeaveRequest(anyString(), any(RejectionRequest.class));
    }

    @Test
    void testCancelLeaveRequest_WhenValidRequest_ShouldReturnCancelledRequest() throws Exception {
        // Given
        LeaveRequestResponse cancelledResponse = new LeaveRequestResponse();
        cancelledResponse.setRequestId("LR-12345678");
        cancelledResponse.setStatus(LeaveStatus.CANCELLED);

        when(leaveService.cancelLeaveRequest("LR-12345678", "EMP001")).thenReturn(cancelledResponse);

        // When & Then
        mockMvc.perform(put("/api/leave-requests/LR-12345678/cancel")
                .param("employeeId", "EMP001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("LR-12345678"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(leaveService).cancelLeaveRequest("LR-12345678", "EMP001");
    }

    @Test
    void testCancelLeaveRequest_WhenNotOwner_ShouldReturnBadRequest() throws Exception {
        // Given
        when(leaveService.cancelLeaveRequest("LR-12345678", "EMP002"))
            .thenThrow(new IllegalArgumentException("You can only cancel your own leave requests"));

        // When & Then
        mockMvc.perform(put("/api/leave-requests/LR-12345678/cancel")
                .param("employeeId", "EMP002"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You can only cancel your own leave requests"));

        verify(leaveService).cancelLeaveRequest("LR-12345678", "EMP002");
    }

    @Test
    void testGetLeaveRequestsByStatus_ShouldReturnFilteredRequests() throws Exception {
        // Given
        List<LeaveRequestResponse> approvedRequests = Arrays.asList(leaveRequestResponse);
        when(leaveService.getLeaveRequestsByStatus(LeaveStatus.APPROVED)).thenReturn(approvedRequests);

        // When & Then
        mockMvc.perform(get("/api/leave-requests/status/APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].requestId").value("LR-12345678"));

        verify(leaveService).getLeaveRequestsByStatus(LeaveStatus.APPROVED);
    }

    @Test
    void testGetLeaveRequestsByStatus_WithPagination_ShouldReturnPagedResults() throws Exception {
        // Given
        List<LeaveRequestResponse> pendingRequests = Arrays.asList(leaveRequestResponse);
        Page<LeaveRequestResponse> pendingPage = new PageImpl<>(pendingRequests, PageRequest.of(0, 10), 1);
        when(leaveService.getLeaveRequestsByStatus(eq(LeaveStatus.PENDING), any(Pageable.class)))
            .thenReturn(pendingPage);

        // When & Then
        mockMvc.perform(get("/api/leave-requests/status/PENDING")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(leaveService).getLeaveRequestsByStatus(eq(LeaveStatus.PENDING), any(Pageable.class));
    }

    @Test
    void testGetLeaveRequestsInDateRange_ShouldReturnRequestsInRange() throws Exception {
        // Given
        List<LeaveRequestResponse> requestsInRange = Arrays.asList(leaveRequestResponse);
        when(leaveService.getLeaveRequestsInDateRange(LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 30)))
            .thenReturn(requestsInRange);

        // When & Then
        mockMvc.perform(get("/api/leave-requests/date-range")
                .param("startDate", "2024-04-01")
                .param("endDate", "2024-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].startDate").value("2024-04-15"))
                .andExpect(jsonPath("$[0].endDate").value("2024-04-17"));

        verify(leaveService).getLeaveRequestsInDateRange(LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 30));
    }

    @Test
    void testGetLeaveRequestsInDateRange_WithInvalidRange_ShouldReturnBadRequest() throws Exception {
        // Given
        when(leaveService.getLeaveRequestsInDateRange(LocalDate.of(2024, 4, 30), LocalDate.of(2024, 4, 1)))
            .thenThrow(new IllegalArgumentException("Invalid date range"));

        // When & Then
        mockMvc.perform(get("/api/leave-requests/date-range")
                .param("startDate", "2024-04-30")
                .param("endDate", "2024-04-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid date range"));

        verify(leaveService).getLeaveRequestsInDateRange(LocalDate.of(2024, 4, 30), LocalDate.of(2024, 4, 1));
    }

    @Test
    void testGetEmergencyLeaveRequests_ShouldReturnEmergencyRequests() throws Exception {
        // Given
        LeaveRequestResponse emergencyResponse = new LeaveRequestResponse();
        emergencyResponse.setRequestId("LR-EMERGENCY");
        emergencyResponse.setIsEmergencyLeave(true);
        emergencyResponse.setLeaveType(LeaveType.EMERGENCY);
        
        List<LeaveRequestResponse> emergencyRequests = Arrays.asList(emergencyResponse);
        when(leaveService.getEmergencyLeaveRequests()).thenReturn(emergencyRequests);

        // When & Then
        mockMvc.perform(get("/api/leave-requests/emergency"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].isEmergencyLeave").value(true))
                .andExpect(jsonPath("$[0].leaveType").value("EMERGENCY"));

        verify(leaveService).getEmergencyLeaveRequests();
    }

    @Test
    void testGetUpcomingLeaveRequests_ShouldReturnUpcomingRequests() throws Exception {
        // Given
        List<LeaveRequestResponse> upcomingRequests = Arrays.asList(leaveRequestResponse);
        when(leaveService.getUpcomingLeaveRequests(7)).thenReturn(upcomingRequests);

        // When & Then
        mockMvc.perform(get("/api/leave-requests/upcoming")
                .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].requestId").value("LR-12345678"));

        verify(leaveService).getUpcomingLeaveRequests(7);
    }

    @Test
    void testGetActiveLeaveRequests_ShouldReturnActiveRequests() throws Exception {
        // Given
        List<LeaveRequestResponse> activeRequests = Arrays.asList(leaveRequestResponse);
        when(leaveService.getActiveLeaveRequests()).thenReturn(activeRequests);

        // When & Then
        mockMvc.perform(get("/api/leave-requests/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].requestId").value("LR-12345678"));

        verify(leaveService).getActiveLeaveRequests();
    }

    // Edge case and validation tests
    @Test
    void testApplyForLeave_WithBackdatedRequest_ShouldIncludeBackdatedFlag() throws Exception {
        // Given - Backdated request
        leaveApplicationRequest.setStartDate(LocalDate.now().minusDays(5));
        leaveApplicationRequest.setEndDate(LocalDate.now().minusDays(3));
        leaveApplicationRequest.setBackdatedJustification("Medical emergency - unable to apply earlier");

        LeaveRequestResponse backdatedResponse = new LeaveRequestResponse();
        backdatedResponse.setRequestId("LR-BACKDATED");
        backdatedResponse.setIsBackdated(true);
        backdatedResponse.setStatus(LeaveStatus.PENDING);

        when(leaveService.applyForLeave(any(LeaveApplicationRequest.class))).thenReturn(backdatedResponse);

        // When & Then
        mockMvc.perform(post("/api/leave-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(leaveApplicationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isBackdated").value(true))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(leaveService).applyForLeave(any(LeaveApplicationRequest.class));
    }

    @Test
    void testGetPendingRequestsForManager_WithoutManagerId_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/leave-requests/pending"))
                .andExpect(status().isBadRequest());

        verify(leaveService, never()).getPendingRequestsForManager(anyString());
    }

    @Test
    void testGetLeaveRequestsInDateRange_WithMissingDates_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/leave-requests/date-range")
                .param("startDate", "2024-04-01"))
                // Missing endDate parameter
                .andExpect(status().isBadRequest());

        verify(leaveService, never()).getLeaveRequestsInDateRange(any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testGetUpcomingLeaveRequests_WithDefaultDays_ShouldUseDefault() throws Exception {
        // Given
        List<LeaveRequestResponse> upcomingRequests = Collections.emptyList();
        when(leaveService.getUpcomingLeaveRequests(30)).thenReturn(upcomingRequests); // Default 30 days

        // When & Then
        mockMvc.perform(get("/api/leave-requests/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(leaveService).getUpcomingLeaveRequests(30);
    }
}