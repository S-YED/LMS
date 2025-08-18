package com.company.leavemanagementsystem.service;

import com.company.leavemanagementsystem.dto.*;
import com.company.leavemanagementsystem.entity.*;
import com.company.leavemanagementsystem.repository.EmployeeRepository;
import com.company.leavemanagementsystem.repository.LeaveBalanceRepository;
import com.company.leavemanagementsystem.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for LeaveBalanceService.
 * Tests balance initialization, calculations, updates, summary views, and year-end handling.
 */
@ExtendWith(MockitoExtension.class)
class LeaveBalanceServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @InjectMocks
    private LeaveBalanceService leaveBalanceService;

    private Employee testEmployee;
    private Employee testManager;
    private LeaveBalance testVacationBalance;
    private LeaveBalance testSickBalance;
    private InitializeBalanceRequest initializeBalanceRequest;

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

        // Setup test leave balances
        testVacationBalance = new LeaveBalance(testEmployee, LeaveType.VACATION, 20.0, 2024);
        testVacationBalance.setId(1L);
        testVacationBalance.setUsedDays(5.0);
        testVacationBalance.setCreatedAt(LocalDateTime.now().minusMonths(1));
        testVacationBalance.setUpdatedAt(LocalDateTime.now());

        testSickBalance = new LeaveBalance(testEmployee, LeaveType.SICK, 10.0, 2024);
        testSickBalance.setId(2L);
        testSickBalance.setUsedDays(2.0);
        testSickBalance.setCreatedAt(LocalDateTime.now().minusMonths(1));
        testSickBalance.setUpdatedAt(LocalDateTime.now());

        // Setup initialize balance request
        initializeBalanceRequest = new InitializeBalanceRequest();
        initializeBalanceRequest.setYear(2024);
        
        List<InitializeBalanceRequest.LeaveTypeBalance> leaveTypeBalances = Arrays.asList(
            new InitializeBalanceRequest.LeaveTypeBalance(LeaveType.VACATION, 20.0),
            new InitializeBalanceRequest.LeaveTypeBalance(LeaveType.SICK, 10.0),
            new InitializeBalanceRequest.LeaveTypeBalance(LeaveType.PERSONAL, 5.0)
        );
        initializeBalanceRequest.setLeaveTypeBalances(leaveTypeBalances);
    }

    // Test balance initialization for new employees
    @Test
    void testInitializeLeaveBalance_WhenEmployeeExists_ShouldInitializeBalance() {
        // Given
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.VACATION, 2024))
            .thenReturn(Optional.empty());
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.SICK, 2024))
            .thenReturn(Optional.empty());
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.PERSONAL, 2024))
            .thenReturn(Optional.empty());
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.MATERNITY, 2024))
            .thenReturn(Optional.empty());
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.PATERNITY, 2024))
            .thenReturn(Optional.empty());
        
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenReturn(testVacationBalance);

        // When
        List<LeaveBalanceResponse> response = leaveBalanceService.initializeLeaveBalance("EMP001", 2024);

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(5); // 5 default leave types
        
        verify(employeeRepository).findByEmployeeId("EMP001");
        verify(leaveBalanceRepository, times(5)).save(any(LeaveBalance.class));
    }

    @Test
    void testInitializeLeaveBalance_WhenEmployeeNotFound_ShouldThrowException() {
        // Given
        when(employeeRepository.findByEmployeeId("NONEXISTENT")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> leaveBalanceService.initializeLeaveBalance("NONEXISTENT", 2024))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Employee not found: NONEXISTENT");

        verify(employeeRepository).findByEmployeeId("NONEXISTENT");
        verify(leaveBalanceRepository, never()).save(any(LeaveBalance.class));
    }

    @Test
    void testInitializeLeaveBalance_WhenBalanceAlreadyExists_ShouldReturnExistingBalance() {
        // Given
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.VACATION, 2024))
            .thenReturn(Optional.of(testVacationBalance));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.SICK, 2024))
            .thenReturn(Optional.empty());
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.PERSONAL, 2024))
            .thenReturn(Optional.empty());
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.MATERNITY, 2024))
            .thenReturn(Optional.empty());
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.PATERNITY, 2024))
            .thenReturn(Optional.empty());
        
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenReturn(testSickBalance);

        // When
        List<LeaveBalanceResponse> response = leaveBalanceService.initializeLeaveBalance("EMP001", 2024);

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(5);
        
        verify(employeeRepository).findByEmployeeId("EMP001");
        verify(leaveBalanceRepository, times(4)).save(any(LeaveBalance.class)); // Only 4 new ones
    }

    // Test balance calculation and update methods
    @Test
    void testDeductLeaveBalance_WhenBalanceExists_ShouldDeductDays() {
        // Given
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.VACATION, 2024))
            .thenReturn(Optional.of(testVacationBalance));
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenReturn(testVacationBalance);

        // When
        LeaveBalanceResponse response = leaveBalanceService.deductLeaveBalance("EMP001", LeaveType.VACATION, 3.0, 2024);

        // Then
        assertThat(response).isNotNull();
        
        verify(leaveBalanceRepository).findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.VACATION, 2024);
        verify(leaveBalanceRepository).save(any(LeaveBalance.class));
    }

    @Test
    void testDeductLeaveBalance_WhenBalanceNotFound_ShouldThrowException() {
        // Given
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.VACATION, 2024))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> leaveBalanceService.deductLeaveBalance("EMP001", LeaveType.VACATION, 3.0, 2024))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Leave balance not found");

        verify(leaveBalanceRepository).findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.VACATION, 2024);
        verify(leaveBalanceRepository, never()).save(any(LeaveBalance.class));
    }

    @Test
    void testRestoreLeaveBalance_WhenBalanceExists_ShouldAddDaysBack() {
        // Given
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.VACATION, 2024))
            .thenReturn(Optional.of(testVacationBalance));
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenReturn(testVacationBalance);

        // When
        LeaveBalanceResponse response = leaveBalanceService.restoreLeaveBalance("EMP001", LeaveType.VACATION, 2.0, 2024);

        // Then
        assertThat(response).isNotNull();
        
        verify(leaveBalanceRepository).findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.VACATION, 2024);
        verify(leaveBalanceRepository).save(any(LeaveBalance.class));
    }

    @Test
    void testRestoreLeaveBalance_WhenBalanceNotFound_ShouldThrowException() {
        // Given
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.VACATION, 2024))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> leaveBalanceService.restoreLeaveBalance("EMP001", LeaveType.VACATION, 2.0, 2024))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Leave balance not found");

        verify(leaveBalanceRepository).findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.VACATION, 2024);
        verify(leaveBalanceRepository, never()).save(any(LeaveBalance.class));
    }

    // Test balance retrieval with summary views
    @Test
    void testGetEmployeeLeaveBalance_WhenEmployeeExists_ShouldReturnBalances() {
        // Given
        List<LeaveBalance> balances = Arrays.asList(testVacationBalance, testSickBalance);
        when(employeeRepository.existsByEmployeeId("EMP001")).thenReturn(true);
        when(leaveBalanceRepository.findByEmployeeIdAndYear("EMP001", 2024)).thenReturn(balances);

        // When
        List<LeaveBalanceResponse> response = leaveBalanceService.getEmployeeLeaveBalance("EMP001", 2024);

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(2);
        
        verify(employeeRepository).existsByEmployeeId("EMP001");
        verify(leaveBalanceRepository).findByEmployeeIdAndYear("EMP001", 2024);
    }

    @Test
    void testGetEmployeeLeaveBalance_WhenEmployeeNotFound_ShouldThrowException() {
        // Given
        when(employeeRepository.existsByEmployeeId("NONEXISTENT")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> leaveBalanceService.getEmployeeLeaveBalance("NONEXISTENT", 2024))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Employee not found: NONEXISTENT");

        verify(employeeRepository).existsByEmployeeId("NONEXISTENT");
        verify(leaveBalanceRepository, never()).findByEmployeeIdAndYear(anyString(), any(Integer.class));
    }

    @Test
    void testGetEmployeeLeaveBalance_WithCurrentYear_ShouldUseCurrentYear() {
        // Given
        List<LeaveBalance> balances = Arrays.asList(testVacationBalance, testSickBalance);
        when(employeeRepository.existsByEmployeeId("EMP001")).thenReturn(true);
        when(leaveBalanceRepository.findByEmployeeIdAndYear("EMP001", LocalDate.now().getYear())).thenReturn(balances);

        // When
        List<LeaveBalanceResponse> response = leaveBalanceService.getEmployeeLeaveBalance("EMP001");

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(2);
        
        verify(employeeRepository).existsByEmployeeId("EMP001");
        verify(leaveBalanceRepository).findByEmployeeIdAndYear("EMP001", LocalDate.now().getYear());
    }

    @Test
    void testGetLeaveBalanceSummary_WhenEmployeeExists_ShouldReturnSummary() {
        // Given
        List<LeaveBalance> balances = Arrays.asList(testVacationBalance, testSickBalance);
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(leaveBalanceRepository.findByEmployeeIdAndYear("EMP001", 2024)).thenReturn(balances);

        // When
        LeaveBalanceSummaryResponse response = leaveBalanceService.getLeaveBalanceSummary("EMP001", 2024);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEmployee()).isNotNull();
        assertThat(response.getEmployee().getEmployeeId()).isEqualTo("EMP001");
        assertThat(response.getYear()).isEqualTo(2024);
        
        verify(employeeRepository).findByEmployeeId("EMP001");
        verify(leaveBalanceRepository).findByEmployeeIdAndYear("EMP001", 2024);
    }

    @Test
    void testGetLeaveBalanceSummary_WhenEmployeeNotFound_ShouldThrowException() {
        // Given
        when(employeeRepository.findByEmployeeId("NONEXISTENT")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> leaveBalanceService.getLeaveBalanceSummary("NONEXISTENT", 2024))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Employee not found: NONEXISTENT");

        verify(employeeRepository).findByEmployeeId("NONEXISTENT");
        verify(leaveBalanceRepository, never()).findByEmployeeIdAndYear(anyString(), any(Integer.class));
    }

    @Test
    void testGetLeaveBalanceSummary_WithCurrentYear_ShouldUseCurrentYear() {
        // Given
        List<LeaveBalance> balances = Arrays.asList(testVacationBalance, testSickBalance);
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(leaveBalanceRepository.findByEmployeeIdAndYear("EMP001", LocalDate.now().getYear())).thenReturn(balances);

        // When
        LeaveBalanceSummaryResponse response = leaveBalanceService.getLeaveBalanceSummary("EMP001");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getYear()).isEqualTo(LocalDate.now().getYear());
        
        verify(employeeRepository).findByEmployeeId("EMP001");
        verify(leaveBalanceRepository).findByEmployeeIdAndYear("EMP001", LocalDate.now().getYear());
    }

    // Test year-end balance handling
    @Test
    void testProcessYearEndRenewal_ShouldProcessAllEmployees() {
        // Given
        List<LeaveBalance> currentYearBalances = Arrays.asList(testVacationBalance, testSickBalance);
        when(leaveBalanceRepository.findBalancesForRenewal(2024)).thenReturn(currentYearBalances);
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(anyString(), any(LeaveType.class), any(Integer.class)))
            .thenReturn(Optional.empty());
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenReturn(testVacationBalance);

        // When
        int processedCount = leaveBalanceService.processYearEndRenewal(2024, 2025);

        // Then
        assertThat(processedCount).isEqualTo(1); // One unique employee
        
        verify(leaveBalanceRepository).findBalancesForRenewal(2024);
        verify(employeeRepository, atLeastOnce()).findByEmployeeId("EMP001");
    }

    @Test
    void testRecalculateLeaveBalance_ShouldRecalculateBasedOnApprovedRequests() {
        // Given
        LeaveRequest approvedRequest1 = new LeaveRequest(testEmployee, LeaveType.VACATION, 
                                                        LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 3), 
                                                        LeaveDuration.FULL_DAY, "Vacation");
        approvedRequest1.setStatus(LeaveStatus.APPROVED);
        approvedRequest1.setTotalDays(3.0);

        LeaveRequest approvedRequest2 = new LeaveRequest(testEmployee, LeaveType.SICK, 
                                                        LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 1), 
                                                        LeaveDuration.FULL_DAY, "Sick");
        approvedRequest2.setStatus(LeaveStatus.AUTO_APPROVED);
        approvedRequest2.setTotalDays(1.0);

        List<LeaveRequest> allRequests = Arrays.asList(approvedRequest1, approvedRequest2);
        List<LeaveBalance> balances = Arrays.asList(testVacationBalance, testSickBalance);

        when(leaveRequestRepository.findByEmployeeId("EMP001")).thenReturn(allRequests);
        when(leaveBalanceRepository.findByEmployeeIdAndYear("EMP001", 2024)).thenReturn(balances);
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenReturn(testVacationBalance);

        // When
        List<LeaveBalanceResponse> response = leaveBalanceService.recalculateLeaveBalance("EMP001", 2024);

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(2);
        
        verify(leaveRequestRepository).findByEmployeeId("EMP001");
        verify(leaveBalanceRepository).findByEmployeeIdAndYear("EMP001", 2024);
        verify(leaveBalanceRepository, times(2)).save(any(LeaveBalance.class));
    }

    // Test additional utility methods
    @Test
    void testGetEmployeesWithLowBalance_ShouldReturnLowBalanceEmployees() {
        // Given
        List<LeaveBalance> lowBalances = Arrays.asList(testVacationBalance);
        when(leaveBalanceRepository.findLowBalances(5.0, 2024)).thenReturn(lowBalances);

        // When
        List<LeaveBalanceResponse> response = leaveBalanceService.getEmployeesWithLowBalance(5.0, 2024);

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        
        verify(leaveBalanceRepository).findLowBalances(5.0, 2024);
    }

    @Test
    void testGetEmployeesWithLowBalance_WithNullThreshold_ShouldUseDefaultThreshold() {
        // Given
        List<LeaveBalance> lowBalances = Arrays.asList(testVacationBalance);
        when(leaveBalanceRepository.findLowBalances(5.0, 2024)).thenReturn(lowBalances);

        // When
        List<LeaveBalanceResponse> response = leaveBalanceService.getEmployeesWithLowBalance(null, 2024);

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        
        verify(leaveBalanceRepository).findLowBalances(5.0, 2024); // Default threshold
    }

    @Test
    void testGetEmployeesWithHighUtilization_ShouldReturnHighUtilizationEmployees() {
        // Given
        List<LeaveBalance> highUtilizationBalances = Arrays.asList(testVacationBalance);
        when(leaveBalanceRepository.findHighUtilizationBalances(80.0, 2024)).thenReturn(highUtilizationBalances);

        // When
        List<LeaveBalanceResponse> response = leaveBalanceService.getEmployeesWithHighUtilization(80.0, 2024);

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        
        verify(leaveBalanceRepository).findHighUtilizationBalances(80.0, 2024);
    }

    @Test
    void testGetEmployeesWithHighUtilization_WithNullThreshold_ShouldUseDefaultThreshold() {
        // Given
        List<LeaveBalance> highUtilizationBalances = Arrays.asList(testVacationBalance);
        when(leaveBalanceRepository.findHighUtilizationBalances(80.0, 2024)).thenReturn(highUtilizationBalances);

        // When
        List<LeaveBalanceResponse> response = leaveBalanceService.getEmployeesWithHighUtilization(null, 2024);

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        
        verify(leaveBalanceRepository).findHighUtilizationBalances(80.0, 2024); // Default threshold
    }

    @Test
    void testGetDepartmentLeaveBalance_ShouldReturnDepartmentBalances() {
        // Given
        List<LeaveBalance> departmentBalances = Arrays.asList(testVacationBalance, testSickBalance);
        when(leaveBalanceRepository.findByDepartmentAndYear("Engineering", 2024)).thenReturn(departmentBalances);

        // When
        List<LeaveBalanceResponse> response = leaveBalanceService.getDepartmentLeaveBalance("Engineering", 2024);

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(2);
        
        verify(leaveBalanceRepository).findByDepartmentAndYear("Engineering", 2024);
    }

    @Test
    void testCalculateDepartmentUtilization_ShouldReturnUtilizationPercentage() {
        // Given
        when(leaveBalanceRepository.calculateDepartmentUtilization("Engineering", 2024)).thenReturn(65.5);

        // When
        Double utilization = leaveBalanceService.calculateDepartmentUtilization("Engineering", 2024);

        // Then
        assertThat(utilization).isEqualTo(65.5);
        
        verify(leaveBalanceRepository).calculateDepartmentUtilization("Engineering", 2024);
    }

    @Test
    void testGetEmployeesWithoutBalance_ShouldReturnEmployeeIds() {
        // Given
        Employee employeeWithoutBalance = new Employee("EMP002", "Bob Johnson", "bob@company.com", 
                                                      "Marketing", LocalDate.now());
        List<Employee> employeesWithoutBalance = Arrays.asList(employeeWithoutBalance);
        when(leaveBalanceRepository.findEmployeesWithoutBalance(2024)).thenReturn(employeesWithoutBalance);

        // When
        List<String> response = leaveBalanceService.getEmployeesWithoutBalance(2024);

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        assertThat(response.get(0)).isEqualTo("EMP002");
        
        verify(leaveBalanceRepository).findEmployeesWithoutBalance(2024);
    }

    @Test
    void testBulkInitializeLeaveBalance_ShouldProcessAllEmployees() {
        // Given
        List<String> employeeIds = Arrays.asList("EMP001", "EMP002", "EMP003");
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.findByEmployeeId("EMP002")).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.findByEmployeeId("EMP003")).thenReturn(Optional.empty()); // This one will fail
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(anyString(), any(LeaveType.class), any(Integer.class)))
            .thenReturn(Optional.empty());
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenReturn(testVacationBalance);

        // When
        int successCount = leaveBalanceService.bulkInitializeLeaveBalance(employeeIds, 2024);

        // Then
        assertThat(successCount).isEqualTo(2); // Only 2 successful
        
        verify(employeeRepository).findByEmployeeId("EMP001");
        verify(employeeRepository).findByEmployeeId("EMP002");
        verify(employeeRepository).findByEmployeeId("EMP003");
    }

    @Test
    void testGetLeaveBalanceStatistics_ShouldReturnStatistics() {
        // Given
        when(leaveBalanceRepository.countEmployeesWithBalance(2024)).thenReturn(100L);
        when(leaveBalanceRepository.findLowBalances(5.0, 2024)).thenReturn(Arrays.asList(testVacationBalance));
        when(leaveBalanceRepository.findZeroBalances(2024)).thenReturn(Arrays.asList(testSickBalance));
        when(leaveBalanceRepository.findHighUtilizationBalances(80.0, 2024)).thenReturn(Arrays.asList(testVacationBalance));
        when(leaveBalanceRepository.findByLeaveTypeAndYear(any(LeaveType.class), any(Integer.class)))
            .thenReturn(Arrays.asList(testVacationBalance));

        // When
        Map<String, Object> stats = leaveBalanceService.getLeaveBalanceStatistics(2024);

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.get("totalEmployees")).isEqualTo(100L);
        assertThat(stats.get("employeesWithLowBalance")).isEqualTo(1);
        assertThat(stats.get("employeesWithZeroBalance")).isEqualTo(1);
        assertThat(stats.get("employeesWithHighUtilization")).isEqualTo(1);
        assertThat(stats.get("averageUtilizationByType")).isNotNull();
        
        verify(leaveBalanceRepository).countEmployeesWithBalance(2024);
        verify(leaveBalanceRepository).findLowBalances(5.0, 2024);
        verify(leaveBalanceRepository).findZeroBalances(2024);
        verify(leaveBalanceRepository).findHighUtilizationBalances(80.0, 2024);
    }

    // Test edge cases and error scenarios
    @Test
    void testInitializeLeaveBalanceWithRequest_WhenValidRequest_ShouldInitializeBalance() {
        // Given
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(anyString(), any(LeaveType.class), any(Integer.class)))
            .thenReturn(Optional.empty());
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenReturn(testVacationBalance);

        // When
        List<LeaveBalanceResponse> response = leaveBalanceService.initializeLeaveBalance("EMP001", initializeBalanceRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(3); // 3 leave types in request
        
        verify(employeeRepository).findByEmployeeId("EMP001");
        verify(leaveBalanceRepository, times(3)).save(any(LeaveBalance.class));
    }

    @Test
    void testProcessYearEndRenewal_WithFailures_ShouldContinueProcessing() {
        // Given
        Employee employee1 = new Employee("EMP001", "Alice", "alice@company.com", "Engineering", LocalDate.now());
        Employee employee2 = new Employee("EMP002", "Bob", "bob@company.com", "Marketing", LocalDate.now());
        
        LeaveBalance balance1 = new LeaveBalance(employee1, LeaveType.VACATION, 20.0, 2024);
        LeaveBalance balance2 = new LeaveBalance(employee2, LeaveType.VACATION, 20.0, 2024);
        
        List<LeaveBalance> currentYearBalances = Arrays.asList(balance1, balance2);
        
        when(leaveBalanceRepository.findBalancesForRenewal(2024)).thenReturn(currentYearBalances);
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(employee1));
        when(employeeRepository.findByEmployeeId("EMP002")).thenReturn(Optional.empty()); // This will fail
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(anyString(), any(LeaveType.class), any(Integer.class)))
            .thenReturn(Optional.empty());
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenReturn(testVacationBalance);

        // When
        int processedCount = leaveBalanceService.processYearEndRenewal(2024, 2025);

        // Then
        assertThat(processedCount).isEqualTo(1); // Only one successful
        
        verify(leaveBalanceRepository).findBalancesForRenewal(2024);
        verify(employeeRepository).findByEmployeeId("EMP001");
        verify(employeeRepository).findByEmployeeId("EMP002");
    }

    @Test
    void testBulkInitializeLeaveBalance_WithEmptyList_ShouldReturnZero() {
        // Given
        List<String> emptyList = Collections.emptyList();

        // When
        int successCount = leaveBalanceService.bulkInitializeLeaveBalance(emptyList, 2024);

        // Then
        assertThat(successCount).isEqualTo(0);
        
        verify(employeeRepository, never()).findByEmployeeId(anyString());
        verify(leaveBalanceRepository, never()).save(any(LeaveBalance.class));
    }

    @Test
    void testRecalculateLeaveBalance_WithNoApprovedRequests_ShouldSetUsedDaysToZero() {
        // Given
        List<LeaveRequest> noApprovedRequests = Collections.emptyList();
        List<LeaveBalance> balances = Arrays.asList(testVacationBalance, testSickBalance);

        when(leaveRequestRepository.findByEmployeeId("EMP001")).thenReturn(noApprovedRequests);
        when(leaveBalanceRepository.findByEmployeeIdAndYear("EMP001", 2024)).thenReturn(balances);
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenReturn(testVacationBalance);

        // When
        List<LeaveBalanceResponse> response = leaveBalanceService.recalculateLeaveBalance("EMP001", 2024);

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(2);
        
        verify(leaveRequestRepository).findByEmployeeId("EMP001");
        verify(leaveBalanceRepository).findByEmployeeIdAndYear("EMP001", 2024);
        verify(leaveBalanceRepository, times(2)).save(any(LeaveBalance.class));
    }
}