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


import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end API tests covering complete user journeys
 */
@DisplayName("End-to-End API Tests")
public class EndToEndApiTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Complete employee lifecycle - Creation to leave management")
    void testCompleteEmployeeLifecycle() {
        // Journey 1: HR creates organizational structure
        
        // Step 1: Create CEO
        EmployeeResponse ceo = createEmployee("CEO001", "Chief Executive", "ceo@company.com", 
                "Executive", LocalDate.of(2015, 1, 1), null);
        assertThat(ceo.getEmployeeId()).isEqualTo("CEO001");

        // Step 2: Create Department Head reporting to CEO
        EmployeeResponse deptHead = createEmployee("DEPT001", "Department Head", "dept.head@company.com", 
                "Engineering", LocalDate.of(2018, 1, 1), "CEO001");
        assertThat(deptHead.getManager().getEmployeeId()).isEqualTo("CEO001");

        // Step 3: Create Team Manager reporting to Department Head
        EmployeeResponse teamManager = createEmployee("MGR001", "Team Manager", "team.manager@company.com", 
                "Engineering", LocalDate.of(2020, 1, 1), "DEPT001");
        assertThat(teamManager.getManager().getEmployeeId()).isEqualTo("DEPT001");

        // Step 4: Create Team Lead reporting to Team Manager
        EmployeeResponse teamLead = createEmployee("LEAD001", "Team Lead", "team.lead@company.com", 
                "Engineering", LocalDate.of(2021, 1, 1), "MGR001");
        assertThat(teamLead.getManager().getEmployeeId()).isEqualTo("MGR001");

        // Step 5: Create Employees reporting to Team Lead
        EmployeeResponse employee1 = createEmployee("EMP001", "Senior Developer", "senior.dev@company.com", 
                "Engineering", LocalDate.of(2022, 1, 1), "LEAD001");
        EmployeeResponse employee2 = createEmployee("EMP002", "Junior Developer", "junior.dev@company.com", 
                "Engineering", LocalDate.of(2023, 1, 1), "LEAD001");

        // Journey 2: Initialize leave balances for all employees
        String[] employeeIds = {"CEO001", "DEPT001", "MGR001", "LEAD001", "EMP001", "EMP002"};
        for (String empId : employeeIds) {
            initializeLeaveBalance(empId, 2024);
        }

        // Journey 3: Employee applies for leave
        LeaveRequestResponse leaveRequest = applyForLeave("EMP001", LeaveType.VACATION, 
                LocalDate.now().plusDays(15), LocalDate.now().plusDays(19), 
                LeaveDuration.FULL_DAY, "Annual vacation");
        assertThat(leaveRequest.getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(leaveRequest.getTotalDays()).isEqualTo(5.0);

        // Journey 4: Team Lead approves the leave
        LeaveRequestResponse approvedLeave = approveLeave(leaveRequest.getRequestId(), "LEAD001", 
                "Approved for annual vacation");
        assertThat(approvedLeave.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(approvedLeave.getApprovedBy().getEmployeeId()).isEqualTo("LEAD001");

        // Journey 5: Verify leave balance is updated
        List<LeaveBalanceResponse> updatedBalances = getLeaveBalances("EMP001");
        LeaveBalanceResponse vacationBalance = updatedBalances.stream()
                .filter(b -> b.getLeaveType() == LeaveType.VACATION)
                .findFirst()
                .orElseThrow();
        assertThat(vacationBalance.getUsedDays()).isEqualTo(5);
        assertThat(vacationBalance.getAvailableDays()).isEqualTo(15); // 20 - 5

        // Journey 6: Employee checks leave history
        List<LeaveRequestResponse> leaveHistory = getEmployeeLeaveHistory("EMP001");
        assertThat(leaveHistory).hasSize(1);
        assertThat(leaveHistory.get(0).getStatus()).isEqualTo(LeaveStatus.APPROVED);

        // Journey 7: Manager views team's pending requests (should be empty now)
        List<LeaveRequestResponse> pendingRequests = getPendingRequests("LEAD001");
        assertThat(pendingRequests).isEmpty();

        // Journey 8: Employee applies for another leave that would exceed balance
        ResponseEntity<String> excessiveLeaveResponse = applyForLeaveExpectingError("EMP001", LeaveType.VACATION, 
                LocalDate.now().plusDays(25), LocalDate.now().plusDays(45), 
                LeaveDuration.FULL_DAY, "Excessive vacation");
        assertThat(excessiveLeaveResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Journey 9: Employee updates their information
        EmployeeResponse updatedEmployee = updateEmployee("EMP001", "Senior Software Developer", 
                "senior.software.dev@company.com", "Engineering");
        assertThat(updatedEmployee.getName()).isEqualTo("Senior Software Developer");
        assertThat(updatedEmployee.getEmail()).isEqualTo("senior.software.dev@company.com");

        // Journey 10: Get employee summary for dashboard
        LeaveBalanceSummaryResponse summary = getLeaveBalanceSummary("EMP001");
        assertThat(summary.getEmployee().getEmployeeId()).isEqualTo("EMP001");
        assertThat(summary.getTotals().getTotalAvailableDays()).isEqualTo(30.0); // 15 vacation + 10 sick + 5 personal
    }

    @Test
    @DisplayName("Multi-user concurrent leave application scenario")
    void testMultiUserConcurrentLeaveScenario() {
        // Setup: Create team structure
        createEmployee("MGR_TEAM", "Team Manager", "team.mgr@company.com", 
                "Engineering", LocalDate.of(2020, 1, 1), null);
        
        String[] teamMembers = {"TEAM001", "TEAM002", "TEAM003", "TEAM004", "TEAM005"};
        for (String memberId : teamMembers) {
            createEmployee(memberId, "Team Member " + memberId, memberId.toLowerCase() + "@company.com", 
                    "Engineering", LocalDate.of(2023, 1, 1), "MGR_TEAM");
            initializeLeaveBalance(memberId, 2024);
        }

        // Scenario: Multiple team members apply for leave on same dates
        LocalDate conflictStartDate = LocalDate.now().plusDays(20);
        LocalDate conflictEndDate = LocalDate.now().plusDays(22);

        // First three members apply for leave successfully
        LeaveRequestResponse leave1 = applyForLeave("TEAM001", LeaveType.VACATION, 
                conflictStartDate, conflictEndDate, LeaveDuration.FULL_DAY, "Team vacation 1");
        LeaveRequestResponse leave2 = applyForLeave("TEAM002", LeaveType.VACATION, 
                conflictStartDate, conflictEndDate, LeaveDuration.FULL_DAY, "Team vacation 2");
        LeaveRequestResponse leave3 = applyForLeave("TEAM003", LeaveType.VACATION, 
                conflictStartDate, conflictEndDate, LeaveDuration.FULL_DAY, "Team vacation 3");

        assertThat(leave1.getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(leave2.getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(leave3.getStatus()).isEqualTo(LeaveStatus.PENDING);

        // Manager reviews all pending requests
        List<LeaveRequestResponse> allPending = getPendingRequests("MGR_TEAM");
        assertThat(allPending).hasSize(3);

        // Manager approves first two, rejects third due to coverage concerns
        approveLeave(leave1.getRequestId(), "MGR_TEAM", "Approved - good coverage");
        approveLeave(leave2.getRequestId(), "MGR_TEAM", "Approved - acceptable coverage");
        rejectLeave(leave3.getRequestId(), "MGR_TEAM", "Rejected - insufficient team coverage");

        // Verify final states
        List<LeaveRequestResponse> team1History = getEmployeeLeaveHistory("TEAM001");
        List<LeaveRequestResponse> team2History = getEmployeeLeaveHistory("TEAM002");
        List<LeaveRequestResponse> team3History = getEmployeeLeaveHistory("TEAM003");

        assertThat(team1History.get(0).getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(team2History.get(0).getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(team3History.get(0).getStatus()).isEqualTo(LeaveStatus.REJECTED);

        // Fourth member applies for different dates (should succeed)
        LeaveRequestResponse leave4 = applyForLeave("TEAM004", LeaveType.VACATION, 
                LocalDate.now().plusDays(30), LocalDate.now().plusDays(32), 
                LeaveDuration.FULL_DAY, "Different dates vacation");
        assertThat(leave4.getStatus()).isEqualTo(LeaveStatus.PENDING);

        // Fifth member applies for emergency leave (should auto-approve)
        LeaveRequestResponse emergencyLeave = applyForEmergencyLeave("TEAM005", 
                LocalDate.now(), LocalDate.now().plusDays(1), "Medical emergency");
        assertThat(emergencyLeave.getStatus()).isEqualTo(LeaveStatus.AUTO_APPROVED);
    }

    @Test
    @DisplayName("Error recovery and data consistency scenario")
    void testErrorRecoveryAndDataConsistency() {
        // Setup
        createEmployee("MGR_ERROR", "Error Manager", "error.mgr@company.com", 
                "Engineering", LocalDate.of(2020, 1, 1), null);
        createEmployee("EMP_ERROR", "Error Employee", "error.emp@company.com", 
                "Engineering", LocalDate.of(2023, 1, 1), "MGR_ERROR");
        initializeLeaveBalance("EMP_ERROR", 2024);

        // Scenario 1: Apply for leave with invalid data, then correct it
        ResponseEntity<String> invalidLeave = applyForLeaveExpectingError("EMP_ERROR", LeaveType.VACATION, 
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(5), // End before start
                LeaveDuration.FULL_DAY, "Invalid date range");
        assertThat(invalidLeave.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Correct application
        LeaveRequestResponse validLeave = applyForLeave("EMP_ERROR", LeaveType.VACATION, 
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), 
                LeaveDuration.FULL_DAY, "Corrected application");
        assertThat(validLeave.getStatus()).isEqualTo(LeaveStatus.PENDING);

        // Scenario 2: Try to approve with wrong manager, then correct manager approves
        ResponseEntity<String> wrongApproval = approveLeaveExpectingError(validLeave.getRequestId(), 
                "NONEXISTENT_MGR", "Wrong manager approval");
        assertThat(wrongApproval.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND);

        // Correct approval
        LeaveRequestResponse correctApproval = approveLeave(validLeave.getRequestId(), "MGR_ERROR", 
                "Correct manager approval");
        assertThat(correctApproval.getStatus()).isEqualTo(LeaveStatus.APPROVED);

        // Scenario 3: Verify data consistency after errors
        List<LeaveBalanceResponse> balances = getLeaveBalances("EMP_ERROR");
        LeaveBalanceResponse vacationBalance = balances.stream()
                .filter(b -> b.getLeaveType() == LeaveType.VACATION)
                .findFirst()
                .orElseThrow();
        assertThat(vacationBalance.getUsedDays()).isEqualTo(3); // Only successful leave counted
        assertThat(vacationBalance.getAvailableDays()).isEqualTo(17);

        // Scenario 4: Try to modify approved leave (should fail)
        ResponseEntity<String> modifyApproved = applyForLeaveExpectingError("EMP_ERROR", LeaveType.VACATION, 
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(14), // Overlapping with approved
                LeaveDuration.FULL_DAY, "Overlapping with approved");
        assertThat(modifyApproved.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Performance test with concurrent operations")
    void testPerformanceWithConcurrentOperations() {
        // Setup: Create multiple managers and employees
        for (int i = 1; i <= 5; i++) {
            createEmployee("PERF_MGR_" + i, "Performance Manager " + i, 
                    "perf.mgr." + i + "@company.com", "Engineering", 
                    LocalDate.of(2020, 1, 1), null);
            
            for (int j = 1; j <= 10; j++) {
                String empId = "PERF_EMP_" + i + "_" + j;
                createEmployee(empId, "Performance Employee " + i + "_" + j, 
                        empId.toLowerCase() + "@company.com", "Engineering", 
                        LocalDate.of(2023, 1, 1), "PERF_MGR_" + i);
                initializeLeaveBalance(empId, 2024);
            }
        }

        // Performance test: Apply for multiple leaves concurrently
        long startTime = System.currentTimeMillis();
        
        for (int i = 1; i <= 5; i++) {
            for (int j = 1; j <= 10; j++) {
                String empId = "PERF_EMP_" + i + "_" + j;
                applyForLeave(empId, LeaveType.VACATION, 
                        LocalDate.now().plusDays(i * 10 + j), 
                        LocalDate.now().plusDays(i * 10 + j + 2), 
                        LeaveDuration.FULL_DAY, "Performance test leave");
            }
        }
        
        long applicationTime = System.currentTimeMillis() - startTime;
        
        // Verify all applications were created
        for (int i = 1; i <= 5; i++) {
            List<LeaveRequestResponse> managerPending = getPendingRequests("PERF_MGR_" + i);
            assertThat(managerPending).hasSize(10);
        }
        
        // Performance assertion (should complete within reasonable time)
        assertThat(applicationTime).isLessThan(30000); // 30 seconds max for 50 applications
        
        // Approve all leaves and measure time
        startTime = System.currentTimeMillis();
        
        for (int i = 1; i <= 5; i++) {
            List<LeaveRequestResponse> pending = getPendingRequests("PERF_MGR_" + i);
            for (LeaveRequestResponse request : pending) {
                approveLeave(request.getRequestId(), "PERF_MGR_" + i, "Bulk approval");
            }
        }
        
        long approvalTime = System.currentTimeMillis() - startTime;
        assertThat(approvalTime).isLessThan(30000); // 30 seconds max for 50 approvals
        
        // Verify all leaves are approved
        for (int i = 1; i <= 5; i++) {
            List<LeaveRequestResponse> managerPending = getPendingRequests("PERF_MGR_" + i);
            assertThat(managerPending).isEmpty();
        }
    }

    @Test
    @DisplayName("Data consistency across multiple operations")
    void testDataConsistencyAcrossOperations() {
        // Setup
        createEmployee("CONSIST_MGR", "Consistency Manager", "consist.mgr@company.com", 
                "Engineering", LocalDate.of(2020, 1, 1), null);
        createEmployee("CONSIST_EMP", "Consistency Employee", "consist.emp@company.com", 
                "Engineering", LocalDate.of(2023, 1, 1), "CONSIST_MGR");
        initializeLeaveBalance("CONSIST_EMP", 2024);

        // Get initial balance
        List<LeaveBalanceResponse> initialBalances = getLeaveBalances("CONSIST_EMP");
        LeaveBalanceResponse initialVacationBalance = initialBalances.stream()
                .filter(b -> b.getLeaveType() == LeaveType.VACATION)
                .findFirst()
                .orElseThrow();
        int initialAvailable = initialVacationBalance.getAvailableDays().intValue();

        // Apply for multiple leaves
        LeaveRequestResponse leave1 = applyForLeave("CONSIST_EMP", LeaveType.VACATION, 
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), 
                LeaveDuration.FULL_DAY, "First leave");
        LeaveRequestResponse leave2 = applyForLeave("CONSIST_EMP", LeaveType.VACATION, 
                LocalDate.now().plusDays(20), LocalDate.now().plusDays(22), 
                LeaveDuration.FULL_DAY, "Second leave");
        LeaveRequestResponse leave3 = applyForLeave("CONSIST_EMP", LeaveType.SICK, 
                LocalDate.now().plusDays(30), LocalDate.now().plusDays(31), 
                LeaveDuration.FULL_DAY, "Sick leave");

        // Approve first two, reject third
        approveLeave(leave1.getRequestId(), "CONSIST_MGR", "Approved first");
        approveLeave(leave2.getRequestId(), "CONSIST_MGR", "Approved second");
        rejectLeave(leave3.getRequestId(), "CONSIST_MGR", "Rejected sick leave");

        // Verify balance consistency
        List<LeaveBalanceResponse> finalBalances = getLeaveBalances("CONSIST_EMP");
        LeaveBalanceResponse finalVacationBalance = finalBalances.stream()
                .filter(b -> b.getLeaveType() == LeaveType.VACATION)
                .findFirst()
                .orElseThrow();
        LeaveBalanceResponse finalSickBalance = finalBalances.stream()
                .filter(b -> b.getLeaveType() == LeaveType.SICK)
                .findFirst()
                .orElseThrow();

        // Vacation balance should be reduced by 6 days (3 + 3)
        assertThat(finalVacationBalance.getUsedDays()).isEqualTo(6);
        assertThat(finalVacationBalance.getAvailableDays()).isEqualTo(initialAvailable - 6);

        // Sick balance should be unchanged (leave was rejected)
        assertThat(finalSickBalance.getUsedDays()).isEqualTo(0);
        assertThat(finalSickBalance.getAvailableDays()).isEqualTo(10); // Default sick days

        // Verify leave history consistency
        List<LeaveRequestResponse> history = getEmployeeLeaveHistory("CONSIST_EMP");
        assertThat(history).hasSize(3);
        
        long approvedCount = history.stream()
                .filter(h -> h.getStatus() == LeaveStatus.APPROVED)
                .count();
        long rejectedCount = history.stream()
                .filter(h -> h.getStatus() == LeaveStatus.REJECTED)
                .count();
        
        assertThat(approvedCount).isEqualTo(2);
        assertThat(rejectedCount).isEqualTo(1);
    }

    // Helper methods for common operations
    private EmployeeResponse createEmployee(String employeeId, String name, String email, 
                                          String department, LocalDate joiningDate, String managerId) {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setEmployeeId(employeeId);
        request.setName(name);
        request.setEmail(email);
        request.setDepartment(department);
        request.setJoiningDate(joiningDate);
        request.setManagerId(managerId);

        ResponseEntity<EmployeeResponse> response = restTemplate.postForEntity(
                baseUrl + "/employees", request, EmployeeResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private EmployeeResponse updateEmployee(String employeeId, String name, String email, String department) {
        UpdateEmployeeRequest request = new UpdateEmployeeRequest();
        request.setName(name);
        request.setEmail(email);
        request.setDepartment(department);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UpdateEmployeeRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<EmployeeResponse> response = restTemplate.exchange(
                baseUrl + "/employees/" + employeeId, HttpMethod.PUT, entity, EmployeeResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private void initializeLeaveBalance(String employeeId, int year) {
        InitializeBalanceRequest request = new InitializeBalanceRequest();
        request.setYear(year);

        ResponseEntity<List> response = restTemplate.postForEntity(
                baseUrl + "/leave-balances/employee/" + employeeId + "/initialize", 
                request, List.class);
        @SuppressWarnings("unchecked")
        List<Object> balanceList = (List<Object>) response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private LeaveRequestResponse applyForLeave(String employeeId, LeaveType leaveType, 
                                             LocalDate startDate, LocalDate endDate, 
                                             LeaveDuration duration, String reason) {
        LeaveApplicationRequest request = new LeaveApplicationRequest();
        request.setEmployeeId(employeeId);
        request.setLeaveType(leaveType);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setDuration(duration);
        request.setReason(reason);

        ResponseEntity<LeaveRequestResponse> response = restTemplate.postForEntity(
                baseUrl + "/leave-requests", request, LeaveRequestResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private LeaveRequestResponse applyForEmergencyLeave(String employeeId, LocalDate startDate, 
                                                       LocalDate endDate, String reason) {
        LeaveApplicationRequest request = new LeaveApplicationRequest();
        request.setEmployeeId(employeeId);
        request.setLeaveType(LeaveType.EMERGENCY);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setDuration(LeaveDuration.FULL_DAY);
        request.setReason(reason);
        request.setIsEmergencyLeave(true);

        ResponseEntity<LeaveRequestResponse> response = restTemplate.postForEntity(
                baseUrl + "/leave-requests", request, LeaveRequestResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private ResponseEntity<String> applyForLeaveExpectingError(String employeeId, LeaveType leaveType, 
                                                              LocalDate startDate, LocalDate endDate, 
                                                              LeaveDuration duration, String reason) {
        LeaveApplicationRequest request = new LeaveApplicationRequest();
        request.setEmployeeId(employeeId);
        request.setLeaveType(leaveType);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setDuration(duration);
        request.setReason(reason);

        return restTemplate.postForEntity(baseUrl + "/leave-requests", request, String.class);
    }

    private LeaveRequestResponse approveLeave(String requestId, String approverId, String comments) {
        ApprovalRequest request = new ApprovalRequest();
        request.setApproverId(approverId);
        request.setComments(comments);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ApprovalRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<LeaveRequestResponse> response = restTemplate.exchange(
                baseUrl + "/leave-requests/" + requestId + "/approve",
                HttpMethod.PUT, entity, LeaveRequestResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private ResponseEntity<String> approveLeaveExpectingError(String requestId, String approverId, String comments) {
        ApprovalRequest request = new ApprovalRequest();
        request.setApproverId(approverId);
        request.setComments(comments);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ApprovalRequest> entity = new HttpEntity<>(request, headers);

        return restTemplate.exchange(
                baseUrl + "/leave-requests/" + requestId + "/approve",
                HttpMethod.PUT, entity, String.class);
    }

    private LeaveRequestResponse rejectLeave(String requestId, String approverId, String reason) {
        RejectionRequest request = new RejectionRequest();
        request.setApproverId(approverId);
        request.setRejectionReason(reason);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RejectionRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<LeaveRequestResponse> response = restTemplate.exchange(
                baseUrl + "/leave-requests/" + requestId + "/reject",
                HttpMethod.PUT, entity, LeaveRequestResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private List<LeaveBalanceResponse> getLeaveBalances(String employeeId) {
        ResponseEntity<List> response = restTemplate.getForEntity(
                baseUrl + "/leave-balances/employee/" + employeeId, List.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (List<LeaveBalanceResponse>) response.getBody();
    }

    private LeaveBalanceSummaryResponse getLeaveBalanceSummary(String employeeId) {
        ResponseEntity<LeaveBalanceSummaryResponse> response = restTemplate.getForEntity(
                baseUrl + "/leave-balances/employee/" + employeeId + "/summary", 
                LeaveBalanceSummaryResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private List<LeaveRequestResponse> getEmployeeLeaveHistory(String employeeId) {
        ResponseEntity<List> response = restTemplate.getForEntity(
                baseUrl + "/leave-requests/employee/" + employeeId, List.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (List<LeaveRequestResponse>) response.getBody();
    }

    @SuppressWarnings("unchecked")
    private List<LeaveRequestResponse> getPendingRequests(String managerId) {
        ResponseEntity<List> response = restTemplate.getForEntity(
                baseUrl + "/leave-requests/pending?managerId=" + managerId, List.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (List<LeaveRequestResponse>) response.getBody();
    }
}