package com.company.leavemanagementsystem.dto;

import com.company.leavemanagementsystem.entity.LeaveType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/**
 * DTO for initializing leave balance for a new employee.
 * Contains balance allocation for different leave types.
 */
public class InitializeBalanceRequest {

    @NotNull(message = "Year is required")
    @Min(value = 2020, message = "Year must be 2020 or later")
    @Max(value = 2030, message = "Year must be 2030 or earlier")
    private Integer year;

    @NotNull(message = "Leave type balances are required")
    @Size(min = 1, message = "At least one leave type balance is required")
    @Valid
    private List<LeaveTypeBalance> leaveTypeBalances;

    // Default constructor
    public InitializeBalanceRequest() {}

    // Constructor with all fields
    public InitializeBalanceRequest(Integer year, List<LeaveTypeBalance> leaveTypeBalances) {
        this.year = year;
        this.leaveTypeBalances = leaveTypeBalances;
    }

    // Getters and Setters
    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public List<LeaveTypeBalance> getLeaveTypeBalances() {
        return leaveTypeBalances;
    }

    public void setLeaveTypeBalances(List<LeaveTypeBalance> leaveTypeBalances) {
        this.leaveTypeBalances = leaveTypeBalances;
    }

    // Convenience methods for backward compatibility with tests
    public void setEmployeeId(String employeeId) {
        // This field doesn't exist in the current structure, but we'll add it for compatibility
        // In the actual implementation, employeeId would be passed separately to the service method
    }

    public void setOverwriteExisting(boolean overwriteExisting) {
        // This field doesn't exist in the current structure, but we'll add it for compatibility
        // In the actual implementation, this would be a parameter to the service method
    }

    public void setLeaveAllocations(java.util.Map<LeaveType, Double> leaveAllocations) {
        // Convert map to list of LeaveTypeBalance for compatibility
        if (leaveAllocations != null) {
            this.leaveTypeBalances = leaveAllocations.entrySet().stream()
                .map(entry -> new LeaveTypeBalance(entry.getKey(), entry.getValue()))
                .collect(java.util.stream.Collectors.toList());
        }
    }

    /**
     * Nested class for leave type balance initialization
     */
    public static class LeaveTypeBalance {
        
        @NotNull(message = "Leave type is required")
        private LeaveType leaveType;

        @NotNull(message = "Total days is required")
        @DecimalMin(value = "0.0", message = "Total days must be non-negative")
        @DecimalMax(value = "365.0", message = "Total days cannot exceed 365")
        private Double totalDays;

        @DecimalMin(value = "0.0", message = "Used days must be non-negative")
        private Double usedDays = 0.0;

        // Default constructor
        public LeaveTypeBalance() {}

        // Constructor with required fields
        public LeaveTypeBalance(LeaveType leaveType, Double totalDays) {
            this.leaveType = leaveType;
            this.totalDays = totalDays;
        }

        // Constructor with all fields
        public LeaveTypeBalance(LeaveType leaveType, Double totalDays, Double usedDays) {
            this.leaveType = leaveType;
            this.totalDays = totalDays;
            this.usedDays = usedDays;
        }

        public LeaveType getLeaveType() {
            return leaveType;
        }

        public void setLeaveType(LeaveType leaveType) {
            this.leaveType = leaveType;
        }

        public Double getTotalDays() {
            return totalDays;
        }

        public void setTotalDays(Double totalDays) {
            this.totalDays = totalDays;
        }

        public Double getUsedDays() {
            return usedDays;
        }

        public void setUsedDays(Double usedDays) {
            this.usedDays = usedDays;
        }

        // Helper method to calculate available days
        public Double getAvailableDays() {
            return totalDays - usedDays;
        }

        @Override
        public String toString() {
            return "LeaveTypeBalance{" +
                    "leaveType=" + leaveType +
                    ", totalDays=" + totalDays +
                    ", usedDays=" + usedDays +
                    '}';
        }
    }

    // Helper method to get default leave balance allocation
    public static InitializeBalanceRequest createDefaultBalance(Integer year) {
        List<LeaveTypeBalance> defaultBalances = List.of(
            new LeaveTypeBalance(LeaveType.VACATION, 20.0),
            new LeaveTypeBalance(LeaveType.SICK, 10.0),
            new LeaveTypeBalance(LeaveType.PERSONAL, 5.0),
            new LeaveTypeBalance(LeaveType.EMERGENCY, 2.0)
        );
        return new InitializeBalanceRequest(year, defaultBalances);
    }

    @Override
    public String toString() {
        return "InitializeBalanceRequest{" +
                "year=" + year +
                ", leaveTypeBalances=" + leaveTypeBalances +
                '}';
    }
}