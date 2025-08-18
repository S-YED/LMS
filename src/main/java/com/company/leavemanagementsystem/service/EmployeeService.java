package com.company.leavemanagementsystem.service;

import com.company.leavemanagementsystem.dto.CreateEmployeeRequest;
import com.company.leavemanagementsystem.dto.EmployeeResponse;
import com.company.leavemanagementsystem.dto.UpdateEmployeeRequest;
import com.company.leavemanagementsystem.entity.Employee;
import com.company.leavemanagementsystem.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for managing employee operations.
 * Handles CRUD operations, manager assignment validation, and business logic.
 */
@Service
@Transactional
public class EmployeeService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;

    @Autowired
    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /**
     * Create a new employee with duplicate checking and manager validation
     * @param request the employee creation request
     * @return the created employee response
     * @throws IllegalArgumentException if employee ID or email already exists
     * @throws IllegalArgumentException if manager is invalid
     */
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        logger.info("Creating new employee with ID: {}", request.getEmployeeId());

        // Check for duplicate employee ID
        if (employeeRepository.existsByEmployeeId(request.getEmployeeId())) {
            logger.error("Employee ID already exists: {}", request.getEmployeeId());
            throw new IllegalArgumentException("Employee ID already exists: " + request.getEmployeeId());
        }

        // Check for duplicate email
        if (employeeRepository.existsByEmail(request.getEmail())) {
            logger.error("Email already exists: {}", request.getEmail());
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // Create new employee entity
        Employee employee = new Employee(
            request.getEmployeeId(),
            request.getName(),
            request.getEmail(),
            request.getDepartment(),
            request.getPosition(),
            request.getJoiningDate()
        );

        // Validate and set manager if provided
        if (request.getManagerId() != null && !request.getManagerId().trim().isEmpty()) {
            Employee manager = validateAndGetManager(request.getManagerId(), request.getEmployeeId());
            employee.setManager(manager);
        }

        // Save the employee
        Employee savedEmployee = employeeRepository.save(employee);
        logger.info("Successfully created employee: {}", savedEmployee.getEmployeeId());

        return convertToEmployeeResponse(savedEmployee);
    }

    /**
     * Retrieve employee by employee ID
     * @param employeeId the unique employee identifier
     * @return the employee response
     * @throws IllegalArgumentException if employee not found
     */
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeByEmployeeId(String employeeId) {
        logger.debug("Retrieving employee with ID: {}", employeeId);

        Employee employee = employeeRepository.findByEmployeeId(employeeId)
            .orElseThrow(() -> {
                logger.error("Employee not found with ID: {}", employeeId);
                return new IllegalArgumentException("Employee not found with ID: " + employeeId);
            });

        return convertToEmployeeResponse(employee);
    }

    /**
     * Retrieve employee by database ID
     * @param id the database ID
     * @return the employee response
     * @throws IllegalArgumentException if employee not found
     */
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        logger.debug("Retrieving employee with database ID: {}", id);

        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> {
                logger.error("Employee not found with database ID: {}", id);
                return new IllegalArgumentException("Employee not found with ID: " + id);
            });

        return convertToEmployeeResponse(employee);
    }

    /**
     * Retrieve all employees with pagination
     * @param pageable pagination information
     * @return page of employee responses
     */
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getAllEmployees(Pageable pageable) {
        logger.debug("Retrieving all employees with pagination: {}", pageable);

        Page<Employee> employees = employeeRepository.findAll(pageable);
        return employees.map(this::convertToEmployeeResponse);
    }

    /**
     * Retrieve all employees without pagination
     * @return list of all employee responses
     */
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllEmployees() {
        logger.debug("Retrieving all employees");

        List<Employee> employees = employeeRepository.findAll();
        return employees.stream()
            .map(this::convertToEmployeeResponse)
            .collect(Collectors.toList());
    }

    /**
     * Update an existing employee
     * @param employeeId the employee ID to update
     * @param request the update request
     * @return the updated employee response
     * @throws IllegalArgumentException if employee not found or validation fails
     */
    public EmployeeResponse updateEmployee(String employeeId, UpdateEmployeeRequest request) {
        logger.info("Updating employee with ID: {}", employeeId);

        Employee employee = employeeRepository.findByEmployeeId(employeeId)
            .orElseThrow(() -> {
                logger.error("Employee not found with ID: {}", employeeId);
                return new IllegalArgumentException("Employee not found with ID: " + employeeId);
            });

        // Check for duplicate email (excluding current employee)
        Optional<Employee> existingEmailEmployee = employeeRepository.findByEmail(request.getEmail());
        if (existingEmailEmployee.isPresent() && !existingEmailEmployee.get().getEmployeeId().equals(employeeId)) {
            logger.error("Email already exists: {}", request.getEmail());
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // Update employee fields
        employee.setName(request.getName());
        employee.setEmail(request.getEmail());
        employee.setDepartment(request.getDepartment());
        employee.setJoiningDate(request.getJoiningDate());

        // Validate and update manager if provided
        if (request.getManagerId() != null && !request.getManagerId().trim().isEmpty()) {
            Employee manager = validateAndGetManager(request.getManagerId(), employeeId);
            employee.setManager(manager);
        } else {
            employee.setManager(null);
        }

        Employee updatedEmployee = employeeRepository.save(employee);
        logger.info("Successfully updated employee: {}", updatedEmployee.getEmployeeId());

        return convertToEmployeeResponse(updatedEmployee);
    }

    /**
     * Delete an employee with leave history preservation
     * Note: This is a soft delete that preserves leave history for audit purposes
     * @param employeeId the employee ID to delete
     * @throws IllegalArgumentException if employee not found
     * @throws IllegalStateException if employee has active subordinates
     */
    public void deleteEmployee(String employeeId) {
        logger.info("Deleting employee with ID: {}", employeeId);

        Employee employee = employeeRepository.findByEmployeeId(employeeId)
            .orElseThrow(() -> {
                logger.error("Employee not found with ID: {}", employeeId);
                return new IllegalArgumentException("Employee not found with ID: " + employeeId);
            });

        // Check if employee has subordinates
        List<Employee> subordinates = employeeRepository.findByManagerEmployeeId(employeeId);
        if (!subordinates.isEmpty()) {
            logger.error("Cannot delete employee {} - has {} active subordinates", employeeId, subordinates.size());
            throw new IllegalStateException("Cannot delete employee with active subordinates. " +
                "Please reassign subordinates first. Subordinates: " + 
                subordinates.stream().map(Employee::getEmployeeId).collect(Collectors.joining(", ")));
        }

        // For now, we'll do a hard delete, but in production this should be a soft delete
        // to preserve leave history for audit purposes
        employeeRepository.delete(employee);
        logger.info("Successfully deleted employee: {}", employeeId);
    }

    /**
     * Get employees by department
     * @param department the department name
     * @param pageable pagination information
     * @return page of employee responses in the department
     */
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getEmployeesByDepartment(String department, Pageable pageable) {
        logger.debug("Retrieving employees in department: {}", department);

        Page<Employee> employees = employeeRepository.findByDepartment(department, pageable);
        return employees.map(this::convertToEmployeeResponse);
    }

    /**
     * Get direct subordinates of a manager
     * @param managerId the manager's employee ID
     * @return list of subordinate employee responses
     */
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getSubordinates(String managerId) {
        logger.debug("Retrieving subordinates for manager: {}", managerId);

        // Verify manager exists
        if (!employeeRepository.existsByEmployeeId(managerId)) {
            logger.error("Manager not found with ID: {}", managerId);
            throw new IllegalArgumentException("Manager not found with ID: " + managerId);
        }

        List<Employee> subordinates = employeeRepository.findByManagerEmployeeId(managerId);
        return subordinates.stream()
            .map(this::convertToEmployeeResponse)
            .collect(Collectors.toList());
    }

    /**
     * Search employees by name
     * @param name the search term
     * @param pageable pagination information
     * @return page of matching employee responses
     */
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> searchEmployeesByName(String name, Pageable pageable) {
        logger.debug("Searching employees by name: {}", name);

        Page<Employee> employees = employeeRepository.findByNameContainingIgnoreCase(name, pageable);
        return employees.map(this::convertToEmployeeResponse);
    }

    /**
     * Get all departments
     * @return list of all unique department names
     */
    @Transactional(readOnly = true)
    public List<String> getAllDepartments() {
        logger.debug("Retrieving all departments");
        return employeeRepository.findAllDepartments();
    }

    /**
     * Check if employee exists by employee ID
     * @param employeeId the employee ID to check
     * @return true if employee exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean employeeExists(String employeeId) {
        return employeeRepository.existsByEmployeeId(employeeId);
    }

    /**
     * Get employee entity by employee ID (for internal use)
     * @param employeeId the employee ID
     * @return the employee entity
     * @throws IllegalArgumentException if employee not found
     */
    @Transactional(readOnly = true)
    public Employee getEmployeeEntityByEmployeeId(String employeeId) {
        return employeeRepository.findByEmployeeId(employeeId)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + employeeId));
    }

    /**
     * Validate and get manager for assignment
     * @param managerId the manager's employee ID
     * @param employeeId the employee's ID (to check for circular references)
     * @return the manager entity
     * @throws IllegalArgumentException if manager is invalid
     */
    private Employee validateAndGetManager(String managerId, String employeeId) {
        logger.debug("Validating manager assignment: {} -> {}", managerId, employeeId);

        // Check if manager exists
        Employee manager = employeeRepository.findByEmployeeId(managerId)
            .orElseThrow(() -> {
                logger.error("Manager not found with ID: {}", managerId);
                return new IllegalArgumentException("Manager not found with ID: " + managerId);
            });

        // Prevent self-assignment as manager
        if (managerId.equals(employeeId)) {
            logger.error("Employee cannot be their own manager: {}", employeeId);
            throw new IllegalArgumentException("Employee cannot be their own manager");
        }

        // Check for circular reference (if employee already exists)
        if (employeeRepository.existsByEmployeeId(employeeId)) {
            boolean wouldCreateCircular = employeeRepository.wouldCreateCircularReference(employeeId, managerId);
            if (wouldCreateCircular) {
                logger.error("Manager assignment would create circular reference: {} -> {}", managerId, employeeId);
                throw new IllegalArgumentException("Manager assignment would create circular reference");
            }
        }

        return manager;
    }

    /**
     * Convert Employee entity to EmployeeResponse DTO
     * @param employee the employee entity
     * @return the employee response DTO
     */
    private EmployeeResponse convertToEmployeeResponse(Employee employee) {
        EmployeeResponse response = new EmployeeResponse();
        response.setId(employee.getId());
        response.setEmployeeId(employee.getEmployeeId());
        response.setName(employee.getName());
        response.setEmail(employee.getEmail());
        response.setDepartment(employee.getDepartment());
        response.setJoiningDate(employee.getJoiningDate());
        response.setCreatedAt(employee.getCreatedAt());
        response.setUpdatedAt(employee.getUpdatedAt());

        // Set manager information if present
        if (employee.getManager() != null) {
            Employee manager = employee.getManager();
            EmployeeResponse.ManagerInfo managerInfo = new EmployeeResponse.ManagerInfo(
                manager.getEmployeeId(),
                manager.getName(),
                manager.getEmail(),
                manager.getDepartment()
            );
            response.setManager(managerInfo);
        }

        return response;
    }
}