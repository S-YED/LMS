package com.company.leavemanagementsystem.integration;

import com.company.leavemanagementsystem.dto.*;
import com.company.leavemanagementsystem.entity.LeaveDuration;
import com.company.leavemanagementsystem.entity.LeaveStatus;
import com.company.leavemanagementsystem.entity.LeaveType;
// ObjectMapper import removed - was unused
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for complete leave application workflows
 */
@DisplayName("Leave Application Workflow Integration Tests")
public class LeaveApplicationWorkflowIntegrationTest extends BaseIntegrationTest {

    // ObjectMapper removed - was unused

    @Test
    @DisplayName("Complete leave application workflow - Employee applies, Manager approves")
    void testCompleteLeaveApplicationWorkflow() {
        // Step 1: Create manager
        CreateEmployeeRequest managerRequest = new CreateEmployeeRequest();
        managerRequest.setEmployeeId("MGR001");
        managerRequest.setName("John Manager");
        managerRequest.setEmail("john.manager@company.com");
        managerRequest.setDepartment("Engineering");
        managerRequest.setJoiningDate(LocalDate.of(2020, 1, 1));

        ResponseEntity<EmployeeResponse> managerResponse = restTemplate.postForEntity(
                baseUrl + "/employees", managerRequest, EmployeeResponse.class);
        assertThat(managerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        EmployeeResponse manager = managerResponse.getBody();
        assertThat(manager).isNotNull();
        assertThat(manager.getEmployeeId()).isEqualTo("MGR001");

        // Step 2: Create employee with manager
        CreateEmployeeRequest employeeRequest = new CreateEmployeeRequest();
        employeeRequest.setEmployeeId("EMP001");
        employeeRequest.setName("Jane Employee");
        employeeRequest.setEmail("jane.employee@company.com");
        employeeRequest.setDepartment("Engineering");
        employeeRequest.setJoiningDate(LocalDate.of(2023, 1, 1));
        employeeRequest.setManagerId("MGR001");

        ResponseEntity<EmployeeResponse> employeeResponse = restTemplate.postForEntity(
                baseUrl + "/employees", employeeRequest, EmployeeResponse.class);
        assertThat(employeeResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        EmployeeResponse employee = employeeResponse.getBody();
        assertThat(employee).isNotNull();
        assertThat(employee.getEmployeeId()).isEqualTo("EMP001");
        assertThat(employee.getManager().getEmployeeId()).isEqualTo("MGR001");

        // Step 3: Initialize leave balance for employee
        InitializeBalanceRequest balanceRequest = new InitializeBalanceRequest();
        balanceRequest.setYear(2024);

        ResponseEntity<List> balanceResponse = restTemplate.postForEntity(
                baseUrl + "/leave-balances/employee/EMP001/initialize", 
                balanceRequest, List.class);
        @SuppressWarnings("unchecked")
        List<Object> balanceList = (List<Object>) balanceResponse.getBody();
        assertThat(balanceResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Step 4: Check initial leave balance
        ResponseEntity<List> initialBalanceResponse = restTemplate.getForEntity(
                baseUrl + "/leave-balances/employee/EMP001", List.class);
        assertThat(initialBalanceResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Object> initialBalanceList = (List<Object>) initialBalanceResponse.getBody();
        assertThat(initialBalanceResponse.getBody()).isNotEmpty();

        // Step 5: Apply for leave
        LeaveApplicationRequest leaveRequest = new LeaveApplicationRequest();
        leaveRequest.setEmployeeId("EMP001");
        leaveRequest.setLeaveType(LeaveType.VACATION);
        leaveRequest.setStartDate(LocalDate.now().plusDays(10));
        leaveRequest.setEndDate(LocalDate.now().plusDays(12));
        leaveRequest.setDuration(LeaveDuration.FULL_DAY);
        leaveRequest.setReason("Family vacation");
        leaveRequest.setComments("Planning a trip with family");

        ResponseEntity<LeaveRequestResponse> leaveResponse = restTemplate.postForEntity(
                baseUrl + "/leave-requests", leaveRequest, LeaveRequestResponse.class);
        assertThat(leaveResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        LeaveRequestResponse leaveRequestResponse = leaveResponse.getBody();
        assertThat(leaveRequestResponse).isNotNull();
        assertThat(leaveRequestResponse.getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(leaveRequestResponse.getEmployee().getEmployeeId()).isEqualTo("EMP001");
        assertThat(leaveRequestResponse.getTotalDays()).isEqualTo(3.0);

        String requestId = leaveRequestResponse.getRequestId();

        // Step 6: Manager checks pending requests
        ResponseEntity<List> pendingResponse = restTemplate.getForEntity(
                baseUrl + "/leave-requests/pending?managerId=MGR001", List.class);
        assertThat(pendingResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Object> pendingList = (List<Object>) pendingResponse.getBody();
        assertThat(pendingResponse.getBody()).isNotEmpty();

        // Step 7: Manager approves the leave request
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setApproverId("MGR001");
        approvalRequest.setComments("Approved for family vacation");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ApprovalRequest> approvalEntity = new HttpEntity<>(approvalRequest, headers);

        ResponseEntity<LeaveRequestResponse> approvalResponse = restTemplate.exchange(
                baseUrl + "/leave-requests/" + requestId + "/approve",
                HttpMethod.PUT, approvalEntity, LeaveRequestResponse.class);
        assertThat(approvalResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        LeaveRequestResponse approvedRequest = approvalResponse.getBody();
        assertThat(approvedRequest).isNotNull();
        assertThat(approvedRequest.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(approvedRequest.getApprovedBy().getEmployeeId()).isEqualTo("MGR001");

        // Step 8: Verify leave balance is updated
        ResponseEntity<List> updatedBalanceResponse = restTemplate.getForEntity(
                baseUrl + "/leave-balances/employee/EMP001", List.class);
        assertThat(updatedBalanceResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Object> updatedBalanceList = (List<Object>) updatedBalanceResponse.getBody();
        // Balance should be reduced by 3 days

        // Step 9: Check employee's leave history
        ResponseEntity<List> historyResponse = restTemplate.getForEntity(
                baseUrl + "/leave-requests/employee/EMP001", List.class);
        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Object> historyList = (List<Object>) historyResponse.getBody();
        assertThat(historyResponse.getBody()).isNotEmpty();
    }

    @Test
    @DisplayName("Leave rejection workflow")
    void testLeaveRejectionWorkflow() {
        // Create manager and employee (similar setup)
        setupManagerAndEmployee("MGR002", "EMP002");

        // Apply for leave
        LeaveApplicationRequest leaveRequest = new LeaveApplicationRequest();
        leaveRequest.setEmployeeId("EMP002");
        leaveRequest.setLeaveType(LeaveType.PERSONAL);
        leaveRequest.setStartDate(LocalDate.now().plusDays(5));
        leaveRequest.setEndDate(LocalDate.now().plusDays(7));
        leaveRequest.setDuration(LeaveDuration.FULL_DAY);
        leaveRequest.setReason("Personal work");

        ResponseEntity<LeaveRequestResponse> leaveResponse = restTemplate.postForEntity(
                baseUrl + "/leave-requests", leaveRequest, LeaveRequestResponse.class);
        assertThat(leaveResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String requestId = leaveResponse.getBody().getRequestId();

        // Manager rejects the leave request
        RejectionRequest rejectionRequest = new RejectionRequest();
        rejectionRequest.setApproverId("MGR002");
        rejectionRequest.setRejectionReason("Insufficient staffing during this period");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RejectionRequest> rejectionEntity = new HttpEntity<>(rejectionRequest, headers);

        ResponseEntity<LeaveRequestResponse> rejectionResponse = restTemplate.exchange(
                baseUrl + "/leave-requests/" + requestId + "/reject",
                HttpMethod.PUT, rejectionEntity, LeaveRequestResponse.class);
        assertThat(rejectionResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        LeaveRequestResponse rejectedRequest = rejectionResponse.getBody();
        assertThat(rejectedRequest).isNotNull();
        assertThat(rejectedRequest.getStatus()).isEqualTo(LeaveStatus.REJECTED);
        assertThat(rejectedRequest.getRejectionReason()).isEqualTo("Insufficient staffing during this period");
    }

    @Test
    @DisplayName("Emergency leave auto-approval workflow")
    void testEmergencyLeaveAutoApproval() {
        setupManagerAndEmployee("MGR003", "EMP003");

        // Apply for emergency leave (same day)
        LeaveApplicationRequest emergencyRequest = new LeaveApplicationRequest();
        emergencyRequest.setEmployeeId("EMP003");
        emergencyRequest.setLeaveType(LeaveType.EMERGENCY);
        emergencyRequest.setStartDate(LocalDate.now());
        emergencyRequest.setEndDate(LocalDate.now().plusDays(1));
        emergencyRequest.setDuration(LeaveDuration.FULL_DAY);
        emergencyRequest.setReason("Medical emergency");
        emergencyRequest.setIsEmergencyLeave(true);

        ResponseEntity<LeaveRequestResponse> emergencyResponse = restTemplate.postForEntity(
                baseUrl + "/leave-requests", emergencyRequest, LeaveRequestResponse.class);
        assertThat(emergencyResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        LeaveRequestResponse emergencyLeave = emergencyResponse.getBody();
        assertThat(emergencyLeave).isNotNull();
        // Emergency leave should be auto-approved for 2 days or less
        assertThat(emergencyLeave.getStatus()).isEqualTo(LeaveStatus.AUTO_APPROVED);
    }

    @Test
    @DisplayName("Overlapping leave request validation")
    void testOverlappingLeaveValidation() {
        setupManagerAndEmployee("MGR004", "EMP004");

        // Apply for first leave
        LeaveApplicationRequest firstRequest = new LeaveApplicationRequest();
        firstRequest.setEmployeeId("EMP004");
        firstRequest.setLeaveType(LeaveType.VACATION);
        firstRequest.setStartDate(LocalDate.now().plusDays(15));
        firstRequest.setEndDate(LocalDate.now().plusDays(17));
        firstRequest.setDuration(LeaveDuration.FULL_DAY);
        firstRequest.setReason("First vacation");

        ResponseEntity<LeaveRequestResponse> firstResponse = restTemplate.postForEntity(
                baseUrl + "/leave-requests", firstRequest, LeaveRequestResponse.class);
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Approve first leave
        String firstRequestId = firstResponse.getBody().getRequestId();
        ApprovalRequest approval = new ApprovalRequest();
        approval.setApproverId("MGR004");
        approval.setComments("Approved");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ApprovalRequest> approvalEntity = new HttpEntity<>(approval, headers);

        restTemplate.exchange(
                baseUrl + "/leave-requests/" + firstRequestId + "/approve",
                HttpMethod.PUT, approvalEntity, LeaveRequestResponse.class);

        // Try to apply for overlapping leave
        LeaveApplicationRequest overlappingRequest = new LeaveApplicationRequest();
        overlappingRequest.setEmployeeId("EMP004");
        overlappingRequest.setLeaveType(LeaveType.PERSONAL);
        overlappingRequest.setStartDate(LocalDate.now().plusDays(16));
        overlappingRequest.setEndDate(LocalDate.now().plusDays(18));
        overlappingRequest.setDuration(LeaveDuration.FULL_DAY);
        overlappingRequest.setReason("Overlapping leave");

        ResponseEntity<String> overlappingResponse = restTemplate.postForEntity(
                baseUrl + "/leave-requests", overlappingRequest, String.class);
        assertThat(overlappingResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Insufficient leave balance validation")
    void testInsufficientLeaveBalanceValidation() {
        setupManagerAndEmployee("MGR005", "EMP005");

        // Try to apply for more leave than available balance
        LeaveApplicationRequest excessiveRequest = new LeaveApplicationRequest();
        excessiveRequest.setEmployeeId("EMP005");
        excessiveRequest.setLeaveType(LeaveType.VACATION);
        excessiveRequest.setStartDate(LocalDate.now().plusDays(20));
        excessiveRequest.setEndDate(LocalDate.now().plusDays(50)); // 31 days, more than default 20
        excessiveRequest.setDuration(LeaveDuration.FULL_DAY);
        excessiveRequest.setReason("Long vacation");

        ResponseEntity<String> excessiveResponse = restTemplate.postForEntity(
                baseUrl + "/leave-requests", excessiveRequest, String.class);
        assertThat(excessiveResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private void setupManagerAndEmployee(String managerId, String employeeId) {
        // Create manager
        CreateEmployeeRequest managerRequest = new CreateEmployeeRequest();
        managerRequest.setEmployeeId(managerId);
        managerRequest.setName("Manager " + managerId);
        managerRequest.setEmail(managerId.toLowerCase() + "@company.com");
        managerRequest.setDepartment("Engineering");
        managerRequest.setJoiningDate(LocalDate.of(2020, 1, 1));

        restTemplate.postForEntity(baseUrl + "/employees", managerRequest, EmployeeResponse.class);

        // Create employee
        CreateEmployeeRequest employeeRequest = new CreateEmployeeRequest();
        employeeRequest.setEmployeeId(employeeId);
        employeeRequest.setName("Employee " + employeeId);
        employeeRequest.setEmail(employeeId.toLowerCase() + "@company.com");
        employeeRequest.setDepartment("Engineering");
        employeeRequest.setJoiningDate(LocalDate.of(2023, 1, 1));
        employeeRequest.setManagerId(managerId);

        restTemplate.postForEntity(baseUrl + "/employees", employeeRequest, EmployeeResponse.class);

        // Initialize leave balance
        InitializeBalanceRequest balanceRequest = new InitializeBalanceRequest();
        balanceRequest.setYear(2024);
        restTemplate.postForEntity(
                baseUrl + "/leave-balances/employee/" + employeeId + "/initialize", 
                balanceRequest, List.class);
    }
}