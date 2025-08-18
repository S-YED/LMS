package com.company.leavemanagementsystem.service;

import com.company.leavemanagementsystem.entity.*;
import com.company.leavemanagementsystem.repository.EmployeeRepository;
import com.company.leavemanagementsystem.repository.LeaveBalanceRepository;
import com.company.leavemanagementsystem.repository.LeaveRequestRepository;
import com.company.leavemanagementsystem.service.LeaveValidationService.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for LeaveValidationService.
 * Tests all validation methods, business rules, and edge cases with mocking.
 */
@ExtendWith(MockitoExtension.class)
class LeaveValidationServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @InjectMocks
    private LeaveValidationService leaveValidationService;

    private Employee testEmployee;
    private LeaveBalance testBalance;
    private LeaveRequest existingRequest;

    @BeforeEach
    void setUp() {
        testEmployee = new Employee("EMP001", "Alice Smith", "alice.smith@company.com", 
                                   "Engineering", LocalDate.of(2021, 3, 10));
        testEmployee.setId(1L);

        testBalance = new LeaveBalance(testEmployee, LeaveType.VACATION, 20.0, 2024);
        testBalance.setUsedDays(5.0);
        testBalance.setAvailableDays(15.0);

        existingRequest = new LeaveRequest(testEmployee, LeaveType.VACATION, 
                                          LocalDate.of(2024, 3, 15), 
                                          LocalDate.of(2024, 3, 17), 
                                          LeaveDuration.FULL_DAY, "Existing vacation");
        existingRequest.setStatus(LeaveStatus.APPROVED);
    }

    @Test
    void testValidateLeaveRequest_WhenAllValidationsPass_ShouldReturnValid() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 4, 15); // Monday
        LocalDate endDate = LocalDate.of(2024, 4, 17);   // Wednesday
        
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.VACATION, 2024))
            .thenReturn(Optional.of(testBalance));
        when(leaveRequestRepository.findOverlappingLeaves("EMP001", startDate, endDate))
            .thenReturn(Collections.emptyList());

        // When
        ValidationResult result = leaveValidationService.validateLeaveRequest(
            "EMP001", LeaveType.VACATION, startDate, endDate, LeaveDuration.FULL_DAY, false);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
        verify(employeeRepository).findByEmployeeId("EMP001");
        verify(leaveBalanceRepository).findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.VACATION, 2024);
        verify(leaveRequestRepository).findOverlappingLeaves("EMP001", startDate, endDate);
    }

    @Test
    void testValidateLeaveRequest_WhenEndDateBeforeStartDate_ShouldReturnInvalid() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 4, 17);
        LocalDate endDate = LocalDate.of(2024, 4, 15); // Before start date

        // When
        ValidationResult result = leaveValidationService.validateLeaveRequest(
            "EMP001", LeaveType.VACATION, startDate, endDate, LeaveDuration.FULL_DAY, false);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("End date must be on or after start date");
    }

    @Test
    void testValidateLeaveRequest_WhenEmployeeNotFound_ShouldReturnInvalid() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 4, 15);
        LocalDate endDate = LocalDate.of(2024, 4, 17);
        
        when(employeeRepository.findByEmployeeId("NONEXISTENT")).thenReturn(Optional.empty());

        // When
        ValidationResult result = leaveValidationService.validateLeaveRequest(
            "NONEXISTENT", LeaveType.VACATION, startDate, endDate, LeaveDuration.FULL_DAY, false);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("Employee not found with ID: NONEXISTENT");
        verify(employeeRepository).findByEmployeeId("NONEXISTENT");
    }

    @Test
    void testValidateLeaveRequest_WhenLeaveBeforeJoiningDate_ShouldReturnInvalid() {
        // Given
        LocalDate startDate = LocalDate.of(2021, 1, 15); // Before joining date (2021-03-10)
        LocalDate endDate = LocalDate.of(2021, 1, 17);
        
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));

        // When
        ValidationResult result = leaveValidationService.validateLeaveRequest(
            "EMP001", LeaveType.VACATION, startDate, endDate, LeaveDuration.FULL_DAY, false);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(error -> error.contains("Leave cannot be applied before joining date"));
        verify(employeeRepository).findByEmployeeId("EMP001");
    }

    @Test
    void testValidateLeaveRequest_WhenInsufficientBalance_ShouldReturnInvalid() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 4, 1); // Monday
        LocalDate endDate = LocalDate.of(2024, 4, 30);  // 22 working days (more than available 15)
        
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.VACATION, 2024))
            .thenReturn(Optional.of(testBalance));
        when(leaveRequestRepository.findOverlappingLeaves("EMP001", startDate, endDate))
            .thenReturn(Collections.emptyList());

        // When
        ValidationResult result = leaveValidationService.validateLeaveRequest(
            "EMP001", LeaveType.VACATION, startDate, endDate, LeaveDuration.FULL_DAY, false);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(error -> error.contains("Insufficient"));
    }

    @Test
    void testValidateLeaveRequest_WhenOverlappingLeaves_ShouldReturnInvalid() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 3, 16); // Overlaps with existing request
        LocalDate endDate = LocalDate.of(2024, 3, 18);
        
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.VACATION, 2024))
            .thenReturn(Optional.of(testBalance));
        when(leaveRequestRepository.findOverlappingLeaves("EMP001", startDate, endDate))
            .thenReturn(Arrays.asList(existingRequest));

        // When
        ValidationResult result = leaveValidationService.validateLeaveRequest(
            "EMP001", LeaveType.VACATION, startDate, endDate, LeaveDuration.FULL_DAY, false);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(error -> error.contains("overlaps with existing requests"));
    }

    @Test
    void testValidateLeaveRequest_WhenEmergencyLeaveUnder2Days_ShouldSkipBalanceCheck() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 4, 15);
        LocalDate endDate = LocalDate.of(2024, 4, 15); // 1 day emergency leave
        
        // Set balance to 0 to test that balance check is skipped
        testBalance.setAvailableDays(0.0);
        
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(leaveRequestRepository.findOverlappingLeaves("EMP001", startDate, endDate))
            .thenReturn(Collections.emptyList());

        // When
        ValidationResult result = leaveValidationService.validateLeaveRequest(
            "EMP001", LeaveType.EMERGENCY, startDate, endDate, LeaveDuration.FULL_DAY, true);

        // Then
        assertThat(result.isValid()).isTrue();
        // Balance check should be skipped for emergency leave <= 2 days
        verify(leaveBalanceRepository, never()).findByEmployeeIdAndLeaveTypeAndYear(anyString(), any(), any());
    }

    @Test
    void testValidateLeaveRequest_WhenBackdatedRequest_ShouldAddWarning() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(5); // 5 days ago
        LocalDate endDate = LocalDate.now().minusDays(3);
        
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.VACATION, LocalDate.now().getYear()))
            .thenReturn(Optional.of(testBalance));
        when(leaveRequestRepository.findOverlappingLeaves("EMP001", startDate, endDate))
            .thenReturn(Collections.emptyList());

        // When
        ValidationResult result = leaveValidationService.validateLeaveRequest(
            "EMP001", LeaveType.VACATION, startDate, endDate, LeaveDuration.FULL_DAY, false);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.hasWarnings()).isTrue();
        assertThat(result.getWarnings()).anyMatch(warning -> warning.contains("backdated request"));
    }

    @Test
    void testValidateLeaveRequest_WhenWeekendOnlyRequest_ShouldAddWarning() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 4, 13); // Saturday
        LocalDate endDate = LocalDate.of(2024, 4, 14);   // Sunday
        
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.VACATION, 2024))
            .thenReturn(Optional.of(testBalance));
        when(leaveRequestRepository.findOverlappingLeaves("EMP001", startDate, endDate))
            .thenReturn(Collections.emptyList());

        // When
        ValidationResult result = leaveValidationService.validateLeaveRequest(
            "EMP001", LeaveType.VACATION, startDate, endDate, LeaveDuration.FULL_DAY, false);

        // Then
        assertThat(result.isValid()).isFalse(); // Invalid because no working days
        assertThat(result.getErrors()).contains("Leave request must include at least one working day");
    }

    @Test
    void testIsValidDateRange_WithValidRange_ShouldReturnTrue() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 4, 15);
        LocalDate endDate = LocalDate.of(2024, 4, 17);

        // When
        boolean result = leaveValidationService.isValidDateRange(startDate, endDate);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void testIsValidDateRange_WithInvalidRange_ShouldReturnFalse() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 4, 17);
        LocalDate endDate = LocalDate.of(2024, 4, 15);

        // When
        boolean result = leaveValidationService.isValidDateRange(startDate, endDate);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void testIsValidDateRange_WithSameDate_ShouldReturnTrue() {
        // Given
        LocalDate date = LocalDate.of(2024, 4, 15);

        // When
        boolean result = leaveValidationService.isValidDateRange(date, date);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void testIsLeaveAfterJoiningDate_WithValidDate_ShouldReturnTrue() {
        // Given
        LocalDate joiningDate = LocalDate.of(2021, 3, 10);
        LocalDate leaveDate = LocalDate.of(2024, 4, 15);

        // When
        boolean result = leaveValidationService.isLeaveAfterJoiningDate(leaveDate, joiningDate);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void testIsLeaveAfterJoiningDate_WithInvalidDate_ShouldReturnFalse() {
        // Given
        LocalDate joiningDate = LocalDate.of(2021, 3, 10);
        LocalDate leaveDate = LocalDate.of(2021, 1, 15);

        // When
        boolean result = leaveValidationService.isLeaveAfterJoiningDate(leaveDate, joiningDate);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void testCalculateWorkingDays_WithFullWeek_ShouldReturn5Days() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 4, 15); // Monday
        LocalDate endDate = LocalDate.of(2024, 4, 19);   // Friday

        // When
        double result = leaveValidationService.calculateWorkingDays(startDate, endDate, LeaveDuration.FULL_DAY);

        // Then
        assertThat(result).isEqualTo(5.0);
    }

    @Test
    void testCalculateWorkingDays_WithHalfDays_ShouldReturn2Point5Days() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 4, 15); // Monday
        LocalDate endDate = LocalDate.of(2024, 4, 19);   // Friday

        // When
        double result = leaveValidationService.calculateWorkingDays(startDate, endDate, LeaveDuration.HALF_DAY);

        // Then
        assertThat(result).isEqualTo(2.5);
    }

    @Test
    void testCalculateWorkingDays_WithWeekend_ShouldExcludeWeekends() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 4, 13); // Saturday
        LocalDate endDate = LocalDate.of(2024, 4, 21);   // Sunday (next week)

        // When
        double result = leaveValidationService.calculateWorkingDays(startDate, endDate, LeaveDuration.FULL_DAY);

        // Then
        assertThat(result).isEqualTo(5.0); // Only weekdays count
    }

    @Test
    void testCalculateWorkingDaysExcludingHolidays_WithHolidays_ShouldExcludeHolidays() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 4, 15); // Monday
        LocalDate endDate = LocalDate.of(2024, 4, 19);   // Friday
        List<LocalDate> holidays = Arrays.asList(LocalDate.of(2024, 4, 17)); // Wednesday is holiday

        // When
        double result = leaveValidationService.calculateWorkingDaysExcludingHolidays(
            startDate, endDate, LeaveDuration.FULL_DAY, holidays);

        // Then
        assertThat(result).isEqualTo(4.0); // 5 weekdays - 1 holiday
    }

    @Test
    void testValidateLeaveBalance_WhenBalanceNotFound_ShouldReturnInvalid() {
        // Given
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.VACATION, LocalDate.now().getYear()))
            .thenReturn(Optional.empty());

        // When
        ValidationResult result = leaveValidationService.validateLeaveBalance("EMP001", LeaveType.VACATION, 5.0);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(error -> error.contains("Leave balance not found"));
    }

    @Test
    void testValidateLeaveBalance_WhenSufficientBalance_ShouldReturnValid() {
        // Given
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.VACATION, LocalDate.now().getYear()))
            .thenReturn(Optional.of(testBalance));

        // When
        ValidationResult result = leaveValidationService.validateLeaveBalance("EMP001", LeaveType.VACATION, 10.0);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void testValidateLeaveBalance_WhenLowBalanceAfterRequest_ShouldAddWarning() {
        // Given
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.VACATION, LocalDate.now().getYear()))
            .thenReturn(Optional.of(testBalance));

        // When - Request 13 days, leaving 2 days (< 5 days threshold)
        ValidationResult result = leaveValidationService.validateLeaveBalance("EMP001", LeaveType.VACATION, 13.0);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.hasWarnings()).isTrue();
        assertThat(result.getWarnings()).anyMatch(warning -> warning.contains("Low") && warning.contains("balance warning"));
    }

    @Test
    void testValidateBackdatedRequest_WhenWithinAllowedPeriod_ShouldReturnValidWithWarning() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(15); // 15 days ago (within 30 day limit)

        // When
        ValidationResult result = leaveValidationService.validateBackdatedRequest(startDate);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.hasWarnings()).isTrue();
        assertThat(result.getWarnings()).anyMatch(warning -> warning.contains("backdated request"));
    }

    @Test
    void testValidateBackdatedRequest_WhenBeyondAllowedPeriod_ShouldReturnInvalid() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(35); // 35 days ago (beyond 30 day limit)

        // When
        ValidationResult result = leaveValidationService.validateBackdatedRequest(startDate);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(error -> error.contains("Backdated leave requests are only allowed up to 30 days"));
    }

    @Test
    void testIsWeekendOnlyRequest_WithWeekendOnly_ShouldReturnTrue() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 4, 13); // Saturday
        LocalDate endDate = LocalDate.of(2024, 4, 14);   // Sunday

        // When
        boolean result = leaveValidationService.isWeekendOnlyRequest(startDate, endDate);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void testIsWeekendOnlyRequest_WithWeekdays_ShouldReturnFalse() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 4, 15); // Monday
        LocalDate endDate = LocalDate.of(2024, 4, 17);   // Wednesday

        // When
        boolean result = leaveValidationService.isWeekendOnlyRequest(startDate, endDate);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void testIsSameDayRequest_WithToday_ShouldReturnTrue() {
        // Given
        LocalDate today = LocalDate.now();

        // When
        boolean result = leaveValidationService.isSameDayRequest(today);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void testIsSameDayRequest_WithDifferentDay_ShouldReturnFalse() {
        // Given
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // When
        boolean result = leaveValidationService.isSameDayRequest(tomorrow);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void testValidateEmergencyLeaveAutoApproval_WithValidEmergencyLeave_ShouldReturnValidWithNoWarnings() {
        // Given
        LocalDate today = LocalDate.now();

        // When
        ValidationResult result = leaveValidationService.validateEmergencyLeaveAutoApproval(2.0, today);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.hasWarnings()).isFalse();
    }

    @Test
    void testValidateEmergencyLeaveAutoApproval_WithExcessiveDays_ShouldAddWarning() {
        // Given
        LocalDate today = LocalDate.now();

        // When
        ValidationResult result = leaveValidationService.validateEmergencyLeaveAutoApproval(3.0, today);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.hasWarnings()).isTrue();
        assertThat(result.getWarnings()).anyMatch(warning -> warning.contains("exceeds 2 days"));
    }

    @Test
    void testGetWorkingDaysBetween_WithValidRange_ShouldReturnCorrectCount() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 4, 15); // Monday
        LocalDate endDate = LocalDate.of(2024, 4, 19);   // Friday

        // When
        long result = leaveValidationService.getWorkingDaysBetween(startDate, endDate);

        // Then
        assertThat(result).isEqualTo(5);
    }

    @Test
    void testIsWorkingDay_WithWeekday_ShouldReturnTrue() {
        // Given
        LocalDate monday = LocalDate.of(2024, 4, 15);

        // When
        boolean result = leaveValidationService.isWorkingDay(monday);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void testIsWorkingDay_WithWeekend_ShouldReturnFalse() {
        // Given
        LocalDate saturday = LocalDate.of(2024, 4, 13);

        // When
        boolean result = leaveValidationService.isWorkingDay(saturday);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void testGetNextWorkingDay_FromFriday_ShouldReturnMonday() {
        // Given
        LocalDate friday = LocalDate.of(2024, 4, 19);

        // When
        LocalDate result = leaveValidationService.getNextWorkingDay(friday);

        // Then
        assertThat(result).isEqualTo(LocalDate.of(2024, 4, 22)); // Monday
    }

    @Test
    void testGetPreviousWorkingDay_FromMonday_ShouldReturnFriday() {
        // Given
        LocalDate monday = LocalDate.of(2024, 4, 22);

        // When
        LocalDate result = leaveValidationService.getPreviousWorkingDay(monday);

        // Then
        assertThat(result).isEqualTo(LocalDate.of(2024, 4, 19)); // Previous Friday
    }

    // Edge case tests
    @Test
    void testCalculateWorkingDays_WithNullValues_ShouldReturnZero() {
        // When
        double result = leaveValidationService.calculateWorkingDays(null, null, null);

        // Then
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    void testValidateLeaveRequest_WithExcludeRequestId_ShouldUseExclusionQuery() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 4, 15);
        LocalDate endDate = LocalDate.of(2024, 4, 17);
        String excludeRequestId = "LR-12345";
        
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear("EMP001", LeaveType.VACATION, 2024))
            .thenReturn(Optional.of(testBalance));
        when(leaveRequestRepository.findOverlappingLeavesExcluding("EMP001", startDate, endDate, excludeRequestId))
            .thenReturn(Collections.emptyList());

        // When
        ValidationResult result = leaveValidationService.validateLeaveRequest(
            "EMP001", LeaveType.VACATION, startDate, endDate, LeaveDuration.FULL_DAY, false, excludeRequestId);

        // Then
        assertThat(result.isValid()).isTrue();
        verify(leaveRequestRepository).findOverlappingLeavesExcluding("EMP001", startDate, endDate, excludeRequestId);
        verify(leaveRequestRepository, never()).findOverlappingLeaves(anyString(), any(), any());
    }
}