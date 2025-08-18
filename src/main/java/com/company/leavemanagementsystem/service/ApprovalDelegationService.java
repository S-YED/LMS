package com.company.leavemanagementsystem.service;

import com.company.leavemanagementsystem.entity.Employee;
import com.company.leavemanagementsystem.entity.LeaveRequest;
import com.company.leavemanagementsystem.entity.LeaveStatus;
import com.company.leavemanagementsystem.repository.EmployeeRepository;
import com.company.leavemanagementsystem.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for handling approval delegation and manager workflow validations.
 * Manages manager availability, alternate approvers, and authorization validation.
 */
@Service
public class ApprovalDelegationService {

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    @Autowired
    public ApprovalDelegationService(EmployeeRepository employeeRepository,
                                   LeaveRequestRepository leaveRequestRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    /**
     * Check if a manager is available to approve leave requests
     * @param managerId the manager's employee ID
     * @param requestDate the date when approval is needed
     * @return ApprovalAvailabilityResult containing availability status and alternate approvers
     */
    public ApprovalAvailabilityResult checkManagerAvailability(String managerId, LocalDate requestDate) {
        List<String> issues = new ArrayList<>();
        List<Employee> alternateApprovers = new ArrayList<>();

        Optional<Employee> managerOpt = employeeRepository.findByEmployeeId(managerId);
        if (managerOpt.isEmpty()) {
            issues.add("Manager not found with ID: " + managerId);
            return new ApprovalAvailabilityResult(false, null, alternateApprovers, issues);
        }

        Employee manager = managerOpt.get();

        // Check if manager is on leave during the approval period
        List<LeaveRequest> managerLeaves = leaveRequestRepository.findActiveLeaveRequests(requestDate);
        boolean managerOnLeave = managerLeaves.stream()
            .anyMatch(leave -> leave.getEmployee().getEmployeeId().equals(managerId));

        if (managerOnLeave) {
            issues.add("Primary manager is on leave during approval period");
            alternateApprovers = findAlternateApprovers(manager);
            return new ApprovalAvailabilityResult(false, manager, alternateApprovers, issues);
        }

        return new ApprovalAvailabilityResult(true, manager, alternateApprovers, issues);
    }

    /**
     * Find alternate approvers when primary manager is unavailable
     * @param primaryManager the primary manager who is unavailable
     * @return List of alternate approvers (manager's manager, HR, etc.)
     */
    public List<Employee> findAlternateApprovers(Employee primaryManager) {
        List<Employee> alternates = new ArrayList<>();

        // 1. Manager's manager (next level up in hierarchy)
        if (primaryManager.getManager() != null) {
            alternates.add(primaryManager.getManager());
        }

        // 2. Other managers in the same department
        List<Employee> departmentManagers = findDepartmentManagers(primaryManager.getDepartment());
        for (Employee manager : departmentManagers) {
            if (!manager.getEmployeeId().equals(primaryManager.getEmployeeId()) && 
                !alternates.contains(manager)) {
                alternates.add(manager);
            }
        }

        // 3. HR representatives (employees without managers - assuming top-level)
        if (alternates.isEmpty()) {
            List<Employee> hrReps = employeeRepository.findByManagerIsNull();
            alternates.addAll(hrReps);
        }

        return alternates;
    }

    /**
     * Find all managers in a specific department
     * @param department the department name
     * @return List of employees who are managers in the department
     */
    public List<Employee> findDepartmentManagers(String department) {
        List<Employee> departmentEmployees = employeeRepository.findByDepartment(department);
        List<Employee> managers = new ArrayList<>();

        for (Employee employee : departmentEmployees) {
            // Check if this employee has subordinates (is a manager)
            long subordinateCount = employeeRepository.countSubordinates(employee.getEmployeeId());
            if (subordinateCount > 0) {
                managers.add(employee);
            }
        }

        return managers;
    }

    /**
     * Validate authorization for approval requests
     * @param approverId the ID of the person attempting to approve
     * @param leaveRequest the leave request to be approved
     * @return AuthorizationResult containing validation status and messages
     */
    public AuthorizationResult validateApprovalAuthorization(String approverId, LeaveRequest leaveRequest) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 1. Check if approver exists
        Optional<Employee> approverOpt = employeeRepository.findByEmployeeId(approverId);
        if (approverOpt.isEmpty()) {
            errors.add("Approver not found with ID: " + approverId);
            return new AuthorizationResult(false, null, errors, warnings);
        }

        Employee approver = approverOpt.get();
        Employee requestingEmployee = leaveRequest.getEmployee();

        // 2. Prevent self-approval
        if (approverId.equals(requestingEmployee.getEmployeeId())) {
            errors.add("Employees cannot approve their own leave requests");
            return new AuthorizationResult(false, approver, errors, warnings);
        }

        // 3. Check if approver is the direct manager
        boolean isDirectManager = requestingEmployee.getManager() != null && 
            requestingEmployee.getManager().getEmployeeId().equals(approverId);

        if (isDirectManager) {
            return new AuthorizationResult(true, approver, errors, warnings);
        }

        // 4. Check if approver is an alternate approver
        Employee primaryManager = requestingEmployee.getManager();
        if (primaryManager != null) {
            ApprovalAvailabilityResult availability = checkManagerAvailability(
                primaryManager.getEmployeeId(), LocalDate.now());
            
            if (!availability.isAvailable()) {
                List<Employee> alternates = availability.getAlternateApprovers();
                boolean isAlternateApprover = alternates.stream()
                    .anyMatch(alt -> alt.getEmployeeId().equals(approverId));
                
                if (isAlternateApprover) {
                    warnings.add("Approving as alternate approver due to primary manager unavailability");
                    return new AuthorizationResult(true, approver, errors, warnings);
                }
            }
        }

        // 5. Check if approver is in management hierarchy
        List<Employee> managementChain = employeeRepository.findManagerHierarchy(requestingEmployee.getEmployeeId());
        boolean isInManagementChain = managementChain.stream()
            .anyMatch(manager -> manager.getEmployeeId().equals(approverId));

        if (isInManagementChain) {
            warnings.add("Approving as higher-level manager in hierarchy");
            return new AuthorizationResult(true, approver, errors, warnings);
        }

        // 6. Check if approver is HR (top-level employee)
        if (approver.getManager() == null) {
            warnings.add("Approving as HR representative");
            return new AuthorizationResult(true, approver, errors, warnings);
        }

        // 7. If none of the above, deny authorization
        errors.add("You are not authorized to approve this leave request. " +
            "Only the employee's manager or authorized delegates can approve leave requests.");

        return new AuthorizationResult(false, approver, errors, warnings);
    }

    /**
     * Get the appropriate approver for a leave request
     * @param leaveRequest the leave request
     * @return the employee who should approve this request
     */
    public Employee getAppropriatApprover(LeaveRequest leaveRequest) {
        Employee requestingEmployee = leaveRequest.getEmployee();
        
        // Primary manager is the first choice
        if (requestingEmployee.getManager() != null) {
            Employee primaryManager = requestingEmployee.getManager();
            
            // Check if primary manager is available
            ApprovalAvailabilityResult availability = checkManagerAvailability(
                primaryManager.getEmployeeId(), LocalDate.now());
            
            if (availability.isAvailable()) {
                return primaryManager;
            } else {
                // Return first available alternate approver
                List<Employee> alternates = availability.getAlternateApprovers();
                if (!alternates.isEmpty()) {
                    return alternates.get(0);
                }
            }
        }

        // Fallback to HR (top-level employees)
        List<Employee> hrReps = employeeRepository.findByManagerIsNull();
        if (!hrReps.isEmpty()) {
            return hrReps.get(0);
        }

        return null; // Should not happen in a well-structured organization
    }

    /**
     * Check if a leave request can be auto-approved based on business rules
     * @param leaveRequest the leave request to check
     * @return true if the request can be auto-approved
     */
    public boolean canAutoApprove(LeaveRequest leaveRequest) {
        // Auto-approve emergency leave <= 2 days
        return leaveRequest.getIsEmergencyLeave() && 
               leaveRequest.getTotalDays() != null && 
               leaveRequest.getTotalDays() <= 2.0;
    }

    /**
     * Get pending approval count for a manager
     * @param managerId the manager's employee ID
     * @return number of pending approvals
     */
    public long getPendingApprovalCount(String managerId) {
        return leaveRequestRepository.countPendingRequestsByManager(managerId);
    }

    /**
     * Get all pending requests for a manager with delegation support
     * @param managerId the manager's employee ID
     * @return List of leave requests pending approval
     */
    public List<LeaveRequest> getPendingRequestsForManager(String managerId) {
        List<LeaveRequest> directReports = leaveRequestRepository.findPendingRequestsByManager(managerId);
        
        // Also include requests where this manager is an alternate approver
        List<LeaveRequest> alternateRequests = findRequestsWhereAlternateApprover(managerId);
        
        // Combine and deduplicate
        List<LeaveRequest> allRequests = new ArrayList<>(directReports);
        for (LeaveRequest request : alternateRequests) {
            if (!allRequests.contains(request)) {
                allRequests.add(request);
            }
        }
        
        return allRequests;
    }

    /**
     * Find requests where the given manager is an alternate approver
     * @param managerId the manager's employee ID
     * @return List of requests where manager can act as alternate approver
     */
    private List<LeaveRequest> findRequestsWhereAlternateApprover(String managerId) {
        List<LeaveRequest> alternateRequests = new ArrayList<>();
        
        // Find all pending requests
        List<LeaveRequest> allPendingRequests = leaveRequestRepository.findByStatus(LeaveStatus.PENDING);
        
        for (LeaveRequest request : allPendingRequests) {
            Employee primaryManager = request.getEmployee().getManager();
            if (primaryManager != null) {
                ApprovalAvailabilityResult availability = checkManagerAvailability(
                    primaryManager.getEmployeeId(), LocalDate.now());
                
                if (!availability.isAvailable()) {
                    List<Employee> alternates = availability.getAlternateApprovers();
                    boolean isAlternate = alternates.stream()
                        .anyMatch(alt -> alt.getEmployeeId().equals(managerId));
                    
                    if (isAlternate) {
                        alternateRequests.add(request);
                    }
                }
            }
        }
        
        return alternateRequests;
    }

    /**
     * Validate that a leave request is in a state that allows approval/rejection
     * @param leaveRequest the leave request
     * @return ValidationResult indicating if the request can be processed
     */
    public ValidationResult validateRequestProcessable(LeaveRequest leaveRequest) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (leaveRequest == null) {
            errors.add("Leave request not found");
            return new ValidationResult(false, errors, warnings);
        }

        // Check if request is in pending status
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            errors.add("Leave request is not in pending status. Current status: " + 
                leaveRequest.getStatus().getDisplayName());
        }

        // Check if request is not too old (optional business rule)
        // For example, requests older than 30 days might need special handling
        if (leaveRequest.getCreatedAt() != null && 
            leaveRequest.getCreatedAt().isBefore(java.time.LocalDateTime.now().minusDays(30))) {
            warnings.add("This leave request is older than 30 days and may need special attention");
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    /**
     * Result class for approval availability checks
     */
    public static class ApprovalAvailabilityResult {
        private final boolean available;
        private final Employee primaryManager;
        private final List<Employee> alternateApprovers;
        private final List<String> issues;

        public ApprovalAvailabilityResult(boolean available, Employee primaryManager, 
                                        List<Employee> alternateApprovers, List<String> issues) {
            this.available = available;
            this.primaryManager = primaryManager;
            this.alternateApprovers = alternateApprovers != null ? new ArrayList<>(alternateApprovers) : new ArrayList<>();
            this.issues = issues != null ? new ArrayList<>(issues) : new ArrayList<>();
        }

        public boolean isAvailable() {
            return available;
        }

        public Employee getPrimaryManager() {
            return primaryManager;
        }

        public List<Employee> getAlternateApprovers() {
            return new ArrayList<>(alternateApprovers);
        }

        public List<String> getIssues() {
            return new ArrayList<>(issues);
        }

        public boolean hasAlternates() {
            return !alternateApprovers.isEmpty();
        }
    }

    /**
     * Result class for authorization validation
     */
    public static class AuthorizationResult {
        private final boolean authorized;
        private final Employee approver;
        private final List<String> errors;
        private final List<String> warnings;

        public AuthorizationResult(boolean authorized, Employee approver, 
                                 List<String> errors, List<String> warnings) {
            this.authorized = authorized;
            this.approver = approver;
            this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
            this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
        }

        public boolean isAuthorized() {
            return authorized;
        }

        public Employee getApprover() {
            return approver;
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
    }

    /**
     * Reusable validation result class
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
    }
}