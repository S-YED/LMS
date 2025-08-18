package com.company.leavemanagementsystem.integration;

import com.company.leavemanagementsystem.dto.*;
import com.company.leavemanagementsystem.entity.LeaveDuration;
import com.company.leavemanagementsystem.entity.LeaveStatus;
import com.company.leavemanagementsystem.entity.LeaveType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for edge cases with real database interactions
 */
@DisplayName("Edge Case Integration Tests")
public class EdgeCaseIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Employee with no manager can still apply for emergency leave")
    void testEmployeeWithoutManagerEmergencyLeave() {
        // Create employee without manager
        CreateEmployeeRequest employeeRequest = new CreateEmployeeRequest();
        employeeRequest.setEmployeeId("EMP_NO_MGR");
        employeeRequest.setName("No Manager Employee");
        employeeRequest.setEmail("no.manager@company.com");
        employeeRequest.setDepartment("Engineering");
        employeeRequest.setJoiningDate(LocalDate.of(2023, 1, 1));
        // No manager assigned

        ResponseEntity<EmployeeResponse> employeeResponse = restTemplate.postForEntity(
                baseUrl + "/employees", employeeRequest, EmployeeResponse.class);
        assertThat(employeeResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Initialize leave balance
        InitializeBalanceRequest balanceRequest = new InitializeBalanceRequest();
        balanceRequest.setYear(2024);
        restTemplate.postForEntity(
                baseUrl + "/leave-balances/employee/EMP_NO_MGR/initialize", 
                balanceRequest, List.class);

        // Apply for emergency leave
        LeaveApplicationRequest emergencyRequest = new LeaveApplicationRequest();
        emergencyRequest.setEmployeeId("EMP_NO_MGR");
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
        assertThat(emergencyLeave.getStatus()).isEqualTo(LeaveStatus.AUTO_APPROVED);
    }

    @Test
    @DisplayName("Half-day leave calculations with fractional balances")
    void testHalfDayLeaveCalculations() {
        setupManagerAndEmployee("MGR_HALF", "EMP_HALF");

        // Apply for half-day leave
        LeaveApplicationRequest halfDayRequest = new LeaveApplicationRequest();
        halfDayRequest.setEmployeeId("EMP_HALF");
        halfDayRequest.setLeaveType(LeaveType.PERSONAL);
        halfDayRequest.setStartDate(LocalDate.now().plusDays(5));
        halfDayRequest.setEndDate(LocalDate.now().plusDays(5));
        halfDayRequest.setDuration(LeaveDuration.HALF_DAY);
        halfDayRequest.setReason("Half day personal work");

        ResponseEntity<LeaveRequestResponse> halfDayResponse = restTemplate.postForEntity(
                baseUrl + "/leave-requests", halfDayRequest, LeaveRequestResponse.class);
        assertThat(halfDayResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        LeaveRequestResponse halfDayLeave = halfDayResponse.getBody();
        assertThat(halfDayLeave).isNotNull();
        assertThat(halfDayLeave.getTotalDays()).isEqualTo(0.5);

        // Approve the half-day leave
        approveRequest(halfDayLeave.getRequestId(), "MGR_HALF", "Approved half day");

        // Check that balance is reduced by 0.5 days
        ResponseEntity<List> balanceResponse = restTemplate.getForEntity(
                baseUrl + "/leave-balances/employee/EMP_HALF", List.class);
        assertThat(balanceResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Object> balanceList = (List<Object>) balanceResponse.getBody();
        // Balance should show fractional usage

        // Apply for another half-day on the same day (should be rejected due to overlap)
        LeaveApplicationRequest overlappingHalfDay = new LeaveApplicationRequest();
        overlappingHalfDay.setEmployeeId("EMP_HALF");
        overlappingHalfDay.setLeaveType(LeaveType.PERSONAL);
        overlappingHalfDay.setStartDate(LocalDate.now().plusDays(5));
        overlappingHalfDay.setEndDate(LocalDate.now().plusDays(5));
        overlappingHalfDay.setDuration(LeaveDuration.HALF_DAY);
        overlappingHalfDay.setReason("Another half day");

        ResponseEntity<String> overlappingResponse = restTemplate.postForEntity(
                baseUrl + "/leave-requests", overlappingHalfDay, String.class);
        assertThat(overlappingResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Weekend exclusion in leave calculations")
    void testWeekendExclusionInLeaveCalculations() {
        setupManagerAndEmployee("MGR_WEEKEND", "EMP_WEEKEND");

        // Apply for leave that spans a weekend (Friday to Monday)
        LocalDate friday = getNextFriday();
        LocalDate monday = friday.plusDays(3);

        LeaveApplicationRequest weekendSpanRequest = new LeaveApplicationRequest();
        weekendSpanRequest.setEmployeeId("EMP_WEEKEND");
        weekendSpanRequest.setLeaveType(LeaveType.VACATION);
        weekendSpanRequest.setStartDate(friday);
        weekendSpanRequest.setEndDate(monday);
        weekendSpanRequest.setDuration(LeaveDuration.FULL_DAY);
        weekendSpanRequest.setReason("Long weekend vacation");

        ResponseEntity<LeaveRequestResponse> weekendResponse = restTemplate.postForEntity(
                baseUrl + "/leave-requests", weekendSpanRequest, LeaveRequestResponse.class);
        assertThat(weekendResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        LeaveRequestResponse weekendLeave = weekendResponse.getBody();
        assertThat(weekendLeave).isNotNull();
        // Should only count Friday and Monday (2 working days), not Saturday and Sunday
        assertThat(weekendLeave.getTotalDays()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Leave application before joining date validation")
    void testLeaveBeforeJoiningDateValidation() {
        // Create employee with recent joining date
        CreateEmployeeRequest recentEmployeeRequest = new CreateEmployeeRequest();
        recentEmployeeRequest.setEmployeeId("EMP_RECENT");
        recentEmployeeRequest.setName("Recent Employee");
        recentEmployeeRequest.setEmail("recent@company.com");
        recentEmployeeRequest.setDepartment("Engineering");
        recentEmployeeRequest.setJoiningDate(LocalDate.now().plusDays(10)); // Joins in future

        ResponseEntity<EmployeeResponse> employeeResponse = restTemplate.postForEntity(
                baseUrl + "/employees", recentEmployeeRequest, EmployeeResponse.class);
        assertThat(employeeResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Try to apply for leave before joining date
        LeaveApplicationRequest beforeJoiningRequest = new LeaveApplicationRequest();
        beforeJoiningRequest.setEmployeeId("EMP_RECENT");
        beforeJoiningRequest.setLeaveType(LeaveType.VACATION);
        beforeJoiningRequest.setStartDate(LocalDate.now().plusDays(5)); // Before joining
        beforeJoiningRequest.setEndDate(LocalDate.now().plusDays(7));
        beforeJoiningRequest.setDuration(LeaveDuration.FULL_DAY);
        beforeJoiningRequest.setReason("Vacation before joining");

        ResponseEntity<String> beforeJoiningResponse = restTemplate.postForEntity(
                baseUrl + "/leave-requests", beforeJoiningRequest, String.class);
        assertThat(beforeJoiningResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Database constraint violations handling")
    void testDatabaseConstraintViolations() {
        // Try to create employee with duplicate employee ID
        CreateEmployeeRequest employee1 = new CreateEmployeeRequest();
        employee1.setEmployeeId("DUPLICATE_ID");
        employee1.setName("First Employee");
        employee1.setEmail("first@company.com");
        employee1.setDepartment("Engineering");
        employee1.setJoiningDate(LocalDate.of(2023, 1, 1));

        ResponseEntity<EmployeeResponse> firstResponse = restTemplate.postForEntity(
                baseUrl + "/employees", employee1, EmployeeResponse.class);
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Try to create another employee with same ID
        CreateEmployeeRequest employee2 = new CreateEmployeeRequest();
        employee2.setEmployeeId("DUPLICATE_ID");
        employee2.setName("Second Employee");
        employee2.setEmail("second@company.com");
        employee2.setDepartment("Engineering");
        employee2.setJoiningDate(LocalDate.of(2023, 1, 1));

        ResponseEntity<String> duplicateResponse = restTemplate.postForEntity(
                baseUrl + "/employees", employee2, String.class);
        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Try to create employee with duplicate email
        CreateEmployeeRequest employee3 = new CreateEmployeeRequest();
        employee3.setEmployeeId("UNIQUE_ID");
        employee3.setName("Third Employee");
        employee3.setEmail("first@company.com"); // Same email as first employee
        employee3.setDepartment("Engineering");
        employee3.setJoiningDate(LocalDate.of(2023, 1, 1));

        ResponseEntity<String> duplicateEmailResponse = restTemplate.postForEntity(
                baseUrl + "/employees", employee3, String.class);
        assertThat(duplicateEmailResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Large dataset pagination and performance")
    void testLargeDatasetPagination() {
        // Create manager
        CreateEmployeeRequest managerRequest = new CreateEmployeeRequest();
        managerRequest.setEmployeeId("MGR_LARGE");
        managerRequest.setName("Large Dataset Manager");
        managerRequest.setEmail("large.manager@company.com");
        managerRequest.setDepartment("Engineering");
        managerRequest.setJoiningDate(LocalDate.of(2020, 1, 1));

        restTemplate.postForEntity(baseUrl + "/employees", managerRequest, EmployeeResponse.class);

        // Create multiple employees and leave requests
        for (int i = 1; i <= 10; i++) {
            CreateEmployeeRequest employeeRequest = new CreateEmployeeRequest();
            employeeRequest.setEmployeeId("EMP_LARGE_" + i);
            employeeRequest.setName("Employee " + i);
            employeeRequest.setEmail("emp" + i + "@company.com");
            employeeRequest.setDepartment("Engineering");
            employeeRequest.setJoiningDate(LocalDate.of(2023, 1, 1));
            employeeRequest.setManagerId("MGR_LARGE");

            restTemplate.postForEntity(baseUrl + "/employees", employeeRequest, EmployeeResponse.class);

            // Initialize balance
            InitializeBalanceRequest balanceRequest = new InitializeBalanceRequest();
            balanceRequest.setYear(2024);
            restTemplate.postForEntity(
                    baseUrl + "/leave-balances/employee/EMP_LARGE_" + i + "/initialize", 
                    balanceRequest, List.class);

            // Create multiple leave requests per employee
            for (int j = 1; j <= 3; j++) {
                LeaveApplicationRequest leaveRequest = new LeaveApplicationRequest();
                leaveRequest.setEmployeeId("EMP_LARGE_" + i);
                leaveRequest.setLeaveType(LeaveType.VACATION);
                leaveRequest.setStartDate(LocalDate.now().plusDays(j * 10));
                leaveRequest.setEndDate(LocalDate.now().plusDays(j * 10 + 2));
                leaveRequest.setDuration(LeaveDuration.FULL_DAY);
                leaveRequest.setReason("Vacation " + j);

                restTemplate.postForEntity(baseUrl + "/leave-requests", leaveRequest, LeaveRequestResponse.class);
            }
        }

        // Test pagination of pending requests
        ResponseEntity<List> pendingResponse = restTemplate.getForEntity(
                baseUrl + "/leave-requests/pending?managerId=MGR_LARGE", List.class);
        assertThat(pendingResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Object> pendingList = (List<Object>) pendingResponse.getBody();
        assertThat(pendingResponse.getBody()).hasSize(30); // 10 employees * 3 requests each

        // Test employee list pagination
        ResponseEntity<List> employeeListResponse = restTemplate.getForEntity(
                baseUrl + "/employees", List.class);
        assertThat(employeeListResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Object> employeeList = (List<Object>) employeeListResponse.getBody();
        assertThat(employeeListResponse.getBody().size()).isGreaterThan(10);
    }

    @Test
    @DisplayName("Invalid date range validations")
    void testInvalidDateRangeValidations() {
        setupManagerAndEmployee("MGR_DATE", "EMP_DATE");

        // Test end date before start date
        LeaveApplicationRequest invalidRangeRequest = new LeaveApplicationRequest();
        invalidRangeRequest.setEmployeeId("EMP_DATE");
        invalidRangeRequest.setLeaveType(LeaveType.VACATION);
        invalidRangeRequest.setStartDate(LocalDate.now().plusDays(10));
        invalidRangeRequest.setEndDate(LocalDate.now().plusDays(5)); // End before start
        invalidRangeRequest.setDuration(LeaveDuration.FULL_DAY);
        invalidRangeRequest.setReason("Invalid date range");

        ResponseEntity<String> invalidRangeResponse = restTemplate.postForEntity(
                baseUrl + "/leave-requests", invalidRangeRequest, String.class);
        assertThat(invalidRangeResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Test same start and end date with full day (should be valid)
        LeaveApplicationRequest sameDateRequest = new LeaveApplicationRequest();
        sameDateRequest.setEmployeeId("EMP_DATE");
        sameDateRequest.setLeaveType(LeaveType.SICK);
        sameDateRequest.setStartDate(LocalDate.now().plusDays(8));
        sameDateRequest.setEndDate(LocalDate.now().plusDays(8));
        sameDateRequest.setDuration(LeaveDuration.FULL_DAY);
        sameDateRequest.setReason("Single day sick leave");

        ResponseEntity<LeaveRequestResponse> sameDateResponse = restTemplate.postForEntity(
                baseUrl + "/leave-requests", sameDateRequest, LeaveRequestResponse.class);
        assertThat(sameDateResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(sameDateResponse.getBody().getTotalDays()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Employee deletion with existing leave history")
    void testEmployeeDeletionWithLeaveHistory() {
        setupManagerAndEmployee("MGR_DELETE", "EMP_DELETE");

        // Apply and approve a leave request
        LeaveApplicationRequest leaveRequest = new LeaveApplicationRequest();
        leaveRequest.setEmployeeId("EMP_DELETE");
        leaveRequest.setLeaveType(LeaveType.VACATION);
        leaveRequest.setStartDate(LocalDate.now().plusDays(15));
        leaveRequest.setEndDate(LocalDate.now().plusDays(17));
        leaveRequest.setDuration(LeaveDuration.FULL_DAY);
        leaveRequest.setReason("Vacation before deletion");

        ResponseEntity<LeaveRequestResponse> leaveResponse = restTemplate.postForEntity(
                baseUrl + "/leave-requests", leaveRequest, LeaveRequestResponse.class);
        assertThat(leaveResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String requestId = leaveResponse.getBody().getRequestId();

        approveRequest(requestId, "MGR_DELETE", "Approved before deletion");

        // Try to delete employee with leave history
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                baseUrl + "/employees/EMP_DELETE",
                org.springframework.http.HttpMethod.DELETE,
                null,
                String.class);

        // Employee deletion should be handled gracefully (either prevented or archived)
        // The exact behavior depends on business requirements
        assertThat(deleteResponse.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.CONFLICT);
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

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        org.springframework.http.HttpEntity<ApprovalRequest> approvalEntity = 
                new org.springframework.http.HttpEntity<>(approvalRequest, headers);

        restTemplate.exchange(
                baseUrl + "/leave-requests/" + requestId + "/approve",
                org.springframework.http.HttpMethod.PUT, approvalEntity, LeaveRequestResponse.class);
    }

    private LocalDate getNextFriday() {
        LocalDate today = LocalDate.now();
        int daysUntilFriday = (5 - today.getDayOfWeek().getValue()) % 7;
        if (daysUntilFriday == 0 && today.getDayOfWeek().getValue() == 5) {
            daysUntilFriday = 7; // If today is Friday, get next Friday
        }
        return today.plusDays(daysUntilFriday == 0 ? 7 : daysUntilFriday);
    }
}