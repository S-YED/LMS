package com.company.leavemanagementsystem.integration;

import com.company.leavemanagementsystem.dto.*;
import com.company.leavemanagementsystem.entity.LeaveDuration;

import com.company.leavemanagementsystem.entity.LeaveType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance integration tests for concurrent operations
 */
@DisplayName("Performance Integration Tests")
public class PerformanceIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Concurrent leave applications performance test")
    void testConcurrentLeaveApplicationsPerformance() throws Exception {
        // Setup: Create manager and multiple employees
        createEmployee("PERF_MGR", "Performance Manager", "perf.mgr@company.com", 
                "Engineering", LocalDate.of(2020, 1, 1), null);

        int numberOfEmployees = 20;
        List<String> employeeIds = new ArrayList<>();
        
        for (int i = 1; i <= numberOfEmployees; i++) {
            String empId = "PERF_EMP_" + i;
            employeeIds.add(empId);
            createEmployee(empId, "Performance Employee " + i, empId.toLowerCase() + "@company.com", 
                    "Engineering", LocalDate.of(2023, 1, 1), "PERF_MGR");
            initializeLeaveBalance(empId, 2024);
        }

        // Performance test: Concurrent leave applications
        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (String empId : employeeIds) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    LeaveApplicationRequest request = new LeaveApplicationRequest();
                    request.setEmployeeId(empId);
                    request.setLeaveType(LeaveType.VACATION);
                    request.setStartDate(LocalDate.now().plusDays(15));
                    request.setEndDate(LocalDate.now().plusDays(17));
                    request.setDuration(LeaveDuration.FULL_DAY);
                    request.setReason("Concurrent performance test");

                    ResponseEntity<LeaveRequestResponse> response = restTemplate.postForEntity(
                            baseUrl + "/leave-requests", request, LeaveRequestResponse.class);
                    
                    if (response.getStatusCode() == HttpStatus.CREATED) {
                        successCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            }, executor);
            
            futures.add(future);
        }
        
        // Wait for all operations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        
        // Performance assertions
        assertThat(successCount.get()).isEqualTo(numberOfEmployees);
        assertThat(errorCount.get()).isEqualTo(0);
        assertThat(totalTime).isLessThan(15000); // Should complete within 15 seconds
        
        // Verify all requests are in the system
        ResponseEntity<List> pendingResponse = restTemplate.getForEntity(
                baseUrl + "/leave-requests/pending?managerId=PERF_MGR", List.class);
        assertThat(pendingResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Object> pendingList = (List<Object>) pendingResponse.getBody();
        assertThat(pendingList).isNotNull();
        assertThat(pendingResponse.getBody()).hasSize(numberOfEmployees);
        
        System.out.println("Concurrent leave applications performance:");
        System.out.println("- Total applications: " + numberOfEmployees);
        System.out.println("- Success count: " + successCount.get());
        System.out.println("- Error count: " + errorCount.get());
        System.out.println("- Total time: " + totalTime + "ms");
        System.out.println("- Average time per application: " + (totalTime / numberOfEmployees) + "ms");
    }

    @Test
    @DisplayName("Concurrent approval operations performance test")
    void testConcurrentApprovalOperationsPerformance() throws Exception {
        // Setup: Create manager and employees with pending leave requests
        createEmployee("APPROVAL_MGR", "Approval Manager", "approval.mgr@company.com", 
                "Engineering", LocalDate.of(2020, 1, 1), null);

        int numberOfRequests = 15;
        List<String> requestIds = new ArrayList<>();
        
        for (int i = 1; i <= numberOfRequests; i++) {
            String empId = "APPROVAL_EMP_" + i;
            createEmployee(empId, "Approval Employee " + i, empId.toLowerCase() + "@company.com", 
                    "Engineering", LocalDate.of(2023, 1, 1), "APPROVAL_MGR");
            initializeLeaveBalance(empId, 2024);
            
            // Create leave request
            LeaveApplicationRequest request = new LeaveApplicationRequest();
            request.setEmployeeId(empId);
            request.setLeaveType(LeaveType.VACATION);
            request.setStartDate(LocalDate.now().plusDays(20 + i));
            request.setEndDate(LocalDate.now().plusDays(22 + i));
            request.setDuration(LeaveDuration.FULL_DAY);
            request.setReason("Approval performance test " + i);

            ResponseEntity<LeaveRequestResponse> response = restTemplate.postForEntity(
                    baseUrl + "/leave-requests", request, LeaveRequestResponse.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            LeaveRequestResponse responseBody = response.getBody();
            assertThat(responseBody).isNotNull();
            requestIds.add(responseBody.getRequestId());
        }

        // Performance test: Concurrent approvals
        ExecutorService executor = Executors.newFixedThreadPool(5);
        AtomicInteger approvalSuccessCount = new AtomicInteger(0);
        AtomicInteger approvalErrorCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (String requestId : requestIds) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    ApprovalRequest approvalRequest = new ApprovalRequest();
                    approvalRequest.setApproverId("APPROVAL_MGR");
                    approvalRequest.setComments("Concurrent approval test");

                    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                    org.springframework.http.HttpEntity<ApprovalRequest> entity = 
                            new org.springframework.http.HttpEntity<>(approvalRequest, headers);

                    ResponseEntity<LeaveRequestResponse> response = restTemplate.exchange(
                            baseUrl + "/leave-requests/" + requestId + "/approve",
                            org.springframework.http.HttpMethod.PUT, entity, LeaveRequestResponse.class);
                    
                    if (response.getStatusCode() == HttpStatus.OK) {
                        approvalSuccessCount.incrementAndGet();
                    } else {
                        approvalErrorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    approvalErrorCount.incrementAndGet();
                }
            }, executor);
            
            futures.add(future);
        }
        
        // Wait for all operations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        
        // Performance assertions
        assertThat(approvalSuccessCount.get()).isEqualTo(numberOfRequests);
        assertThat(approvalErrorCount.get()).isEqualTo(0);
        assertThat(totalTime).isLessThan(10000); // Should complete within 10 seconds
        
        // Verify no pending requests remain
        ResponseEntity<List> pendingResponse = restTemplate.getForEntity(
                baseUrl + "/leave-requests/pending?managerId=APPROVAL_MGR", List.class);
        assertThat(pendingResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Object> pendingList = (List<Object>) pendingResponse.getBody();
        assertThat(pendingList).isNotNull();
        assertThat(pendingResponse.getBody()).isEmpty();
        
        System.out.println("Concurrent approval operations performance:");
        System.out.println("- Total approvals: " + numberOfRequests);
        System.out.println("- Success count: " + approvalSuccessCount.get());
        System.out.println("- Error count: " + approvalErrorCount.get());
        System.out.println("- Total time: " + totalTime + "ms");
        System.out.println("- Average time per approval: " + (totalTime / numberOfRequests) + "ms");
    }

    @Test
    @DisplayName("Database connection pool stress test")
    void testDatabaseConnectionPoolStressTest() throws Exception {
        // Setup: Create multiple managers and employees
        int numberOfManagers = 5;
        int employeesPerManager = 10;
        
        for (int i = 1; i <= numberOfManagers; i++) {
            createEmployee("STRESS_MGR_" + i, "Stress Manager " + i, 
                    "stress.mgr." + i + "@company.com", "Engineering", 
                    LocalDate.of(2020, 1, 1), null);
            
            for (int j = 1; j <= employeesPerManager; j++) {
                String empId = "STRESS_EMP_" + i + "_" + j;
                createEmployee(empId, "Stress Employee " + i + "_" + j, 
                        empId.toLowerCase() + "@company.com", "Engineering", 
                        LocalDate.of(2023, 1, 1), "STRESS_MGR_" + i);
                initializeLeaveBalance(empId, 2024);
            }
        }

        // Stress test: Multiple concurrent operations
        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger totalOperations = new AtomicInteger(0);
        AtomicInteger successfulOperations = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // Mix of different operations
        for (int i = 1; i <= numberOfManagers; i++) {
            final int managerId = i; // Make effectively final for lambda
            for (int j = 1; j <= employeesPerManager; j++) {
                final int empIndex = j; // Make effectively final for lambda
                final String empId = "STRESS_EMP_" + managerId + "_" + empIndex;
                final String mgrId = "STRESS_MGR_" + managerId;
                
                // Leave application
                CompletableFuture<Void> applyFuture = CompletableFuture.runAsync(() -> {
                    try {
                        totalOperations.incrementAndGet();
                        LeaveApplicationRequest request = new LeaveApplicationRequest();
                        request.setEmployeeId(empId);
                        request.setLeaveType(LeaveType.VACATION);
                        request.setStartDate(LocalDate.now().plusDays(30 + empIndex));
                        request.setEndDate(LocalDate.now().plusDays(32 + empIndex));
                        request.setDuration(LeaveDuration.FULL_DAY);
                        request.setReason("Stress test leave");

                        ResponseEntity<LeaveRequestResponse> response = restTemplate.postForEntity(
                                baseUrl + "/leave-requests", request, LeaveRequestResponse.class);
                        
                        if (response.getStatusCode() == HttpStatus.CREATED) {
                            successfulOperations.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // Expected under stress conditions
                    }
                }, executor);
                
                // Balance check
                CompletableFuture<Void> balanceFuture = CompletableFuture.runAsync(() -> {
                    try {
                        totalOperations.incrementAndGet();
                        ResponseEntity<List> response = restTemplate.getForEntity(
                                baseUrl + "/leave-balances/employee/" + empId, List.class);
                        @SuppressWarnings("unchecked")
                        List<Object> balanceList = (List<Object>) response.getBody();
                        
                        if (response.getStatusCode() == HttpStatus.OK) {
                            successfulOperations.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // Expected under stress conditions
                    }
                }, executor);
                
                // Pending requests check
                CompletableFuture<Void> pendingFuture = CompletableFuture.runAsync(() -> {
                    try {
                        totalOperations.incrementAndGet();
                        ResponseEntity<List> response = restTemplate.getForEntity(
                                baseUrl + "/leave-requests/pending?managerId=" + mgrId, List.class);
                        @SuppressWarnings("unchecked")
                        List<Object> pendingList = (List<Object>) response.getBody();
                        
                        if (response.getStatusCode() == HttpStatus.OK) {
                            successfulOperations.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // Expected under stress conditions
                    }
                }, executor);
                
                futures.add(applyFuture);
                futures.add(balanceFuture);
                futures.add(pendingFuture);
            }
        }
        
        // Wait for all operations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);
        
        // Calculate success rate
        double successRate = (double) successfulOperations.get() / totalOperations.get() * 100;
        
        // Performance assertions (allow for some failures under stress)
        assertThat(successRate).isGreaterThan(80.0); // At least 80% success rate
        assertThat(totalTime).isLessThan(60000); // Should complete within 60 seconds
        
        System.out.println("Database connection pool stress test:");
        System.out.println("- Total operations: " + totalOperations.get());
        System.out.println("- Successful operations: " + successfulOperations.get());
        System.out.println("- Success rate: " + String.format("%.2f%%", successRate));
        System.out.println("- Total time: " + totalTime + "ms");
        System.out.println("- Operations per second: " + (totalOperations.get() * 1000 / totalTime));
    }

    // Helper methods
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
}