package com.company.leavemanagementsystem.controller;

import com.company.leavemanagementsystem.dto.InitializeBalanceRequest;
import com.company.leavemanagementsystem.dto.LeaveBalanceResponse;
import com.company.leavemanagementsystem.dto.LeaveBalanceSummaryResponse;
import com.company.leavemanagementsystem.entity.LeaveType;
import com.company.leavemanagementsystem.service.LeaveBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Leave Balance management operations.
 * Provides endpoints for viewing and managing employee leave balances.
 */
@RestController
@RequestMapping("/api/leave-balances")
@Tag(name = "Leave Balance Management", description = "APIs for managing employee leave balances")
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    @Autowired
    public LeaveBalanceController(LeaveBalanceService leaveBalanceService) {
        this.leaveBalanceService = leaveBalanceService;
    }

    /**
     * Get leave balance for a specific employee.
     * 
     * @param employeeId Employee ID
     * @param year Year for balance (optional, defaults to current year)
     * @param leaveType Specific leave type (optional, returns all types if not specified)
     * @return List of leave balances for the employee
     */
    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get employee leave balance", 
               description = "Retrieves leave balance details for a specific employee")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Leave balance retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<List<LeaveBalanceResponse>> getEmployeeLeaveBalance(
            @Parameter(description = "Employee ID", required = true)
            @PathVariable String employeeId,
            @Parameter(description = "Year for balance (defaults to current year)")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Specific leave type (optional)")
            @RequestParam(required = false) LeaveType leaveType) {
        
        List<LeaveBalanceResponse> balances = year != null ? 
            leaveBalanceService.getEmployeeLeaveBalance(employeeId, year) :
            leaveBalanceService.getEmployeeLeaveBalance(employeeId);
        
        return ResponseEntity.ok(balances);
    }

    /**
     * Get leave balance summary for dashboard view.
     * 
     * @param employeeId Employee ID
     * @param year Year for balance (optional, defaults to current year)
     * @return Summary of leave balances with totals and warnings
     */
    @GetMapping("/employee/{employeeId}/summary")
    @Operation(summary = "Get leave balance summary", 
               description = "Retrieves leave balance summary for employee dashboard")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Leave balance summary retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<LeaveBalanceSummaryResponse> getLeaveBalanceSummary(
            @Parameter(description = "Employee ID", required = true)
            @PathVariable String employeeId,
            @Parameter(description = "Year for balance (defaults to current year)")
            @RequestParam(required = false) Integer year) {
        
        LeaveBalanceSummaryResponse summary = year != null ?
            leaveBalanceService.getLeaveBalanceSummary(employeeId, year) :
            leaveBalanceService.getLeaveBalanceSummary(employeeId);
        
        return ResponseEntity.ok(summary);
    }

    /**
     * Initialize leave balance for a new employee.
     * 
     * @param employeeId Employee ID
     * @param request Balance initialization request
     * @return List of initialized leave balances
     */
    @PostMapping("/employee/{employeeId}/initialize")
    @Operation(summary = "Initialize employee leave balance", 
               description = "Initializes leave balance for a new employee")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Leave balance initialized successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Employee not found"),
        @ApiResponse(responseCode = "409", description = "Leave balance already exists for this employee and year")
    })
    public ResponseEntity<List<LeaveBalanceResponse>> initializeLeaveBalance(
            @Parameter(description = "Employee ID", required = true)
            @PathVariable String employeeId,
            @Valid @RequestBody InitializeBalanceRequest request) {
        
        List<LeaveBalanceResponse> balances = leaveBalanceService.initializeLeaveBalance(employeeId, request);
        
        return new ResponseEntity<>(balances, HttpStatus.CREATED);
    }

    /**
     * Recalculate leave balance for a specific employee.
     * This is typically used for manual adjustments by HR.
     * 
     * @param employeeId Employee ID
     * @param year Year for the balance (optional, defaults to current year)
     * @return Updated leave balances
     */
    @PutMapping("/employee/{employeeId}/recalculate")
    @Operation(summary = "Recalculate employee leave balance", 
               description = "Recalculates leave balance based on approved requests (HR use)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Leave balance recalculated successfully"),
        @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<List<LeaveBalanceResponse>> recalculateLeaveBalance(
            @Parameter(description = "Employee ID", required = true)
            @PathVariable String employeeId,
            @Parameter(description = "Year for balance (defaults to current year)")
            @RequestParam(required = false) Integer year) {
        
        Integer targetYear = year != null ? year : java.time.LocalDate.now().getYear();
        List<LeaveBalanceResponse> balances = leaveBalanceService.recalculateLeaveBalance(
            employeeId, targetYear);
        
        return ResponseEntity.ok(balances);
    }

    /**
     * Get leave balance for all employees in a department.
     * Useful for HR and managers to view team leave status.
     * 
     * @param department Department name
     * @param year Year for balance (optional, defaults to current year)
     * @return List of employee leave balances in the department
     */
    @GetMapping("/department/{department}")
    @Operation(summary = "Get department leave balances", 
               description = "Retrieves leave balances for all employees in a department")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Department leave balances retrieved successfully")
    })
    public ResponseEntity<List<LeaveBalanceResponse>> getDepartmentLeaveBalances(
            @Parameter(description = "Department name", required = true)
            @PathVariable String department,
            @Parameter(description = "Year for balance (defaults to current year)")
            @RequestParam(required = false) Integer year) {
        
        Integer targetYear = year != null ? year : java.time.LocalDate.now().getYear();
        List<LeaveBalanceResponse> balances = leaveBalanceService.getDepartmentLeaveBalance(
            department, targetYear);
        
        return ResponseEntity.ok(balances);
    }

    /**
     * Get employees with low leave balance.
     * Useful for HR to identify employees who need to take leave.
     * 
     * @param threshold Balance threshold (defaults to 5 days)
     * @param year Year for balance (optional, defaults to current year)
     * @param department Filter by department (optional)
     * @return List of employees with low leave balance
     */
    @GetMapping("/low-balance")
    @Operation(summary = "Get employees with low leave balance", 
               description = "Retrieves employees with leave balance below threshold")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Low balance employees retrieved successfully")
    })
    public ResponseEntity<List<LeaveBalanceResponse>> getEmployeesWithLowBalance(
            @Parameter(description = "Balance threshold in days (defaults to 5)")
            @RequestParam(defaultValue = "5") Integer threshold,
            @Parameter(description = "Year for balance (defaults to current year)")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Filter by department")
            @RequestParam(required = false) String department) {
        
        Integer targetYear = year != null ? year : java.time.LocalDate.now().getYear();
        List<LeaveBalanceResponse> lowBalanceEmployees = 
            leaveBalanceService.getEmployeesWithLowBalance(threshold.doubleValue(), targetYear);
        
        return ResponseEntity.ok(lowBalanceEmployees);
    }

    /**
     * Process year-end renewal for leave balances.
     * This endpoint is typically used for annual leave balance renewal.
     * 
     * @param currentYear Current year to process
     * @param newYear New year to create balances for
     * @return Number of employees processed
     */
    @PostMapping("/year-end-renewal")
    @Operation(summary = "Process year-end renewal", 
               description = "Processes year-end renewal for leave balances")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Year-end renewal completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid year parameters")
    })
    public ResponseEntity<String> processYearEndRenewal(
            @Parameter(description = "Current year to process", required = true)
            @RequestParam Integer currentYear,
            @Parameter(description = "New year to create balances for", required = true)
            @RequestParam Integer newYear) {
        
        int processedCount = leaveBalanceService.processYearEndRenewal(currentYear, newYear);
        
        return ResponseEntity.ok(String.format(
            "Year-end renewal completed for %d employees", processedCount));
    }

    /**
     * Get leave balance statistics for a year.
     * Provides aggregated statistics for reporting.
     * 
     * @param year Year for statistics (optional, defaults to current year)
     * @return Statistics map with various metrics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get leave balance statistics", 
               description = "Retrieves aggregated leave balance statistics for reporting")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    public ResponseEntity<java.util.Map<String, Object>> getLeaveBalanceStatistics(
            @Parameter(description = "Year for statistics (defaults to current year)")
            @RequestParam(required = false) Integer year) {
        
        Integer targetYear = year != null ? year : java.time.LocalDate.now().getYear();
        java.util.Map<String, Object> statistics = leaveBalanceService.getLeaveBalanceStatistics(targetYear);
        
        return ResponseEntity.ok(statistics);
    }
}