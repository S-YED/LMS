package com.company.leavemanagementsystem.repository;

import com.company.leavemanagementsystem.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test suite for LeaveRequestRepository.
 * Tests complex queries, overlapping leave detection, and edge cases.
 */
@DataJpaTest
@ActiveProfiles("test")
class LeaveRequestRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    private Employee manager;
    private Employee employee1;
    private Employee employee2;
    private Employee employee3;
    private LeaveRequest leaveRequest1;
    private LeaveRequest leaveRequest2;
    private LeaveRequest leaveRequest3;

    @BeforeEach
    void setUp() {
        // Create test employees
        manager = new Employee("MGR001", "John Manager", "john.manager@company.com", 
                              "Engineering", LocalDate.of(2020, 1, 15));
        
        employee1 = new Employee("EMP001", "Alice Smith", "alice.smith@company.com", 
                                "Engineering", LocalDate.of(2021, 3, 10));
        employee1.setManager(manager);
        
        employee2 = new Employee("EMP002", "Bob Johnson", "bob.johnson@company.com", 
                                "Engineering", LocalDate.of(2021, 6, 20));
        employee2.setManager(manager);
        
        employee3 = new Employee("EMP003", "Carol Davis", "carol.davis@company.com", 
                                "Marketing", LocalDate.of(2022, 1, 5));
        
        // Persist employees
        entityManager.persistAndFlush(manager);
        entityManager.persistAndFlush(employee1);
        entityManager.persistAndFlush(employee2);
        entityManager.persistAndFlush(employee3);

        // Create test leave requests
        leaveRequest1 = new LeaveRequest(employee1, LeaveType.VACATION, 
                                        LocalDate.of(2024, 3, 15), 
                                        LocalDate.of(2024, 3, 17), 
                                        LeaveDuration.FULL_DAY, "Family vacation");
        leaveRequest1.setStatus(LeaveStatus.APPROVED);
        leaveRequest1.setApprovedBy(manager);
        leaveRequest1.setApprovedAt(LocalDateTime.now());

        leaveRequest2 = new LeaveRequest(employee1, LeaveType.SICK, 
                                        LocalDate.of(2024, 4, 10), 
                                        LocalDate.of(2024, 4, 11), 
                                        LeaveDuration.FULL_DAY, "Medical appointment");
        leaveRequest2.setStatus(LeaveStatus.PENDING);

        leaveRequest3 = new LeaveRequest(employee2, LeaveType.EMERGENCY, 
                                        LocalDate.of(2024, 3, 20), 
                                        LocalDate.of(2024, 3, 20), 
                                        LeaveDuration.FULL_DAY, "Family emergency");
        leaveRequest3.setStatus(LeaveStatus.AUTO_APPROVED);
        leaveRequest3.setIsEmergencyLeave(true);

        // Persist leave requests
        entityManager.persistAndFlush(leaveRequest1);
        entityManager.persistAndFlush(leaveRequest2);
        entityManager.persistAndFlush(leaveRequest3);
    }

    @Test
    void testFindByRequestId_WhenRequestExists_ShouldReturnRequest() {
        // When
        Optional<LeaveRequest> result = leaveRequestRepository.findByRequestId(leaveRequest1.getRequestId());
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmployee().getEmployeeId()).isEqualTo("EMP001");
        assertThat(result.get().getLeaveType()).isEqualTo(LeaveType.VACATION);
    }

    @Test
    void testFindByRequestId_WhenRequestDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<LeaveRequest> result = leaveRequestRepository.findByRequestId("NONEXISTENT");
        
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testFindByEmployee_ShouldReturnEmployeeRequests() {
        // When
        List<LeaveRequest> results = leaveRequestRepository.findByEmployee(employee1);
        
        // Then
        assertThat(results).hasSize(2);
        assertThat(results)
            .extracting(LeaveRequest::getLeaveType)
            .containsExactlyInAnyOrder(LeaveType.VACATION, LeaveType.SICK);
    }

    @Test
    void testFindByEmployeeId_ShouldReturnEmployeeRequests() {
        // When
        List<LeaveRequest> results = leaveRequestRepository.findByEmployeeId("EMP001");
        
        // Then
        assertThat(results).hasSize(2);
        assertThat(results)
            .extracting(LeaveRequest::getLeaveType)
            .containsExactlyInAnyOrder(LeaveType.VACATION, LeaveType.SICK);
    }

    @Test
    void testFindByEmployeeId_WithPagination_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 1);
        
        // When
        Page<LeaveRequest> result = leaveRequestRepository.findByEmployeeId("EMP001", pageable);
        
        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void testFindByStatus_ShouldReturnRequestsWithStatus() {
        // When
        List<LeaveRequest> pendingRequests = leaveRequestRepository.findByStatus(LeaveStatus.PENDING);
        
        // Then
        assertThat(pendingRequests).hasSize(1);
        assertThat(pendingRequests.get(0).getEmployee().getEmployeeId()).isEqualTo("EMP001");
        assertThat(pendingRequests.get(0).getLeaveType()).isEqualTo(LeaveType.SICK);
    }

    @Test
    void testFindByStatus_WithPagination_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<LeaveRequest> result = leaveRequestRepository.findByStatus(LeaveStatus.APPROVED, pageable);
        
        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void testFindPendingRequestsByManager_ShouldReturnPendingRequestsForManager() {
        // When
        List<LeaveRequest> results = leaveRequestRepository.findPendingRequestsByManager("MGR001");
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEmployee().getEmployeeId()).isEqualTo("EMP001");
        assertThat(results.get(0).getStatus()).isEqualTo(LeaveStatus.PENDING);
    }

    @Test
    void testFindPendingRequestsByManager_WithPagination_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<LeaveRequest> result = leaveRequestRepository.findPendingRequestsByManager("MGR001", pageable);
        
        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void testFindOverlappingLeaves_WhenOverlapExists_ShouldReturnOverlappingRequests() {
        // Given - Create overlapping request dates
        LocalDate startDate = LocalDate.of(2024, 3, 16); // Overlaps with leaveRequest1 (15-17)
        LocalDate endDate = LocalDate.of(2024, 3, 18);
        
        // When
        List<LeaveRequest> results = leaveRequestRepository.findOverlappingLeaves("EMP001", startDate, endDate);
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRequestId()).isEqualTo(leaveRequest1.getRequestId());
    }

    @Test
    void testFindOverlappingLeaves_WhenNoOverlap_ShouldReturnEmptyList() {
        // Given - Non-overlapping dates
        LocalDate startDate = LocalDate.of(2024, 5, 1);
        LocalDate endDate = LocalDate.of(2024, 5, 3);
        
        // When
        List<LeaveRequest> results = leaveRequestRepository.findOverlappingLeaves("EMP001", startDate, endDate);
        
        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void testFindOverlappingLeavesExcluding_ShouldExcludeSpecificRequest() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 3, 16);
        LocalDate endDate = LocalDate.of(2024, 3, 18);
        
        // When - Exclude the overlapping request itself
        List<LeaveRequest> results = leaveRequestRepository.findOverlappingLeavesExcluding(
            "EMP001", startDate, endDate, leaveRequest1.getRequestId());
        
        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void testFindByEmployeeIdAndStatus_ShouldReturnMatchingRequests() {
        // When
        List<LeaveRequest> results = leaveRequestRepository.findByEmployeeIdAndStatus("EMP001", LeaveStatus.APPROVED);
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getLeaveType()).isEqualTo(LeaveType.VACATION);
    }

    @Test
    void testFindByEmployeeIdAndLeaveType_ShouldReturnMatchingRequests() {
        // When
        List<LeaveRequest> results = leaveRequestRepository.findByEmployeeIdAndLeaveType("EMP001", LeaveType.VACATION);
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(LeaveStatus.APPROVED);
    }

    @Test
    void testFindLeaveRequestsInDateRange_ShouldReturnRequestsInRange() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 3, 1);
        LocalDate endDate = LocalDate.of(2024, 3, 31);
        
        // When
        List<LeaveRequest> results = leaveRequestRepository.findLeaveRequestsInDateRange(startDate, endDate);
        
        // Then
        assertThat(results).hasSize(2); // leaveRequest1 and leaveRequest3
        assertThat(results)
            .extracting(lr -> lr.getEmployee().getEmployeeId())
            .containsExactlyInAnyOrder("EMP001", "EMP002");
    }

    @Test
    void testFindLeaveRequestsInDateRange_WithPagination_ShouldReturnPagedResults() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 3, 1);
        LocalDate endDate = LocalDate.of(2024, 3, 31);
        Pageable pageable = PageRequest.of(0, 1);
        
        // When
        Page<LeaveRequest> result = leaveRequestRepository.findLeaveRequestsInDateRange(startDate, endDate, pageable);
        
        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void testFindLeaveRequestsByDepartmentInDateRange_ShouldReturnDepartmentRequests() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 3, 1);
        LocalDate endDate = LocalDate.of(2024, 3, 31);
        
        // When
        List<LeaveRequest> results = leaveRequestRepository.findLeaveRequestsByDepartmentInDateRange(
            "Engineering", startDate, endDate);
        
        // Then
        assertThat(results).hasSize(2); // Both requests are from Engineering department
    }

    @Test
    void testFindEmergencyLeaveRequests_ShouldReturnEmergencyRequests() {
        // When
        List<LeaveRequest> results = leaveRequestRepository.findEmergencyLeaveRequests();
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getIsEmergencyLeave()).isTrue();
        assertThat(results.get(0).getEmployee().getEmployeeId()).isEqualTo("EMP002");
    }

    @Test
    void testFindBackdatedLeaveRequests_ShouldReturnBackdatedRequests() {
        // Given - Create a backdated request
        LeaveRequest backdatedRequest = new LeaveRequest(employee3, LeaveType.SICK, 
                                                        LocalDate.now().minusDays(5), 
                                                        LocalDate.now().minusDays(3), 
                                                        LeaveDuration.FULL_DAY, "Backdated sick leave");
        backdatedRequest.setIsBackdated(true);
        entityManager.persistAndFlush(backdatedRequest);
        
        // When
        List<LeaveRequest> results = leaveRequestRepository.findBackdatedLeaveRequests();
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getIsBackdated()).isTrue();
    }

    @Test
    void testFindRequestsNeedingApproval_ShouldReturnNonEmergencyPendingRequests() {
        // When
        List<LeaveRequest> results = leaveRequestRepository.findRequestsNeedingApproval();
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(results.get(0).getIsEmergencyLeave()).isFalse();
    }

    @Test
    void testFindAutoApprovedRequests_ShouldReturnAutoApprovedRequests() {
        // When
        List<LeaveRequest> results = leaveRequestRepository.findAutoApprovedRequests();
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(LeaveStatus.AUTO_APPROVED);
    }

    @Test
    void testCountPendingRequestsByManager_ShouldReturnCorrectCount() {
        // When
        long count = leaveRequestRepository.countPendingRequestsByManager("MGR001");
        
        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testCountByEmployeeIdAndStatus_ShouldReturnCorrectCount() {
        // When
        long count = leaveRequestRepository.countByEmployeeIdAndStatus("EMP001", LeaveStatus.APPROVED);
        
        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testFindRequestsApprovedBy_ShouldReturnRequestsApprovedByManager() {
        // When
        List<LeaveRequest> results = leaveRequestRepository.findRequestsApprovedBy("MGR001");
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getApprovedBy().getEmployeeId()).isEqualTo("MGR001");
    }

    @Test
    void testFindRequestsCreatedBetween_ShouldReturnRequestsInTimeRange() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now().plusHours(1);
        
        // When
        List<LeaveRequest> results = leaveRequestRepository.findRequestsCreatedBetween(startTime, endTime);
        
        // Then
        assertThat(results).hasSize(3); // All requests created in setup
    }

    @Test
    void testFindUpcomingLeaveRequests_ShouldReturnUpcomingApprovedRequests() {
        // Given - Create future approved request
        LeaveRequest futureRequest = new LeaveRequest(employee3, LeaveType.VACATION, 
                                                     LocalDate.now().plusDays(5), 
                                                     LocalDate.now().plusDays(7), 
                                                     LeaveDuration.FULL_DAY, "Future vacation");
        futureRequest.setStatus(LeaveStatus.APPROVED);
        entityManager.persistAndFlush(futureRequest);
        
        LocalDate endDate = LocalDate.now().plusDays(10);
        
        // When
        List<LeaveRequest> results = leaveRequestRepository.findUpcomingLeaveRequests(endDate);
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEmployee().getEmployeeId()).isEqualTo("EMP003");
    }

    @Test
    void testFindActiveLeaveRequests_ShouldReturnCurrentlyActiveRequests() {
        // Given - Create active request (today)
        LeaveRequest activeRequest = new LeaveRequest(employee3, LeaveType.SICK, 
                                                     LocalDate.now().minusDays(1), 
                                                     LocalDate.now().plusDays(1), 
                                                     LeaveDuration.FULL_DAY, "Active sick leave");
        activeRequest.setStatus(LeaveStatus.APPROVED);
        entityManager.persistAndFlush(activeRequest);
        
        // When
        List<LeaveRequest> results = leaveRequestRepository.findActiveLeaveRequests(LocalDate.now());
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEmployee().getEmployeeId()).isEqualTo("EMP003");
    }

    @Test
    void testFindByStatusIn_ShouldReturnRequestsWithAnyOfTheStatuses() {
        // Given
        List<LeaveStatus> statuses = Arrays.asList(LeaveStatus.APPROVED, LeaveStatus.AUTO_APPROVED);
        
        // When
        List<LeaveRequest> results = leaveRequestRepository.findByStatusIn(statuses);
        
        // Then
        assertThat(results).hasSize(2);
        assertThat(results)
            .extracting(LeaveRequest::getStatus)
            .containsExactlyInAnyOrder(LeaveStatus.APPROVED, LeaveStatus.AUTO_APPROVED);
    }

    @Test
    void testCalculateTotalDaysTakenInYear_ShouldReturnCorrectTotal() {
        // When
        Double totalDays = leaveRequestRepository.calculateTotalDaysTakenInYear("EMP001", 2024);
        
        // Then
        assertThat(totalDays).isEqualTo(3.0); // Only approved vacation request (3 days)
    }

    @Test
    void testCalculateTotalDaysTakenByTypeInYear_ShouldReturnCorrectTotalForType() {
        // When
        Double totalDays = leaveRequestRepository.calculateTotalDaysTakenByTypeInYear("EMP001", LeaveType.VACATION, 2024);
        
        // Then
        assertThat(totalDays).isEqualTo(3.0);
    }

    @Test
    void testFindUrgentLeaveRequests_ShouldReturnEmergencyAndSameDayRequests() {
        // Given - Create same-day request
        LeaveRequest sameDayRequest = new LeaveRequest(employee3, LeaveType.SICK, 
                                                      LocalDate.now(), 
                                                      LocalDate.now(), 
                                                      LeaveDuration.FULL_DAY, "Same day sick leave");
        sameDayRequest.setStatus(LeaveStatus.PENDING);
        entityManager.persistAndFlush(sameDayRequest);
        
        // When
        List<LeaveRequest> results = leaveRequestRepository.findUrgentLeaveRequests();
        
        // Then
        assertThat(results).hasSize(1); // Only the same-day request (emergency is auto-approved)
        assertThat(results.get(0).getStartDate()).isEqualTo(LocalDate.now());
    }

    // Edge case tests
    @Test
    void testFindByEmployeeId_WithNonExistentEmployee_ShouldReturnEmptyList() {
        // When
        List<LeaveRequest> results = leaveRequestRepository.findByEmployeeId("NONEXISTENT");
        
        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void testFindPendingRequestsByManager_WithNonExistentManager_ShouldReturnEmptyList() {
        // When
        List<LeaveRequest> results = leaveRequestRepository.findPendingRequestsByManager("NONEXISTENT");
        
        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void testFindOverlappingLeaves_WithRejectedStatus_ShouldNotReturnRejectedRequests() {
        // Given - Create rejected overlapping request
        LeaveRequest rejectedRequest = new LeaveRequest(employee1, LeaveType.PERSONAL, 
                                                       LocalDate.of(2024, 3, 16), 
                                                       LocalDate.of(2024, 3, 18), 
                                                       LeaveDuration.FULL_DAY, "Personal leave");
        rejectedRequest.setStatus(LeaveStatus.REJECTED);
        entityManager.persistAndFlush(rejectedRequest);
        
        // When
        List<LeaveRequest> results = leaveRequestRepository.findOverlappingLeaves("EMP001", 
                                                                                  LocalDate.of(2024, 3, 16), 
                                                                                  LocalDate.of(2024, 3, 18));
        
        // Then - Should only return approved request, not rejected one
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(LeaveStatus.APPROVED);
    }

    // Performance test
    @Test
    void testPerformanceWithLargeDataset() {
        // Given - Create 100 additional leave requests
        for (int i = 0; i < 100; i++) {
            LeaveRequest request = new LeaveRequest(employee1, LeaveType.VACATION, 
                                                   LocalDate.of(2024, 1, 1).plusDays(i * 7), 
                                                   LocalDate.of(2024, 1, 1).plusDays(i * 7 + 2), 
                                                   LeaveDuration.FULL_DAY, "Vacation " + i);
            request.setStatus(LeaveStatus.APPROVED);
            entityManager.persist(request);
        }
        entityManager.flush();
        
        // When - Measure performance of paginated query
        long startTime = System.currentTimeMillis();
        Page<LeaveRequest> result = leaveRequestRepository.findByEmployeeId("EMP001", PageRequest.of(0, 20));
        long endTime = System.currentTimeMillis();
        
        // Then
        assertThat(result.getContent()).hasSize(20);
        assertThat(result.getTotalElements()).isEqualTo(102); // 2 original + 100 new
        assertThat(endTime - startTime).isLessThan(1000); // Should complete within 1 second
    }
}