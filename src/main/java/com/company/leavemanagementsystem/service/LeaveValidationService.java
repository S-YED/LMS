package com.company.leavemanagementsystem.service;

import com.company.leavemanagementsystem.entity.*;
import com.company.leavemanagementsystem.repository.EmployeeRepository;
import com.company.leavemanagementsystem.repository.LeaveBalanceRepository;
import com.company.leavemanagementsystem.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for comprehensive leave request validation.
 * Handles all business rule validations including date ranges, leave balances,
 * overlapping requests, and working day calculations.
 */
@Service
public class LeaveValidationService {

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    public LeaveValidationService(EmployeeRepository employeeRepository,
                                LeaveRequestRepository leaveRequestRepository,
                                LeaveBalanceRepository leaveBalanceRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
    }

    /**
     * Comprehensive validation for leave request
     * @param employeeId the employee ID
     * @param leaveType the leave type
     * @param startDate the start date
     * @param endDate the end date
     * @param duration the leave duration (full/half day)
     * @param isEmergencyLeave whether this is an emergency leave
     * @return ValidationResult containing validation status and messages
     */
    public ValidationResult validateLeaveRequest(String employeeId, LeaveType leaveType, 
                                               LocalDate startDate, LocalDate endDate, 
                                               LeaveDuration duration, boolean isEmergencyLeave) {
        return validateLeaveRequest(employeeId, leaveType, startDate, endDate, duration, isEmergencyLeave, null);
    }

    /**
     * Comprehensive validation for leave request with exclusion for updates
     * @param employeeId the employee ID
     * @param leaveType the leave type
     * @param startDate the start date
     * @param endDate the end date
     * @param duration the leave duration (full/half day)
     * @param isEmergencyLeave whether this is an emergency leave
     * @param excludeRequestId request ID to exclude from overlap check (for updates)
     * @return ValidationResult containing validation status and messages
     */
    public ValidationResult validateLeaveRequest(String employeeId, LeaveType leaveType, 
                                               LocalDate startDate, LocalDate endDate, 
                                               LeaveDuration duration, boolean isEmergencyLeave,
                                               String excludeRequestId) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 1. Validate date range (end date after start date)
        if (!isValidDateRange(startDate, endDate)) {
            errors.add("End date must be on or after start date");
        }

        // 2. Validate employee exists
        Optional<Employee> employeeOpt = employeeRepository.findByEmployeeId(employeeId);
        if (employeeOpt.isEmpty()) {
            errors.add("Employee not found with ID: " + employeeId);
            return new ValidationResult(false, errors, warnings);
        }

        Employee employee = employeeOpt.get();

        // 3. Validate leave after joining date
        if (!isLeaveAfterJoiningDate(startDate, employee.getJoiningDate())) {
            errors.add("Leave cannot be applied before joining date: " + employee.getJoiningDate());
        }

        // 4. Calculate working days for the request
        double requestedDays = calculateWorkingDays(startDate, endDate, duration);
        if (requestedDays <= 0) {
            errors.add("Leave request must include at least one working day");
        }

        // 5. Check leave balance (skip for emergency leave auto-approval)
        if (!isEmergencyLeave || requestedDays > 2.0) {
            ValidationResult balanceValidation = validateLeaveBalance(employeeId, leaveType, requestedDays);
            if (!balanceValidation.isValid()) {
                errors.addAll(balanceValidation.getErrors());
            }
            warnings.addAll(balanceValidation.getWarnings());
        }

        // 6. Check for overlapping leave requests
        ValidationResult overlapValidation = validateOverlappingLeaves(employeeId, startDate, endDate, excludeRequestId);
        if (!overlapValidation.isValid()) {
            errors.addAll(overlapValidation.getErrors());
        }

        // 7. Additional validations for backdated requests
        if (startDate.isBefore(LocalDate.now())) {
            ValidationResult backdatedValidation = validateBackdatedRequest(startDate);
            if (!backdatedValidation.isValid()) {
                errors.addAll(backdatedValidation.getErrors());
            }
            warnings.addAll(backdatedValidation.getWarnings());
        }

        // 8. Weekend-only request warning
        if (isWeekendOnlyRequest(startDate, endDate)) {
            warnings.add("Leave request covers only weekends - no working days will be deducted");
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    /**
     * Validate date range - end date must be on or after start date
     */
    public boolean isValidDateRange(LocalDate startDate, LocalDate endDate) {
        return startDate != null && endDate != null && !endDate.isBefore(startDate);
    }

    /**
     * Validate that leave is applied after employee joining date
     */
    public boolean isLeaveAfterJoiningDate(LocalDate leaveStartDate, LocalDate joiningDate) {
        return leaveStartDate != null && joiningDate != null && !leaveStartDate.isBefore(joiningDate);
    }

    /**
     * Calculate working days between start and end date, excluding weekends
     * @param startDate the start date
     * @param endDate the end date
     * @param duration the leave duration (full/half day)
     * @return total working days considering duration
     */
    public double calculateWorkingDays(LocalDate startDate, LocalDate endDate, LeaveDuration duration) {
        if (startDate == null || endDate == null || duration == null) {
            return 0.0;
        }

        long workingDays = 0;
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            // Skip weekends (Saturday = 6, Sunday = 7)
            if (current.getDayOfWeek().getValue() < 6) {
                workingDays++;
            }
            current = current.plusDays(1);
        }

        return workingDays * duration.getValue();
    }

    /**
     * Calculate working days between start and end date, excluding weekends and holidays
     * @param startDate the start date
     * @param endDate the end date
     * @param duration the leave duration (full/half day)
     * @param holidays list of holiday dates to exclude
     * @return total working days considering duration and holidays
     */
    public double calculateWorkingDaysExcludingHolidays(LocalDate startDate, LocalDate endDate, 
                                                       LeaveDuration duration, List<LocalDate> holidays) {
        if (startDate == null || endDate == null || duration == null) {
            return 0.0;
        }

        long workingDays = 0;
        LocalDate current = startDate;
        List<LocalDate> holidayList = holidays != null ? holidays : new ArrayList<>();

        while (!current.isAfter(endDate)) {
            // Skip weekends and holidays
            if (current.getDayOfWeek().getValue() < 6 && !holidayList.contains(current)) {
                workingDays++;
            }
            current = current.plusDays(1);
        }

        return workingDays * duration.getValue();
    }

    /**
     * Validate leave balance for the requested leave
     */
    public ValidationResult validateLeaveBalance(String employeeId, LeaveType leaveType, double requestedDays) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        int currentYear = LocalDate.now().getYear();
        Optional<LeaveBalance> balanceOpt = leaveBalanceRepository
            .findByEmployeeIdAndLeaveTypeAndYear(employeeId, leaveType, currentYear);

        if (balanceOpt.isEmpty()) {
            errors.add("Leave balance not found for " + leaveType.getDisplayName() + " in year " + currentYear);
            return new ValidationResult(false, errors, warnings);
        }

        LeaveBalance balance = balanceOpt.get();

        // Check if sufficient balance is available
        if (!balance.hasSufficientBalance(requestedDays)) {
            errors.add(String.format("Insufficient %s balance. Available: %.1f days, Requested: %.1f days", 
                leaveType.getDisplayName(), balance.getAvailableDays(), requestedDays));
        }

        // Warning for low balance after this request
        double remainingAfterRequest = balance.getAvailableDays() - requestedDays;
        if (remainingAfterRequest >= 0 && remainingAfterRequest < 5.0) {
            warnings.add(String.format("Low %s balance warning: %.1f days will remain after this request", 
                leaveType.getDisplayName(), remainingAfterRequest));
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    /**
     * Validate overlapping leave requests
     */
    public ValidationResult validateOverlappingLeaves(String employeeId, LocalDate startDate, LocalDate endDate, String excludeRequestId) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        List<LeaveRequest> overlappingRequests;
        
        if (excludeRequestId != null) {
            overlappingRequests = leaveRequestRepository
                .findOverlappingLeavesExcluding(employeeId, startDate, endDate, excludeRequestId);
        } else {
            overlappingRequests = leaveRequestRepository
                .findOverlappingLeaves(employeeId, startDate, endDate);
        }

        if (!overlappingRequests.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("Leave request overlaps with existing requests: ");
            for (int i = 0; i < overlappingRequests.size(); i++) {
                LeaveRequest overlap = overlappingRequests.get(i);
                if (i > 0) errorMsg.append(", ");
                errorMsg.append(String.format("%s (%s to %s, Status: %s)", 
                    overlap.getRequestId(), 
                    overlap.getStartDate(), 
                    overlap.getEndDate(), 
                    overlap.getStatus().getDisplayName()));
            }
            errors.add(errorMsg.toString());
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    /**
     * Validate backdated leave requests
     */
    public ValidationResult validateBackdatedRequest(LocalDate startDate) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        LocalDate today = LocalDate.now();
        long daysPast = ChronoUnit.DAYS.between(startDate, today);

        // Allow backdated requests up to 30 days
        if (daysPast > 30) {
            errors.add(String.format("Backdated leave requests are only allowed up to 30 days. " +
                "This request is %d days in the past", daysPast));
        } else if (daysPast > 0) {
            warnings.add(String.format("This is a backdated request (%d days ago). " +
                "Please provide proper justification", daysPast));
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    /**
     * Check if the request covers only weekends
     */
    public boolean isWeekendOnlyRequest(LocalDate startDate, LocalDate endDate) {
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            if (current.getDayOfWeek().getValue() < 6) { // Monday to Friday
                return false;
            }
            current = current.plusDays(1);
        }
        return true;
    }

    /**
     * Check if the request is for same day (emergency scenario)
     */
    public boolean isSameDayRequest(LocalDate startDate) {
        return startDate != null && startDate.equals(LocalDate.now());
    }

    /**
     * Validate emergency leave auto-approval eligibility
     */
    public ValidationResult validateEmergencyLeaveAutoApproval(double requestedDays, LocalDate startDate) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Emergency leave can be auto-approved if <= 2 days and same-day request
        if (requestedDays > 2.0) {
            warnings.add("Emergency leave exceeds 2 days - manager approval required");
        }

        if (!isSameDayRequest(startDate)) {
            warnings.add("Emergency leave is not for same day - manager approval may be required");
        }

        return new ValidationResult(true, errors, warnings);
    }

    /**
     * Get working days count between two dates (utility method)
     */
    public long getWorkingDaysBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return 0;
        }

        long workingDays = 0;
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            if (current.getDayOfWeek() != DayOfWeek.SATURDAY && current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                workingDays++;
            }
            current = current.plusDays(1);
        }

        return workingDays;
    }

    /**
     * Check if a specific date is a working day (not weekend)
     */
    public boolean isWorkingDay(LocalDate date) {
        return date != null && date.getDayOfWeek().getValue() < 6;
    }

    /**
     * Get next working day after a given date
     */
    public LocalDate getNextWorkingDay(LocalDate date) {
        if (date == null) return null;
        
        LocalDate nextDay = date.plusDays(1);
        while (!isWorkingDay(nextDay)) {
            nextDay = nextDay.plusDays(1);
        }
        return nextDay;
    }

    /**
     * Get previous working day before a given date
     */
    public LocalDate getPreviousWorkingDay(LocalDate date) {
        if (date == null) return null;
        
        LocalDate prevDay = date.minusDays(1);
        while (!isWorkingDay(prevDay)) {
            prevDay = prevDay.minusDays(1);
        }
        return prevDay;
    }

    /**
     * Validation result class to encapsulate validation outcome
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
            this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }

        public String getWarningMessage() {
            return String.join("; ", warnings);
        }

        @Override
        public String toString() {
            return "ValidationResult{" +
                    "valid=" + valid +
                    ", errors=" + errors +
                    ", warnings=" + warnings +
                    '}';
        }
    }
}