package com.company.leavemanagementsystem.controller;

import com.company.leavemanagementsystem.dto.CreateEmployeeRequest;
import com.company.leavemanagementsystem.dto.EmployeeResponse;
import com.company.leavemanagementsystem.dto.UpdateEmployeeRequest;
import com.company.leavemanagementsystem.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Employee management operations.
 * Provides endpoints for CRUD operations on employee records.
 */
@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employee Management", description = "APIs for managing employee records")
public class EmployeeController {

    private final EmployeeService employeeService;

    @Autowired
    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * Create a new employee record.
     * 
     * @param request Employee creation request containing employee details
     * @return Created employee response with HTTP 201 status
     */
    @PostMapping
    @Operation(summary = "Create new employee", description = "Creates a new employee record in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Employee created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "Employee ID already exists")
    })
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request) {
        
        EmployeeResponse response = employeeService.createEmployee(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Retrieve employee by employee ID.
     * 
     * @param employeeId Unique employee identifier
     * @return Employee details with HTTP 200 status
     */
    @GetMapping("/{employeeId}")
    @Operation(summary = "Get employee by ID", description = "Retrieves employee details by employee ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Employee found"),
        @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<EmployeeResponse> getEmployee(
            @Parameter(description = "Employee ID", required = true)
            @PathVariable String employeeId) {
        
        EmployeeResponse response = employeeService.getEmployeeByEmployeeId(employeeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update existing employee record.
     * 
     * @param employeeId Employee ID to update
     * @param request Updated employee details
     * @return Updated employee response with HTTP 200 status
     */
    @PutMapping("/{employeeId}")
    @Operation(summary = "Update employee", description = "Updates existing employee record")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Employee updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @Parameter(description = "Employee ID", required = true)
            @PathVariable String employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        
        EmployeeResponse response = employeeService.updateEmployee(employeeId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all employees with pagination support.
     * 
     * @param page Page number (0-based, default: 0)
     * @param size Page size (default: 20)
     * @param sortBy Field to sort by (default: name)
     * @param sortDir Sort direction (asc/desc, default: asc)
     * @return Paginated list of employees
     */
    @GetMapping
    @Operation(summary = "Get all employees", description = "Retrieves paginated list of all employees")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Employees retrieved successfully")
    })
    public ResponseEntity<Page<EmployeeResponse>> getAllEmployees(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<EmployeeResponse> employees = employeeService.getAllEmployees(pageable);
        
        return ResponseEntity.ok(employees);
    }

    /**
     * Get all employees without pagination (for dropdown lists, etc.).
     * 
     * @return List of all employees
     */
    @GetMapping("/all")
    @Operation(summary = "Get all employees (no pagination)", 
               description = "Retrieves all employees without pagination for dropdown lists")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All employees retrieved successfully")
    })
    public ResponseEntity<List<EmployeeResponse>> getAllEmployeesNoPagination() {
        List<EmployeeResponse> employees = employeeService.getAllEmployees();
        return ResponseEntity.ok(employees);
    }

    /**
     * Delete employee by employee ID.
     * Note: This performs a soft delete to preserve leave history.
     * 
     * @param employeeId Employee ID to delete
     * @return HTTP 204 No Content status
     */
    @DeleteMapping("/{employeeId}")
    @Operation(summary = "Delete employee", 
               description = "Soft deletes employee record while preserving leave history")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Employee deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Employee not found"),
        @ApiResponse(responseCode = "409", description = "Cannot delete employee with pending leave requests")
    })
    public ResponseEntity<Void> deleteEmployee(
            @Parameter(description = "Employee ID", required = true)
            @PathVariable String employeeId) {
        
        employeeService.deleteEmployee(employeeId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get employees by department.
     * 
     * @param department Department name
     * @param page Page number (0-based, default: 0)
     * @param size Page size (default: 20)
     * @return Paginated list of employees in the department
     */
    @GetMapping("/department/{department}")
    @Operation(summary = "Get employees by department", 
               description = "Retrieves paginated list of employees in a specific department")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Employees retrieved successfully")
    })
    public ResponseEntity<Page<EmployeeResponse>> getEmployeesByDepartment(
            @Parameter(description = "Department name", required = true)
            @PathVariable String department,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        Page<EmployeeResponse> employees = employeeService.getEmployeesByDepartment(department, pageable);
        
        return ResponseEntity.ok(employees);
    }

    /**
     * Get employees managed by a specific manager.
     * 
     * @param managerId Manager's employee ID
     * @return List of employees reporting to the manager
     */
    @GetMapping("/manager/{managerId}/reports")
    @Operation(summary = "Get direct reports", 
               description = "Retrieves list of employees reporting to a specific manager")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Direct reports retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Manager not found")
    })
    public ResponseEntity<List<EmployeeResponse>> getDirectReports(
            @Parameter(description = "Manager's employee ID", required = true)
            @PathVariable String managerId) {
        
        List<EmployeeResponse> directReports = employeeService.getSubordinates(managerId);
        return ResponseEntity.ok(directReports);
    }
}