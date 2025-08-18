package com.company.leavemanagementsystem.service;

import com.company.leavemanagementsystem.dto.*;
import com.company.leavemanagementsystem.entity.*;
import com.company.leavemanagementsystem.repository.EmployeeRepository;
import com.company.leavemanagementsystem.repository.LeaveBalanceRepository;
import com.company.leavemanagementsystem.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for leave balance management operations.
 * Handles balance initialization, calculations, updates, summary views, and
 * year-end processing.
 */
@Service
@Transactional
public class LeaveBalanceService {

    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    // Default leave allocations per year
    private static final Map<LeaveType, Double> DEFAULT_LEAVE_ALLOCATIONS = Map.of(
            LeaveType.VACATION, 20.0,
            LeaveType.SICK, 10.0,
            LeaveType.PERSONAL, 5.0,
            LeaveType.MATERNITY, 90.0,
            LeaveType.PATERNITY, 15.0);

    @Autowired
    public LeaveBalanceService(EmployeeRepository employeeRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            LeaveRequestRepository leaveRequestRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    /**
     * Initialize leave balance for a new employee
     * 
     * @param employeeId the employee ID
     * @param year       the year to initialize balance for
     * @return List of LeaveBalanceResponse containing initialized balances
     * @throws IllegalArgumentException if employee not found
     */
    public List<LeaveBalanceResponse> initializeLeaveBalance(String employeeId, Integer year) {
        // Validate employee exists
        Employee employee = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        List<LeaveBalance> balances = new ArrayList<>();

        // Initialize balance for each leave type
        for (Map.Entry<LeaveType, Double> entry : DEFAULT_LEAVE_ALLOCATIONS.entrySet()) {
            LeaveType leaveType = entry.getKey();
            Double allocation = entry.getValue();

            // Check if balance already exists
            Optional<LeaveBalance> existingBalance = leaveBalanceRepository
                    .findByEmployeeIdAndLeaveTypeAndYear(employeeId, leaveType, year);

            if (existingBalance.isEmpty()) {
                LeaveBalance balance = new LeaveBalance(employee, leaveType, allocation, year);
                balances.add(leaveBalanceRepository.save(balance));
            } else {
                balances.add(existingBalance.get());
            }
        }

        return balances.stream()
                .map(this::convertToLeaveBalanceResponse)
                .collect(Collectors.toList());
    }

    /**
     * Initialize leave balance with custom allocations
     * 
     * @param employeeId the employee ID
     * @param request    the initialization request with custom allocations
     * @return List of LeaveBalanceResponse containing initialized balances
     */
    public List<LeaveBalanceResponse> initializeLeaveBalance(String employeeId, InitializeBalanceRequest request) {
        // Validate employee exists
        Employee employee = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        List<LeaveBalance> balances = new ArrayList<>();

        // Use the leave type balances from the request
        for (InitializeBalanceRequest.LeaveTypeBalance leaveTypeBalance : request.getLeaveTypeBalances()) {
            LeaveType leaveType = leaveTypeBalance.getLeaveType();
            Double allocation = leaveTypeBalance.getTotalDays();

            // Check if balance already exists
            Optional<LeaveBalance> existingBalance = leaveBalanceRepository
                    .findByEmployeeIdAndLeaveTypeAndYear(employeeId, leaveType, request.getYear());

            if (existingBalance.isEmpty()) {
                LeaveBalance balance = new LeaveBalance(employee, leaveType, allocation, request.getYear());
                balance.setUsedDays(leaveTypeBalance.getUsedDays());
                balances.add(leaveBalanceRepository.save(balance));
            } else {
                // Return existing balance
                balances.add(existingBalance.get());
            }
        }

        return balances.stream()
                .map(this::convertToLeaveBalanceResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get leave balance for an employee
     * 
     * @param employeeId the employee ID
     * @return List of LeaveBalanceResponse containing all leave balances
     */
    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getEmployeeLeaveBalance(String employeeId) {
        return getEmployeeLeaveBalance(employeeId, LocalDate.now().getYear());
    }

    /**
     * Get leave balance for an employee for a specific year
     * 
     * @param employeeId the employee ID
     * @param year       the year
     * @return List of LeaveBalanceResponse containing leave balances for the year
     */
    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getEmployeeLeaveBalance(String employeeId, Integer year) {
        // Validate employee exists
        if (!employeeRepository.existsByEmployeeId(employeeId)) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }

        List<LeaveBalance> balances = leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year);
        return balances.stream()
                .map(this::convertToLeaveBalanceResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get leave balance summary for an employee
     * 
     * @param employeeId the employee ID
     * @return LeaveBalanceSummaryResponse containing summary information
     */
    @Transactional(readOnly = true)
    public LeaveBalanceSummaryResponse getLeaveBalanceSummary(String employeeId) {
        return getLeaveBalanceSummary(employeeId, LocalDate.now().getYear());
    }

    /**
     * Get leave balance summary for an employee for a specific year
     * 
     * @param employeeId the employee ID
     * @param year       the year
     * @return LeaveBalanceSummaryResponse containing summary information
     */
    @Transactional(readOnly = true)
    public LeaveBalanceSummaryResponse getLeaveBalanceSummary(String employeeId, Integer year) {
        // Validate employee exists
        Employee employee = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        List<LeaveBalance> balances = leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year);

        // Calculate totals
        double totalAllocated = balances.stream().mapToDouble(LeaveBalance::getTotalDays).sum();
        double totalUsed = balances.stream().mapToDouble(LeaveBalance::getUsedDays).sum();
        double totalAvailable = balances.stream().mapToDouble(LeaveBalance::getAvailableDays).sum();

        // Find low balances
        List<LeaveBalanceResponse> lowBalances = balances.stream()
                .filter(LeaveBalance::isRunningLow)
                .map(this::convertToLeaveBalanceResponse)
                .collect(Collectors.toList());

        // Get individual balances
        List<LeaveBalanceResponse> leaveBalances = balances.stream()
                .map(this::convertToLeaveBalanceResponse)
                .collect(Collectors.toList());

        // Create employee info
        LeaveBalanceSummaryResponse.EmployeeInfo employeeInfo = new LeaveBalanceSummaryResponse.EmployeeInfo(employeeId,
                employee.getName(), employee.getDepartment());

        // Create summary totals
        LeaveBalanceSummaryResponse.SummaryTotals summaryTotals = new LeaveBalanceSummaryResponse.SummaryTotals(
                totalAllocated, totalUsed, totalAvailable,
                totalAllocated > 0 ? (totalUsed / totalAllocated) * 100 : 0.0, lowBalances.size());

        // Create leave type balances
        List<LeaveBalanceSummaryResponse.LeaveTypeBalance> leaveTypeBalances = balances.stream()
                .map(balance -> new LeaveBalanceSummaryResponse.LeaveTypeBalance(
                        balance.getLeaveType().name(),
                        balance.getLeaveType().getDisplayName(),
                        balance.getTotalDays(),
                        balance.getUsedDays(),
                        balance.getAvailableDays(),
                        balance.getUtilizationPercentage(),
                        balance.isRunningLow()))
                .collect(Collectors.toList());

        // Create warnings
        List<String> warnings = new ArrayList<>();
        if (!lowBalances.isEmpty()) {
            warnings.add("You have " + lowBalances.size() + " leave type(s) with low balance");
        }

        LeaveBalanceSummaryResponse response = new LeaveBalanceSummaryResponse();
        response.setEmployee(employeeInfo);
        response.setYear(year);
        response.setBalancesByType(leaveTypeBalances);
        response.setTotals(summaryTotals);
        response.setWarnings(warnings);

        return response;
    }

    /**
     * Update leave balance (deduct days for approved leave)
     * 
     * @param employeeId the employee ID
     * @param leaveType  the leave type
     * @param days       the days to deduct
     * @param year       the year
     * @return updated LeaveBalanceResponse
     */
    public LeaveBalanceResponse deductLeaveBalance(String employeeId, LeaveType leaveType, Double days, Integer year) {
        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeAndYear(employeeId, leaveType, year)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Leave balance not found for employee %s, type %s, year %d",
                                employeeId, leaveType, year)));

        balance.deductDays(days);
        LeaveBalance savedBalance = leaveBalanceRepository.save(balance);
        return convertToLeaveBalanceResponse(savedBalance);
    }

    /**
     * Restore leave balance (add days back for cancelled/rejected leave)
     * 
     * @param employeeId the employee ID
     * @param leaveType  the leave type
     * @param days       the days to add back
     * @param year       the year
     * @return updated LeaveBalanceResponse
     */
    public LeaveBalanceResponse restoreLeaveBalance(String employeeId, LeaveType leaveType, Double days, Integer year) {
        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeAndYear(employeeId, leaveType, year)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Leave balance not found for employee %s, type %s, year %d",
                                employeeId, leaveType, year)));

        balance.addDays(days);
        LeaveBalance savedBalance = leaveBalanceRepository.save(balance);
        return convertToLeaveBalanceResponse(savedBalance);
    }

    /**
     * Recalculate leave balance based on approved leave requests
     * 
     * @param employeeId the employee ID
     * @param year       the year
     * @return List of updated LeaveBalanceResponse
     */
    public List<LeaveBalanceResponse> recalculateLeaveBalance(String employeeId, Integer year) {
        // Get all approved leave requests for the employee in the year
        List<LeaveRequest> approvedRequests = leaveRequestRepository.findByEmployeeId(employeeId)
                .stream()
                .filter(request -> request.getStartDate().getYear() == year)
                .filter(request -> request.getStatus() == LeaveStatus.APPROVED ||
                        request.getStatus() == LeaveStatus.AUTO_APPROVED)
                .collect(Collectors.toList());

        // Group by leave type and calculate total used days
        Map<LeaveType, Double> usedDaysByType = approvedRequests.stream()
                .collect(Collectors.groupingBy(
                        LeaveRequest::getLeaveType,
                        Collectors.summingDouble(LeaveRequest::getTotalDays)));

        // Update balances
        List<LeaveBalance> balances = leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year);
        List<LeaveBalance> updatedBalances = new ArrayList<>();

        for (LeaveBalance balance : balances) {
            Double usedDays = usedDaysByType.getOrDefault(balance.getLeaveType(), 0.0);
            balance.setUsedDays(usedDays);
            updatedBalances.add(leaveBalanceRepository.save(balance));
        }

        return updatedBalances.stream()
                .map(this::convertToLeaveBalanceResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get employees with low leave balance
     * 
     * @param threshold the threshold for low balance (default 5 days)
     * @param year      the year
     * @return List of LeaveBalanceResponse with low balances
     */
    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getEmployeesWithLowBalance(Double threshold, Integer year) {
        Double actualThreshold = threshold != null ? threshold : 5.0;
        List<LeaveBalance> lowBalances = leaveBalanceRepository.findLowBalances(actualThreshold, year);
        return lowBalances.stream()
                .map(this::convertToLeaveBalanceResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get employees with high leave utilization
     * 
     * @param utilizationThreshold the utilization threshold percentage (0-100)
     * @param year                 the year
     * @return List of LeaveBalanceResponse with high utilization
     */
    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getEmployeesWithHighUtilization(Double utilizationThreshold, Integer year) {
        Double actualThreshold = utilizationThreshold != null ? utilizationThreshold : 80.0;
        List<LeaveBalance> highUtilizationBalances = leaveBalanceRepository
                .findHighUtilizationBalances(actualThreshold, year);
        return highUtilizationBalances.stream()
                .map(this::convertToLeaveBalanceResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get department-wise leave balance summary
     * 
     * @param department the department name
     * @param year       the year
     * @return List of LeaveBalanceResponse for the department
     */
    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getDepartmentLeaveBalance(String department, Integer year) {
        List<LeaveBalance> departmentBalances = leaveBalanceRepository.findByDepartmentAndYear(department, year);
        return departmentBalances.stream()
                .map(this::convertToLeaveBalanceResponse)
                .collect(Collectors.toList());
    }

    /**
     * Calculate department utilization percentage
     * 
     * @param department the department name
     * @param year       the year
     * @return average utilization percentage for the department
     */
    @Transactional(readOnly = true)
    public Double calculateDepartmentUtilization(String department, Integer year) {
        return leaveBalanceRepository.calculateDepartmentUtilization(department, year);
    }

    /**
     * Process year-end balance renewal
     * 
     * @param currentYear the current year
     * @param newYear     the new year
     * @return number of employees processed
     */
    public int processYearEndRenewal(Integer currentYear, Integer newYear) {
        // Get all employees who had balances in the current year
        List<LeaveBalance> currentYearBalances = leaveBalanceRepository.findBalancesForRenewal(currentYear);

        // Group by employee
        Map<String, List<LeaveBalance>> balancesByEmployee = currentYearBalances.stream()
                .collect(Collectors.groupingBy(balance -> balance.getEmployee().getEmployeeId()));

        int processedEmployees = 0;

        for (Map.Entry<String, List<LeaveBalance>> entry : balancesByEmployee.entrySet()) {
            String employeeId = entry.getKey();

            // Initialize balance for the new year
            try {
                initializeLeaveBalance(employeeId, newYear);
                processedEmployees++;
            } catch (Exception e) {
                // Log error but continue processing other employees
                System.err.println(
                        "Failed to process year-end renewal for employee " + employeeId + ": " + e.getMessage());
            }
        }

        return processedEmployees;
    }

    /**
     * Get employees without leave balance for a year
     * 
     * @param year the year
     * @return List of employees without balance records
     */
    @Transactional(readOnly = true)
    public List<String> getEmployeesWithoutBalance(Integer year) {
        List<Employee> employeesWithoutBalance = leaveBalanceRepository.findEmployeesWithoutBalance(year);
        return employeesWithoutBalance.stream()
                .map(Employee::getEmployeeId)
                .collect(Collectors.toList());
    }

    /**
     * Bulk initialize leave balance for multiple employees
     * 
     * @param employeeIds list of employee IDs
     * @param year        the year
     * @return number of employees processed successfully
     */
    public int bulkInitializeLeaveBalance(List<String> employeeIds, Integer year) {
        int successCount = 0;

        for (String employeeId : employeeIds) {
            try {
                initializeLeaveBalance(employeeId, year);
                successCount++;
            } catch (Exception e) {
                // Log error but continue processing other employees
                System.err.println("Failed to initialize balance for employee " + employeeId + ": " + e.getMessage());
            }
        }

        return successCount;
    }

    /**
     * Get leave balance statistics for a year
     * 
     * @param year the year
     * @return Map containing various statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getLeaveBalanceStatistics(Integer year) {
        Map<String, Object> stats = new HashMap<>();

        // Total employees with balance
        long totalEmployees = leaveBalanceRepository.countEmployeesWithBalance(year);
        stats.put("totalEmployees", totalEmployees);

        // Low balance count
        List<LeaveBalance> lowBalances = leaveBalanceRepository.findLowBalances(5.0, year);
        stats.put("employeesWithLowBalance", lowBalances.size());

        // Zero balance count
        List<LeaveBalance> zeroBalances = leaveBalanceRepository.findZeroBalances(year);
        stats.put("employeesWithZeroBalance", zeroBalances.size());

        // High utilization count
        List<LeaveBalance> highUtilization = leaveBalanceRepository.findHighUtilizationBalances(80.0, year);
        stats.put("employeesWithHighUtilization", highUtilization.size());

        // Average utilization by leave type
        Map<LeaveType, Double> avgUtilizationByType = new HashMap<>();
        for (LeaveType leaveType : LeaveType.values()) {
            List<LeaveBalance> typeBalances = leaveBalanceRepository.findByLeaveTypeAndYear(leaveType, year);
            double avgUtilization = typeBalances.stream()
                    .filter(balance -> balance.getTotalDays() > 0)
                    .mapToDouble(LeaveBalance::getUtilizationPercentage)
                    .average()
                    .orElse(0.0);
            avgUtilizationByType.put(leaveType, avgUtilization);
        }
        stats.put("averageUtilizationByType", avgUtilizationByType);

        return stats;
    }

    /**
     * Convert LeaveBalance entity to LeaveBalanceResponse DTO
     * 
     * @param leaveBalance the leave balance entity
     * @return LeaveBalanceResponse DTO
     */
    private LeaveBalanceResponse convertToLeaveBalanceResponse(LeaveBalance leaveBalance) {
        LeaveBalanceResponse response = new LeaveBalanceResponse();

        response.setId(leaveBalance.getId());

        // Create employee info
        LeaveBalanceResponse.EmployeeInfo employeeInfo = new LeaveBalanceResponse.EmployeeInfo(
                leaveBalance.getEmployee().getEmployeeId(),
                leaveBalance.getEmployee().getName(),
                leaveBalance.getEmployee().getDepartment());
        response.setEmployee(employeeInfo);

        response.setLeaveType(leaveBalance.getLeaveType());
        response.setTotalDays(leaveBalance.getTotalDays());
        response.setUsedDays(leaveBalance.getUsedDays());
        response.setAvailableDays(leaveBalance.getAvailableDays());
        response.setYear(leaveBalance.getYear());
        response.setUtilizationPercentage(leaveBalance.getUtilizationPercentage());
        response.setIsRunningLow(leaveBalance.isRunningLow());
        response.setCreatedAt(leaveBalance.getCreatedAt());
        response.setUpdatedAt(leaveBalance.getUpdatedAt());

        return response;
    }
}