package com.company.leavemanagementsystem.integration;

import com.company.leavemanagementsystem.dto.*;
import com.company.leavemanagementsystem.entity.LeaveDuration;
import com.company.leavemanagementsystem.entity.LeaveStatus;
import com.company.leavemanagementsystem.entity.LeaveType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for approval processes with multiple users and edge cases
 */
@DisplayName("Approval Process Integration Tests")
public class ApprovalProcessIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Manager delegation when primary manager is unavailable")
    void testManagerDelegationWorkflow() {
        // Create organizational hierarchy: CEO -> Director -> Manager -> Employee
        setupOrganizationalHierarchy();

        // Apply for leave as employee
        LeaveApplicationRequest leaveRequest = new LeaveApplicationRequest();
        leaveRequest.setEmployeeId("EMP_HIER");
        leaveRequest.setLeaveType(LeaveType.VACATION);
        leaveRequest.setStartDate(LocalDate.now().plusDays(10));
        leaveRequest.setEndDate(LocalDate.now().plusDays(12));
        leaveRequest.setDuration(LeaveDuration.FULL_DAY);
        leaveRequest.setReason("Vacation request");

        ResponseEntity<LeaveRequestResponse> leaveResponse = restTemplate.postForEntity(
                baseUrl + "/leave-requests", leaveRequest, LeaveRequestResponse.class);
        assertThat(leaveResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        LeaveRequestResponse leaveResponseBody = leaveResponse.getBody();
        assertThat(leaveResponseBody).isNotNull();
        String requestId = leaveResponseBody.getRequestId();
        assertThat(requestId).isNotNull();

        // Primary manager (MGR_HIER) is on leave, so director should be able to approve
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setApproverId("DIR_HIER");
        approvalRequest.setComments("Approved by director due to manager unavailability");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ApprovalRequest> approvalEntity = new HttpEntity<>(approvalRequest, headers);

        ResponseEntity<LeaveRequestResponse> approvalResponse = restTemplate.exchange(
                baseUrl + "/leave-requests/" + requestId + "/approve",
                HttpMethod.PUT, approvalEntity, LeaveRequestResponse.class);
        assertThat(approvalResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        LeaveRequestResponse responseBody = approvalResponse.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(responseBody.getApprovedBy()).isNotNull();
        assertThat(responseBody.getApprovedBy().getEmployeeId()).isEqualTo("DIR_HIER");
    }

    @Test
    @DisplayName("Multiple pending requests for same employee")
    void testMultiplePendingRequestsWorkflow() {
        setupManagerAndEmployee("MGR_MULTI", "EMP_MULTI");

        // Apply for multiple leave requests
        String[] requestIds = new String[3];
        
        for (int i = 0; i < 3; i++) {
            LeaveApplicationRequest leaveRequest = new LeaveApplicationRequest();
            leaveRequest.setEmployeeId("EMP_MULTI");
            leaveRequest.setLeaveType(LeaveType.VACATION);
            leaveRequest.setStartDate(LocalDate.now().plusDays(10 + (i * 10)));
            leaveRequest.setEndDate(LocalDate.now().plusDays(12 + (i * 10)));
            leaveRequest.setDuration(LeaveDuration.FULL_DAY);
            leaveRequest.setReason("Vacation request " + (i + 1));

            ResponseEntity<LeaveRequestResponse> response = restTemplate.postForEntity(
                    baseUrl + "/leave-requests", leaveRequest, LeaveRequestResponse.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            
            LeaveRequestResponse responseBody = response.getBody();
            assertThat(responseBody).isNotNull();
            requestIds[i] = responseBody.getRequestId();
        }

        // Check that manager sees all pending requests
        ResponseEntity<List> pendingResponse = restTemplate.getForEntity(
                baseUrl + "/leave-requests/pending?managerId=MGR_MULTI", List.class);
        assertThat(pendingResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Object> pendingList = (List<Object>) pendingResponse.getBody();
        
        List<?> pendingBody = pendingResponse.getBody();
        assertThat(pendingBody).isNotNull();
        assertThat(pendingBody).hasSize(3);

        // Approve first request
        approveRequest(requestIds[0], "MGR_MULTI", "Approved first request");

        // Reject second request
        rejectRequest(requestIds[1], "MGR_MULTI", "Rejected due to workload");

        // Leave third request pending

        // Verify final states
        ResponseEntity<List> finalPendingResponse = restTemplate.getForEntity(
                baseUrl + "/leave-requests/pending?managerId=MGR_MULTI", List.class);
        assertThat(finalPendingResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Object> finalPendingList = (List<Object>) finalPendingResponse.getBody();
        
        List<?> finalPendingBody = finalPendingResponse.getBody();
        assertThat(finalPendingBody).isNotNull();
        assertThat(finalPendingBody).hasSize(1); // Only one pending left

        // Check employee's leave history shows all requests
        ResponseEntity<List> historyResponse = restTemplate.getForEntity(
                baseUrl + "/leave-requests/employee/EMP_MULTI", List.class);
        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Object> historyList = (List<Object>) historyResponse.getBody();
        
        List<?> historyBody = historyResponse.getBody();
        assertThat(historyBody).isNotNull();
        assertThat(historyBody).hasSize(3);
    }

    @Test
    @DisplayName("Concurrent approval attempts")
    void testConcurrentApprovalAttempts() throws Exception {
        setupManagerAndEmployee("MGR_CONCURRENT", "EMP_CONCURRENT");

        // Apply for leave
        LeaveApplicationRequest leaveRequest = new LeaveApplicationRequest();
        leaveRequest.setEmployeeId("EMP_CONCURRENT");
        leaveRequest.setLeaveType(LeaveType.SICK);
        leaveRequest.setStartDate(LocalDate.now().plusDays(5));
        leaveRequest.setEndDate(LocalDate.now().plusDays(6));
        leaveRequest.setDuration(LeaveDuration.FULL_DAY);
        leaveRequest.setReason("Sick leave");

        ResponseEntity<LeaveRequestResponse> leaveResponse = restTemplate.postForEntity(
                baseUrl + "/leave-requests", leaveRequest, LeaveRequestResponse.class);
        assertThat(leaveResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        LeaveRequestResponse leaveResponseBody = leaveResponse.getBody();
        assertThat(leaveResponseBody).isNotNull();
        String requestId = leaveResponseBody.getRequestId();

        // Simulate concurrent approval attempts
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        CompletableFuture<ResponseEntity<LeaveRequestResponse>> approval1 = CompletableFuture.supplyAsync(() -> {
            ApprovalRequest approvalRequest = new ApprovalRequest();
            approvalRequest.setApproverId("MGR_CONCURRENT");
            approvalRequest.setComments("Approved by manager 1");
            return attemptApproval(requestId, approvalRequest);
        }, executor);

        CompletableFuture<ResponseEntity<LeaveRequestResponse>> approval2 = CompletableFuture.supplyAsync(() -> {
            ApprovalRequest approvalRequest = new ApprovalRequest();
            approvalRequest.setApproverId("MGR_CONCURRENT");
            approvalRequest.setComments("Approved by manager 2");
            return attemptApproval(requestId, approvalRequest);
        }, executor);

        CompletableFuture<ResponseEntity<String>> rejection = CompletableFuture.supplyAsync(() -> {
            RejectionRequest rejectionRequest = new RejectionRequest();
            rejectionRequest.setApproverId("MGR_CONCURRENT");
            rejectionRequest.setRejectionReason("Rejected concurrently");
            return attemptRejection(requestId, rejectionRequest);
        }, executor);

        // Wait for all operations to complete
        CompletableFuture.allOf(approval1, approval2, rejection).join();
        executor.shutdown();

        // Only one operation should succeed, others should fail with appropriate status
        ResponseEntity<LeaveRequestResponse> result1 = approval1.get();
        ResponseEntity<LeaveRequestResponse> result2 = approval2.get();
        ResponseEntity<String> result3 = rejection.get();

        // At least one should succeed, others should fail
        int successCount = 0;
        if (result1.getStatusCode().is2xxSuccessful()) successCount++;
        if (result2.getStatusCode().is2xxSuccessful()) successCount++;
        if (result3.getStatusCode().is2xxSuccessful()) successCount++;

        assertThat(successCount).isEqualTo(1); // Only one operation should succeed
    }

    @Test
    @DisplayName("Self-approval prevention")
    void testSelfApprovalPrevention() {
        // Create an employee who is also a manager
        CreateEmployeeRequest managerEmployeeRequest = new CreateEmployeeRequest();
        managerEmployeeRequest.setEmployeeId("MGR_SELF");
        managerEmployeeRequest.setName("Self Manager");
        managerEmployeeRequest.setEmail("self.manager@company.com");
        managerEmployeeRequest.setDepartment("Engineering");
        managerEmployeeRequest.setJoiningDate(LocalDate.of(2020, 1, 1));

        restTemplate.postForEntity(baseUrl + "/employees", managerEmployeeRequest, EmployeeResponse.class);

        // Initialize leave balance
        InitializeBalanceRequest balanceRequest = new InitializeBalanceRequest();
        balanceRequest.setYear(2024);
        restTemplate.postForEntity(
                baseUrl + "/leave-balances/employee/MGR_SELF/initialize", 
                balanceRequest, List.class);

        // Apply for leave as manager
        LeaveApplicationRequest leaveRequest = new LeaveApplicationRequest();
        leaveRequest.setEmployeeId("MGR_SELF");
        leaveRequest.setLeaveType(LeaveType.PERSONAL);
        leaveRequest.setStartDate(LocalDate.now().plusDays(8));
        leaveRequest.setEndDate(LocalDate.now().plusDays(9));
        leaveRequest.setDuration(LeaveDuration.FULL_DAY);
        leaveRequest.setReason("Personal work");

        ResponseEntity<LeaveRequestResponse> leaveResponse = restTemplate.postForEntity(
                baseUrl + "/leave-requests", leaveRequest, LeaveRequestResponse.class);
        assertThat(leaveResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        LeaveRequestResponse leaveResponseBody = leaveResponse.getBody();
        assertThat(leaveResponseBody).isNotNull();
        String requestId = leaveResponseBody.getRequestId();

        // Try to self-approve
        ApprovalRequest selfApprovalRequest = new ApprovalRequest();
        selfApprovalRequest.setApproverId("MGR_SELF");
        selfApprovalRequest.setComments("Self approval attempt");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ApprovalRequest> approvalEntity = new HttpEntity<>(selfApprovalRequest, headers);

        ResponseEntity<String> selfApprovalResponse = restTemplate.exchange(
                baseUrl + "/leave-requests/" + requestId + "/approve",
                HttpMethod.PUT, approvalEntity, String.class);
        
        // Self-approval should be prevented
        assertThat(selfApprovalResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Backdated leave request handling")
    void testBackdatedLeaveRequestHandling() {
        setupManagerAndEmployee("MGR_BACKDATE", "EMP_BACKDATE");

        // Apply for backdated leave (within 30 days)
        LeaveApplicationRequest backdatedRequest = new LeaveApplicationRequest();
        backdatedRequest.setEmployeeId("EMP_BACKDATE");
        backdatedRequest.setLeaveType(LeaveType.SICK);
        backdatedRequest.setStartDate(LocalDate.now().minusDays(5));
        backdatedRequest.setEndDate(LocalDate.now().minusDays(3));
        backdatedRequest.setDuration(LeaveDuration.FULL_DAY);
        backdatedRequest.setReason("Was sick, applying retroactively");
        // Note: isBackdated is determined by the system based on dates, not set by user

        ResponseEntity<LeaveRequestResponse> backdatedResponse = restTemplate.postForEntity(
                baseUrl + "/leave-requests", backdatedRequest, LeaveRequestResponse.class);
        assertThat(backdatedResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        LeaveRequestResponse backdatedLeave = backdatedResponse.getBody();
        assertThat(backdatedLeave).isNotNull();
        assertThat(backdatedLeave.getIsBackdated()).isTrue();
        assertThat(backdatedLeave.getStatus()).isEqualTo(LeaveStatus.PENDING);

        // Manager can approve backdated request
        String requestId = backdatedLeave.getRequestId();
        approveRequest(requestId, "MGR_BACKDATE", "Approved backdated sick leave");

        // Verify approval
        ResponseEntity<List> historyResponse = restTemplate.getForEntity(
                baseUrl + "/leave-requests/employee/EMP_BACKDATE", List.class);
        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Object> historyList = (List<Object>) historyResponse.getBody();
        
        List<?> historyBody = historyResponse.getBody();
        assertThat(historyBody).isNotNull();
        assertThat(historyBody).hasSize(1);
    }

    private void setupOrganizationalHierarchy() {
        // Create CEO
        CreateEmployeeRequest ceoRequest = new CreateEmployeeRequest();
        ceoRequest.setEmployeeId("CEO_HIER");
        ceoRequest.setName("CEO Chief");
        ceoRequest.setEmail("ceo@company.com");
        ceoRequest.setDepartment("Executive");
        ceoRequest.setJoiningDate(LocalDate.of(2015, 1, 1));
        restTemplate.postForEntity(baseUrl + "/employees", ceoRequest, EmployeeResponse.class);

        // Create Director reporting to CEO
        CreateEmployeeRequest directorRequest = new CreateEmployeeRequest();
        directorRequest.setEmployeeId("DIR_HIER");
        directorRequest.setName("Director Direct");
        directorRequest.setEmail("director@company.com");
        directorRequest.setDepartment("Engineering");
        directorRequest.setJoiningDate(LocalDate.of(2018, 1, 1));
        directorRequest.setManagerId("CEO_HIER");
        restTemplate.postForEntity(baseUrl + "/employees", directorRequest, EmployeeResponse.class);

        // Create Manager reporting to Director
        CreateEmployeeRequest managerRequest = new CreateEmployeeRequest();
        managerRequest.setEmployeeId("MGR_HIER");
        managerRequest.setName("Manager Manage");
        managerRequest.setEmail("manager@company.com");
        managerRequest.setDepartment("Engineering");
        managerRequest.setJoiningDate(LocalDate.of(2020, 1, 1));
        managerRequest.setManagerId("DIR_HIER");
        restTemplate.postForEntity(baseUrl + "/employees", managerRequest, EmployeeResponse.class);

        // Create Employee reporting to Manager
        CreateEmployeeRequest employeeRequest = new CreateEmployeeRequest();
        employeeRequest.setEmployeeId("EMP_HIER");
        employeeRequest.setName("Employee Employ");
        employeeRequest.setEmail("employee@company.com");
        employeeRequest.setDepartment("Engineering");
        employeeRequest.setJoiningDate(LocalDate.of(2023, 1, 1));
        employeeRequest.setManagerId("MGR_HIER");
        restTemplate.postForEntity(baseUrl + "/employees", employeeRequest, EmployeeResponse.class);

        // Initialize leave balance for employee
        InitializeBalanceRequest balanceRequest = new InitializeBalanceRequest();
        balanceRequest.setYear(2024);
        restTemplate.postForEntity(
                baseUrl + "/leave-balances/employee/EMP_HIER/initialize", 
                balanceRequest, List.class);
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

    private void approveRequest(String requestId, String approverId, String comments) {
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setApproverId(approverId);
        approvalRequest.setComments(comments);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ApprovalRequest> approvalEntity = new HttpEntity<>(approvalRequest, headers);

        restTemplate.exchange(
                baseUrl + "/leave-requests/" + requestId + "/approve",
                HttpMethod.PUT, approvalEntity, LeaveRequestResponse.class);
    }

    private void rejectRequest(String requestId, String approverId, String reason) {
        RejectionRequest rejectionRequest = new RejectionRequest();
        rejectionRequest.setApproverId(approverId);
        rejectionRequest.setRejectionReason(reason);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RejectionRequest> rejectionEntity = new HttpEntity<>(rejectionRequest, headers);

        restTemplate.exchange(
                baseUrl + "/leave-requests/" + requestId + "/reject",
                HttpMethod.PUT, rejectionEntity, RejectionRequest.class);
    }

    private ResponseEntity<LeaveRequestResponse> attemptApproval(String requestId, ApprovalRequest approvalRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ApprovalRequest> approvalEntity = new HttpEntity<>(approvalRequest, headers);

        return restTemplate.exchange(
                baseUrl + "/leave-requests/" + requestId + "/approve",
                HttpMethod.PUT, approvalEntity, LeaveRequestResponse.class);
    }

    private ResponseEntity<String> attemptRejection(String requestId, RejectionRequest rejectionRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RejectionRequest> rejectionEntity = new HttpEntity<>(rejectionRequest, headers);

        return restTemplate.exchange(
                baseUrl + "/leave-requests/" + requestId + "/reject",
                HttpMethod.PUT, rejectionEntity, String.class);
    }
}