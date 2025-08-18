package com.company.leavemanagementsystem.service;

import com.company.leavemanagementsystem.entity.*;
import com.company.leavemanagementsystem.repository.EmployeeRepository;
import com.company.leavemanagementsystem.repository.LeaveRequestRepository;
import com.company.leavemanagementsystem.service.ApprovalDelegationService.ApprovalAvailabilityResult;
import com.company.leavemanagementsystem.service.ApprovalDelegationService.AuthorizationResult;
import com.company.leavemanagementsystem.service.ApprovalDelegationService.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for ApprovalDelegationService.
 * Tests approval workflows, delegation logic, and authorization validation with mocking.
 */
@ExtendWith(MockitoExtension.class)
class ApprovalDelegationServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @InjectMocks
    private ApprovalDelegationService approvalDelegationService;

    private Employee employee;
    private Employee manager;
    private Employee seniorManager;
    private Employee hrRep;
    private Employee departmentManager;
    private LeaveRequest leaveRequest;
    private LeaveRequest managerLeaveRequest;

    @BeforeEach
    void setUp() {
        // Create employee hierarchy
        seniorManager = new Employee("SM001", "Senior Manager", "senior@company.com", 
                                    "Engineering", LocalDate.of(2019, 1, 1));
        seniorManager.setId(1L);

        manager = new Employee("MGR001", "John Manager", "john.manager@company.com", 
                              "Engineering", LocalDate.of(2020, 1, 15));
        manager.setId(2L);
        manager.setManager(seniorManager);

        employee = new Employee("EMP001", "Alice Smith", "alice.smith@company.com", 
                               "Engineering", LocalDate.of(2021, 3, 10));
        employee.setId(3L);
        employee.setManager(manager);

        hrRep = new Employee("HR001", "HR Representative", "hr@company.com", 
                            "HR", LocalDate.of(2018, 1, 1));
        hrRep.setId(4L);
        // HR rep has no manager (top-level)

        departmentManager = new Employee("DM001", "Department Manager", "dm@company.com", 
                                        "Engineering", LocalDate.of(2019, 6, 1));
        departmentManager.setId(5L);

        // Create leave requests
        leaveRequest = new LeaveRequest(employee, LeaveType.VACATION, 
                                       LocalDate.of(2024, 4, 15), 
                                       LocalDate.of(2024, 4, 17), 
                                       LeaveDuration.FULL_DAY, "Vacation");
        leaveRequest.setStatus(LeaveStatus.PENDING);
        leaveRequest.setCreatedAt(LocalDateTime.now());

        managerLeaveRequest = new LeaveRequest(manager, LeaveType.VACATION, 
                                              LocalDate.now(), 
                                              LocalDate.now().plusDays(2), 
                                              LeaveDuration.FULL_DAY, "Manager vacation");
        managerLeaveRequest.setStatus(LeaveStatus.APPROVED);
    }

    @Test
    void testCheckManagerAvailability_WhenManagerAvailable_ShouldReturnAvailable() {
        // Given
        when(employeeRepository.findByEmployeeId("MGR001")).thenReturn(Optional.of(manager));
        when(leaveRequestRepository.findActiveLeaveRequests(any(LocalDate.class)))
            .thenReturn(Collections.emptyList());

        // When
        ApprovalAvailabilityResult result = approvalDelegationService.checkManagerAvailability("MGR001", LocalDate.now());

        // Then
        assertThat(result.isAvailable()).isTrue();
        assertThat(result.getPrimaryManager()).isEqualTo(manager);
        assertThat(result.getIssues()).isEmpty();
        verify(employeeRepository).findByEmployeeId("MGR001");
        verify(leaveRequestRepository).findActiveLeaveRequests(any(LocalDate.class));
    }

    @Test
    void testCheckManagerAvailability_WhenManagerNotFound_ShouldReturnUnavailable() {
        // Given
        when(employeeRepository.findByEmployeeId("NONEXISTENT")).thenReturn(Optional.empty());

        // When
        ApprovalAvailabilityResult result = approvalDelegationService.checkManagerAvailability("NONEXISTENT", LocalDate.now());

        // Then
        assertThat(result.isAvailable()).isFalse();
        assertThat(result.getPrimaryManager()).isNull();
        assertThat(result.getIssues()).contains("Manager not found with ID: NONEXISTENT");
        verify(employeeRepository).findByEmployeeId("NONEXISTENT");
    }

    @Test
    void testCheckManagerAvailability_WhenManagerOnLeave_ShouldReturnUnavailableWithAlternates() {
        // Given
        when(employeeRepository.findByEmployeeId("MGR001")).thenReturn(Optional.of(manager));
        when(leaveRequestRepository.findActiveLeaveRequests(any(LocalDate.class)))
            .thenReturn(Arrays.asList(managerLeaveRequest));
        when(employeeRepository.findByDepartment("Engineering"))
            .thenReturn(Arrays.asList(manager, departmentManager, employee));
        when(employeeRepository.countSubordinates("MGR001")).thenReturn(1L);
        when(employeeRepository.countSubordinates("DM001")).thenReturn(2L);
        when(employeeRepository.countSubordinates("EMP001")).thenReturn(0L);

        // When
        ApprovalAvailabilityResult result = approvalDelegationService.checkManagerAvailability("MGR001", LocalDate.now());

        // Then
        assertThat(result.isAvailable()).isFalse();
        assertThat(result.getPrimaryManager()).isEqualTo(manager);
        assertThat(result.getIssues()).contains("Primary manager is on leave during approval period");
        assertThat(result.hasAlternates()).isTrue();
        assertThat(result.getAlternateApprovers()).contains(seniorManager); // Manager's manager
    }

    @Test
    void testFindAlternateApprovers_WhenManagerHasManager_ShouldIncludeManagersManager() {
        // Given
        when(employeeRepository.findByDepartment("Engineering"))
            .thenReturn(Arrays.asList(manager, departmentManager, employee));
        when(employeeRepository.countSubordinates("MGR001")).thenReturn(1L);
        when(employeeRepository.countSubordinates("DM001")).thenReturn(2L);
        when(employeeRepository.countSubordinates("EMP001")).thenReturn(0L);

        // When
        List<Employee> alternates = approvalDelegationService.findAlternateApprovers(manager);

        // Then
        assertThat(alternates).contains(seniorManager); // Manager's manager should be first alternate
        verify(employeeRepository).findByDepartment("Engineering");
    }

    @Test
    void testFindAlternateApprovers_WhenNoManagersManager_ShouldIncludeDepartmentManagers() {
        // Given
        Employee topLevelManager = new Employee("TLM001", "Top Level Manager", "tlm@company.com", 
                                               "Engineering", LocalDate.of(2018, 1, 1));
        topLevelManager.setManager(null); // No manager above
        
        when(employeeRepository.findByDepartment("Engineering"))
            .thenReturn(Arrays.asList(topLevelManager, departmentManager, employee));
        when(employeeRepository.countSubordinates("TLM001")).thenReturn(0L);
        when(employeeRepository.countSubordinates("DM001")).thenReturn(2L);
        when(employeeRepository.countSubordinates("EMP001")).thenReturn(0L);
        when(employeeRepository.findByManagerIsNull()).thenReturn(Arrays.asList(hrRep));

        // When
        List<Employee> alternates = approvalDelegationService.findAlternateApprovers(topLevelManager);

        // Then
        assertThat(alternates).contains(departmentManager);
        verify(employeeRepository).findByDepartment("Engineering");
    }

    @Test
    void testFindDepartmentManagers_ShouldReturnEmployeesWithSubordinates() {
        // Given
        when(employeeRepository.findByDepartment("Engineering"))
            .thenReturn(Arrays.asList(manager, departmentManager, employee));
        when(employeeRepository.countSubordinates("MGR001")).thenReturn(1L);
        when(employeeRepository.countSubordinates("DM001")).thenReturn(2L);
        when(employeeRepository.countSubordinates("EMP001")).thenReturn(0L);

        // When
        List<Employee> managers = approvalDelegationService.findDepartmentManagers("Engineering");

        // Then
        assertThat(managers).hasSize(2);
        assertThat(managers).containsExactlyInAnyOrder(manager, departmentManager);
        assertThat(managers).doesNotContain(employee); // Employee has no subordinates
    }

    @Test
    void testValidateApprovalAuthorization_WhenDirectManager_ShouldReturnAuthorized() {
        // Given
        when(employeeRepository.findByEmployeeId("MGR001")).thenReturn(Optional.of(manager));

        // When
        AuthorizationResult result = approvalDelegationService.validateApprovalAuthorization("MGR001", leaveRequest);

        // Then
        assertThat(result.isAuthorized()).isTrue();
        assertThat(result.getApprover()).isEqualTo(manager);
        assertThat(result.getErrors()).isEmpty();
        verify(employeeRepository).findByEmployeeId("MGR001");
    }

    @Test
    void testValidateApprovalAuthorization_WhenApproverNotFound_ShouldReturnUnauthorized() {
        // Given
        when(employeeRepository.findByEmployeeId("NONEXISTENT")).thenReturn(Optional.empty());

        // When
        AuthorizationResult result = approvalDelegationService.validateApprovalAuthorization("NONEXISTENT", leaveRequest);

        // Then
        assertThat(result.isAuthorized()).isFalse();
        assertThat(result.getApprover()).isNull();
        assertThat(result.getErrors()).contains("Approver not found with ID: NONEXISTENT");
    }

    @Test
    void testValidateApprovalAuthorization_WhenSelfApproval_ShouldReturnUnauthorized() {
        // Given
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(employee));

        // When
        AuthorizationResult result = approvalDelegationService.validateApprovalAuthorization("EMP001", leaveRequest);

        // Then
        assertThat(result.isAuthorized()).isFalse();
        assertThat(result.getApprover()).isEqualTo(employee);
        assertThat(result.getErrors()).contains("Employees cannot approve their own leave requests");
    }

    @Test
    void testValidateApprovalAuthorization_WhenAlternateApprover_ShouldReturnAuthorizedWithWarning() {
        // Given
        when(employeeRepository.findByEmployeeId("SM001")).thenReturn(Optional.of(seniorManager));
        when(leaveRequestRepository.findActiveLeaveRequests(any(LocalDate.class)))
            .thenReturn(Arrays.asList(managerLeaveRequest)); // Manager is on leave
        when(employeeRepository.findByDepartment("Engineering"))
            .thenReturn(Arrays.asList(manager, departmentManager, employee));
        when(employeeRepository.countSubordinates("MGR001")).thenReturn(1L);
        when(employeeRepository.countSubordinates("DM001")).thenReturn(2L);
        when(employeeRepository.countSubordinates("EMP001")).thenReturn(0L);

        // When
        AuthorizationResult result = approvalDelegationService.validateApprovalAuthorization("SM001", leaveRequest);

        // Then
        assertThat(result.isAuthorized()).isTrue();
        assertThat(result.getApprover()).isEqualTo(seniorManager);
        assertThat(result.hasWarnings()).isTrue();
        assertThat(result.getWarnings()).anyMatch(warning -> warning.contains("alternate approver"));
    }

    @Test
    void testValidateApprovalAuthorization_WhenInManagementHierarchy_ShouldReturnAuthorizedWithWarning() {
        // Given
        when(employeeRepository.findByEmployeeId("SM001")).thenReturn(Optional.of(seniorManager));
        when(leaveRequestRepository.findActiveLeaveRequests(any(LocalDate.class)))
            .thenReturn(Collections.emptyList()); // Manager is available
        when(employeeRepository.findManagerHierarchy("EMP001"))
            .thenReturn(Arrays.asList(manager, seniorManager));

        // When
        AuthorizationResult result = approvalDelegationService.validateApprovalAuthorization("SM001", leaveRequest);

        // Then
        assertThat(result.isAuthorized()).isTrue();
        assertThat(result.getApprover()).isEqualTo(seniorManager);
        assertThat(result.hasWarnings()).isTrue();
        assertThat(result.getWarnings()).anyMatch(warning -> warning.contains("higher-level manager"));
    }

    @Test
    void testValidateApprovalAuthorization_WhenHRRepresentative_ShouldReturnAuthorizedWithWarning() {
        // Given
        when(employeeRepository.findByEmployeeId("HR001")).thenReturn(Optional.of(hrRep));
        when(leaveRequestRepository.findActiveLeaveRequests(any(LocalDate.class)))
            .thenReturn(Collections.emptyList());
        when(employeeRepository.findManagerHierarchy("EMP001"))
            .thenReturn(Arrays.asList(manager));

        // When
        AuthorizationResult result = approvalDelegationService.validateApprovalAuthorization("HR001", leaveRequest);

        // Then
        assertThat(result.isAuthorized()).isTrue();
        assertThat(result.getApprover()).isEqualTo(hrRep);
        assertThat(result.hasWarnings()).isTrue();
        assertThat(result.getWarnings()).anyMatch(warning -> warning.contains("HR representative"));
    }

    @Test
    void testValidateApprovalAuthorization_WhenUnauthorizedEmployee_ShouldReturnUnauthorized() {
        // Given
        Employee randomEmployee = new Employee("RAND001", "Random Employee", "random@company.com", 
                                              "Marketing", LocalDate.of(2022, 1, 1));
        randomEmployee.setManager(hrRep); // Has a manager, so not HR
        
        when(employeeRepository.findByEmployeeId("RAND001")).thenReturn(Optional.of(randomEmployee));
        when(leaveRequestRepository.findActiveLeaveRequests(any(LocalDate.class)))
            .thenReturn(Collections.emptyList());
        when(employeeRepository.findManagerHierarchy("EMP001"))
            .thenReturn(Arrays.asList(manager));

        // When
        AuthorizationResult result = approvalDelegationService.validateApprovalAuthorization("RAND001", leaveRequest);

        // Then
        assertThat(result.isAuthorized()).isFalse();
        assertThat(result.getApprover()).isEqualTo(randomEmployee);
        assertThat(result.getErrors()).anyMatch(error -> error.contains("not authorized to approve"));
    }

    @Test
    void testGetAppropriatApprover_WhenPrimaryManagerAvailable_ShouldReturnPrimaryManager() {
        // Given
        when(leaveRequestRepository.findActiveLeaveRequests(any(LocalDate.class)))
            .thenReturn(Collections.emptyList());

        // When
        Employee approver = approvalDelegationService.getAppropriatApprover(leaveRequest);

        // Then
        assertThat(approver).isEqualTo(manager);
    }

    @Test
    void testGetAppropriatApprover_WhenPrimaryManagerUnavailable_ShouldReturnAlternate() {
        // Given
        when(leaveRequestRepository.findActiveLeaveRequests(any(LocalDate.class)))
            .thenReturn(Arrays.asList(managerLeaveRequest));
        when(employeeRepository.findByDepartment("Engineering"))
            .thenReturn(Arrays.asList(manager, departmentManager, employee));
        when(employeeRepository.countSubordinates("MGR001")).thenReturn(1L);
        when(employeeRepository.countSubordinates("DM001")).thenReturn(2L);
        when(employeeRepository.countSubordinates("EMP001")).thenReturn(0L);

        // When
        Employee approver = approvalDelegationService.getAppropriatApprover(leaveRequest);

        // Then
        assertThat(approver).isEqualTo(seniorManager); // Manager's manager should be first alternate
    }

    @Test
    void testGetAppropriatApprover_WhenNoManager_ShouldReturnHR() {
        // Given
        Employee orphanEmployee = new Employee("ORPH001", "Orphan Employee", "orphan@company.com", 
                                              "Engineering", LocalDate.of(2022, 1, 1));
        orphanEmployee.setManager(null); // No manager
        
        LeaveRequest orphanRequest = new LeaveRequest(orphanEmployee, LeaveType.VACATION, 
                                                     LocalDate.of(2024, 4, 15), 
                                                     LocalDate.of(2024, 4, 17), 
                                                     LeaveDuration.FULL_DAY, "Vacation");
        
        when(employeeRepository.findByManagerIsNull()).thenReturn(Arrays.asList(hrRep));

        // When
        Employee approver = approvalDelegationService.getAppropriatApprover(orphanRequest);

        // Then
        assertThat(approver).isEqualTo(hrRep);
    }

    @Test
    void testCanAutoApprove_WhenEmergencyLeaveUnder2Days_ShouldReturnTrue() {
        // Given
        LeaveRequest emergencyRequest = new LeaveRequest(employee, LeaveType.EMERGENCY, 
                                                        LocalDate.now(), 
                                                        LocalDate.now().plusDays(1), 
                                                        LeaveDuration.FULL_DAY, "Emergency");
        emergencyRequest.setIsEmergencyLeave(true);

        // When
        boolean canAutoApprove = approvalDelegationService.canAutoApprove(emergencyRequest);

        // Then
        assertThat(canAutoApprove).isTrue();
    }

    @Test
    void testCanAutoApprove_WhenEmergencyLeaveOver2Days_ShouldReturnFalse() {
        // Given
        LeaveRequest emergencyRequest = new LeaveRequest(employee, LeaveType.EMERGENCY, 
                                                        LocalDate.now(), 
                                                        LocalDate.now().plusDays(4), 
                                                        LeaveDuration.FULL_DAY, "Emergency");
        emergencyRequest.setIsEmergencyLeave(true);

        // When
        boolean canAutoApprove = approvalDelegationService.canAutoApprove(emergencyRequest);

        // Then
        assertThat(canAutoApprove).isFalse();
    }

    @Test
    void testCanAutoApprove_WhenNotEmergencyLeave_ShouldReturnFalse() {
        // When
        boolean canAutoApprove = approvalDelegationService.canAutoApprove(leaveRequest);

        // Then
        assertThat(canAutoApprove).isFalse();
    }

    @Test
    void testGetPendingApprovalCount_ShouldReturnCorrectCount() {
        // Given
        when(leaveRequestRepository.countPendingRequestsByManager("MGR001")).thenReturn(3L);

        // When
        long count = approvalDelegationService.getPendingApprovalCount("MGR001");

        // Then
        assertThat(count).isEqualTo(3L);
        verify(leaveRequestRepository).countPendingRequestsByManager("MGR001");
    }

    @Test
    void testGetPendingRequestsForManager_ShouldReturnDirectAndAlternateRequests() {
        // Given
        LeaveRequest directRequest = new LeaveRequest(employee, LeaveType.VACATION, 
                                                     LocalDate.of(2024, 5, 1), 
                                                     LocalDate.of(2024, 5, 3), 
                                                     LeaveDuration.FULL_DAY, "Direct report vacation");
        directRequest.setStatus(LeaveStatus.PENDING);

        when(leaveRequestRepository.findPendingRequestsByManager("SM001"))
            .thenReturn(Arrays.asList(directRequest));
        when(leaveRequestRepository.findByStatus(LeaveStatus.PENDING))
            .thenReturn(Arrays.asList(leaveRequest, directRequest));
        when(leaveRequestRepository.findActiveLeaveRequests(any(LocalDate.class)))
            .thenReturn(Arrays.asList(managerLeaveRequest)); // Manager is on leave
        when(employeeRepository.findByDepartment("Engineering"))
            .thenReturn(Arrays.asList(manager, departmentManager, employee));
        when(employeeRepository.countSubordinates("MGR001")).thenReturn(1L);
        when(employeeRepository.countSubordinates("DM001")).thenReturn(2L);
        when(employeeRepository.countSubordinates("EMP001")).thenReturn(0L);

        // When
        List<LeaveRequest> requests = approvalDelegationService.getPendingRequestsForManager("SM001");

        // Then
        assertThat(requests).hasSize(2); // Direct request + alternate request
        assertThat(requests).contains(directRequest);
        assertThat(requests).contains(leaveRequest);
    }

    @Test
    void testValidateRequestProcessable_WhenValidPendingRequest_ShouldReturnValid() {
        // When
        ValidationResult result = approvalDelegationService.validateRequestProcessable(leaveRequest);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void testValidateRequestProcessable_WhenNullRequest_ShouldReturnInvalid() {
        // When
        ValidationResult result = approvalDelegationService.validateRequestProcessable(null);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("Leave request not found");
    }

    @Test
    void testValidateRequestProcessable_WhenNotPendingStatus_ShouldReturnInvalid() {
        // Given
        leaveRequest.setStatus(LeaveStatus.APPROVED);

        // When
        ValidationResult result = approvalDelegationService.validateRequestProcessable(leaveRequest);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(error -> error.contains("not in pending status"));
    }

    @Test
    void testValidateRequestProcessable_WhenOldRequest_ShouldReturnValidWithWarning() {
        // Given
        leaveRequest.setCreatedAt(LocalDateTime.now().minusDays(35)); // 35 days old

        // When
        ValidationResult result = approvalDelegationService.validateRequestProcessable(leaveRequest);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.hasWarnings()).isTrue();
        assertThat(result.getWarnings()).anyMatch(warning -> warning.contains("older than 30 days"));
    }

    // Edge case tests
    @Test
    void testFindAlternateApprovers_WhenNoAlternatesAvailable_ShouldReturnHR() {
        // Given
        Employee isolatedManager = new Employee("ISO001", "Isolated Manager", "iso@company.com", 
                                               "NewDepartment", LocalDate.of(2020, 1, 1));
        isolatedManager.setManager(null); // No manager above
        
        when(employeeRepository.findByDepartment("NewDepartment"))
            .thenReturn(Arrays.asList(isolatedManager));
        when(employeeRepository.countSubordinates("ISO001")).thenReturn(0L);
        when(employeeRepository.findByManagerIsNull()).thenReturn(Arrays.asList(hrRep));

        // When
        List<Employee> alternates = approvalDelegationService.findAlternateApprovers(isolatedManager);

        // Then
        assertThat(alternates).contains(hrRep);
    }

    @Test
    void testGetAppropriatApprover_WhenNoHRAvailable_ShouldReturnNull() {
        // Given
        Employee orphanEmployee = new Employee("ORPH001", "Orphan Employee", "orphan@company.com", 
                                              "Engineering", LocalDate.of(2022, 1, 1));
        orphanEmployee.setManager(null);
        
        LeaveRequest orphanRequest = new LeaveRequest(orphanEmployee, LeaveType.VACATION, 
                                                     LocalDate.of(2024, 4, 15), 
                                                     LocalDate.of(2024, 4, 17), 
                                                     LeaveDuration.FULL_DAY, "Vacation");
        
        when(employeeRepository.findByManagerIsNull()).thenReturn(Collections.emptyList());

        // When
        Employee approver = approvalDelegationService.getAppropriatApprover(orphanRequest);

        // Then
        assertThat(approver).isNull();
    }
}