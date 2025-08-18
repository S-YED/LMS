package com.company.leavemanagementsystem.repository;

import com.company.leavemanagementsystem.entity.Employee;
import com.company.leavemanagementsystem.entity.LeaveRequest;
import com.company.leavemanagementsystem.entity.LeaveStatus;
import com.company.leavemanagementsystem.entity.LeaveType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for LeaveRequest entity operations.
 * Provides complex queries for leave request management including overlapping checks,
 * manager workflows, and comprehensive filtering capabilities.
 */
@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    /**
     * Find leave request by request ID
     * @param requestId the unique request identifier
     * @return Optional containing the leave request if found
     */
    Optional<LeaveRequest> findByRequestId(String requestId);

    /**
     * Find all leave requests for a specific employee
     * @param employee the employee
     * @return List of leave requests for the employee
     */
    List<LeaveRequest> findByEmployee(Employee employee);

    /**
     * Find all leave requests for a specific employee by employee ID
     * @param employeeId the employee's ID
     * @return List of leave requests for the employee
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.employeeId = :employeeId")
    List<LeaveRequest> findByEmployeeId(@Param("employeeId") String employeeId);

    /**
     * Find all leave requests for a specific employee with pagination
     * @param employeeId the employee's ID
     * @param pageable pagination information
     * @return Page of leave requests for the employee
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.employeeId = :employeeId ORDER BY lr.createdAt DESC")
    Page<LeaveRequest> findByEmployeeId(@Param("employeeId") String employeeId, Pageable pageable);

    /**
     * Find leave requests by status
     * @param status the leave status
     * @return List of leave requests with the specified status
     */
    List<LeaveRequest> findByStatus(LeaveStatus status);

    /**
     * Find leave requests by status with pagination
     * @param status the leave status
     * @param pageable pagination information
     * @return Page of leave requests with the specified status
     */
    Page<LeaveRequest> findByStatus(LeaveStatus status, Pageable pageable);

    /**
     * Find all pending leave requests for employees under a specific manager
     * @param managerId the manager's employee ID
     * @return List of pending leave requests for the manager to review
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.manager.employeeId = :managerId AND lr.status = 'PENDING' ORDER BY lr.createdAt ASC")
    List<LeaveRequest> findPendingRequestsByManager(@Param("managerId") String managerId);

    /**
     * Find all pending leave requests for employees under a specific manager with pagination
     * @param managerId the manager's employee ID
     * @param pageable pagination information
     * @return Page of pending leave requests for the manager to review
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.manager.employeeId = :managerId AND lr.status = 'PENDING' ORDER BY lr.createdAt ASC")
    Page<LeaveRequest> findPendingRequestsByManager(@Param("managerId") String managerId, Pageable pageable);

    /**
     * Find overlapping leave requests for a specific employee within a date range
     * Excludes cancelled and rejected requests
     * @param employeeId the employee's ID
     * @param startDate the start date of the new request
     * @param endDate the end date of the new request
     * @return List of overlapping leave requests
     */
    @Query("""
        SELECT lr FROM LeaveRequest lr 
        WHERE lr.employee.employeeId = :employeeId 
        AND lr.status NOT IN ('CANCELLED', 'REJECTED')
        AND NOT (lr.endDate < :startDate OR lr.startDate > :endDate)
        """)
    List<LeaveRequest> findOverlappingLeaves(@Param("employeeId") String employeeId, 
                                           @Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate);

    /**
     * Find overlapping leave requests for a specific employee within a date range, excluding a specific request
     * Used when updating an existing request
     * @param employeeId the employee's ID
     * @param startDate the start date of the request
     * @param endDate the end date of the request
     * @param excludeRequestId the request ID to exclude from the check
     * @return List of overlapping leave requests
     */
    @Query("""
        SELECT lr FROM LeaveRequest lr 
        WHERE lr.employee.employeeId = :employeeId 
        AND lr.requestId != :excludeRequestId
        AND lr.status NOT IN ('CANCELLED', 'REJECTED')
        AND NOT (lr.endDate < :startDate OR lr.startDate > :endDate)
        """)
    List<LeaveRequest> findOverlappingLeavesExcluding(@Param("employeeId") String employeeId, 
                                                    @Param("startDate") LocalDate startDate, 
                                                    @Param("endDate") LocalDate endDate,
                                                    @Param("excludeRequestId") String excludeRequestId);

    /**
     * Find leave requests by employee and status
     * @param employeeId the employee's ID
     * @param status the leave status
     * @return List of leave requests matching the criteria
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.employeeId = :employeeId AND lr.status = :status")
    List<LeaveRequest> findByEmployeeIdAndStatus(@Param("employeeId") String employeeId, @Param("status") LeaveStatus status);

    /**
     * Find leave requests by employee and leave type
     * @param employeeId the employee's ID
     * @param leaveType the leave type
     * @return List of leave requests matching the criteria
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.employeeId = :employeeId AND lr.leaveType = :leaveType")
    List<LeaveRequest> findByEmployeeIdAndLeaveType(@Param("employeeId") String employeeId, @Param("leaveType") LeaveType leaveType);

    /**
     * Find leave requests within a date range
     * @param startDate the range start date
     * @param endDate the range end date
     * @return List of leave requests within the date range
     */
    @Query("""
        SELECT lr FROM LeaveRequest lr 
        WHERE NOT (lr.endDate < :startDate OR lr.startDate > :endDate)
        ORDER BY lr.startDate ASC
        """)
    List<LeaveRequest> findLeaveRequestsInDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find leave requests within a date range with pagination
     * @param startDate the range start date
     * @param endDate the range end date
     * @param pageable pagination information
     * @return Page of leave requests within the date range
     */
    @Query("""
        SELECT lr FROM LeaveRequest lr 
        WHERE NOT (lr.endDate < :startDate OR lr.startDate > :endDate)
        ORDER BY lr.startDate ASC
        """)
    Page<LeaveRequest> findLeaveRequestsInDateRange(@Param("startDate") LocalDate startDate, 
                                                  @Param("endDate") LocalDate endDate, 
                                                  Pageable pageable);

    /**
     * Find leave requests by department within a date range
     * @param department the department name
     * @param startDate the range start date
     * @param endDate the range end date
     * @return List of leave requests for the department within the date range
     */
    @Query("""
        SELECT lr FROM LeaveRequest lr 
        WHERE lr.employee.department = :department
        AND NOT (lr.endDate < :startDate OR lr.startDate > :endDate)
        ORDER BY lr.startDate ASC
        """)
    List<LeaveRequest> findLeaveRequestsByDepartmentInDateRange(@Param("department") String department,
                                                              @Param("startDate") LocalDate startDate, 
                                                              @Param("endDate") LocalDate endDate);

    /**
     * Find emergency leave requests
     * @return List of emergency leave requests
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.isEmergencyLeave = true ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findEmergencyLeaveRequests();

    /**
     * Find backdated leave requests
     * @return List of backdated leave requests
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.isBackdated = true ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findBackdatedLeaveRequests();

    /**
     * Find leave requests that need approval (pending status and not auto-approved)
     * @return List of leave requests needing approval
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.status = 'PENDING' AND lr.isEmergencyLeave = false ORDER BY lr.createdAt ASC")
    List<LeaveRequest> findRequestsNeedingApproval();

    /**
     * Find auto-approved leave requests
     * @return List of auto-approved leave requests
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.status = 'AUTO_APPROVED' ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findAutoApprovedRequests();

    /**
     * Count pending requests for a manager
     * @param managerId the manager's employee ID
     * @return number of pending requests
     */
    @Query("SELECT COUNT(lr) FROM LeaveRequest lr WHERE lr.employee.manager.employeeId = :managerId AND lr.status = 'PENDING'")
    long countPendingRequestsByManager(@Param("managerId") String managerId);

    /**
     * Count leave requests by employee and status
     * @param employeeId the employee's ID
     * @param status the leave status
     * @return number of leave requests matching the criteria
     */
    @Query("SELECT COUNT(lr) FROM LeaveRequest lr WHERE lr.employee.employeeId = :employeeId AND lr.status = :status")
    long countByEmployeeIdAndStatus(@Param("employeeId") String employeeId, @Param("status") LeaveStatus status);

    /**
     * Find leave requests approved by a specific manager
     * @param approverId the approver's employee ID
     * @return List of leave requests approved by the manager
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.approvedBy.employeeId = :approverId ORDER BY lr.approvedAt DESC")
    List<LeaveRequest> findRequestsApprovedBy(@Param("approverId") String approverId);

    /**
     * Find leave requests created within a time period
     * @param startDateTime the start of the time period
     * @param endDateTime the end of the time period
     * @return List of leave requests created within the period
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.createdAt BETWEEN :startDateTime AND :endDateTime ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findRequestsCreatedBetween(@Param("startDateTime") LocalDateTime startDateTime, 
                                                @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * Find upcoming leave requests (starting within next N days)
     * @param days number of days to look ahead
     * @return List of upcoming leave requests
     */
    @Query("""
        SELECT lr FROM LeaveRequest lr 
        WHERE lr.status IN ('APPROVED', 'AUTO_APPROVED')
        AND lr.startDate BETWEEN CURRENT_DATE AND :endDate
        ORDER BY lr.startDate ASC
        """)
    List<LeaveRequest> findUpcomingLeaveRequests(@Param("endDate") LocalDate endDate);

    /**
     * Find current active leave requests (ongoing today)
     * @param currentDate the current date
     * @return List of active leave requests
     */
    @Query("""
        SELECT lr FROM LeaveRequest lr 
        WHERE lr.status IN ('APPROVED', 'AUTO_APPROVED')
        AND lr.startDate <= :currentDate 
        AND lr.endDate >= :currentDate
        ORDER BY lr.startDate ASC
        """)
    List<LeaveRequest> findActiveLeaveRequests(@Param("currentDate") LocalDate currentDate);

    /**
     * Find leave requests by multiple statuses
     * @param statuses list of statuses to filter by
     * @return List of leave requests with any of the specified statuses
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.status IN :statuses ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findByStatusIn(@Param("statuses") List<LeaveStatus> statuses);

    /**
     * Calculate total leave days taken by employee in a year
     * @param employeeId the employee's ID
     * @param year the year to calculate for
     * @return total days taken in the year
     */
    @Query("""
        SELECT COALESCE(SUM(lr.totalDays), 0) FROM LeaveRequest lr 
        WHERE lr.employee.employeeId = :employeeId 
        AND lr.status IN ('APPROVED', 'AUTO_APPROVED')
        AND YEAR(lr.startDate) = :year
        """)
    Double calculateTotalDaysTakenInYear(@Param("employeeId") String employeeId, @Param("year") Integer year);

    /**
     * Calculate total leave days taken by employee and leave type in a year
     * @param employeeId the employee's ID
     * @param leaveType the leave type
     * @param year the year to calculate for
     * @return total days taken for the leave type in the year
     */
    @Query("""
        SELECT COALESCE(SUM(lr.totalDays), 0) FROM LeaveRequest lr 
        WHERE lr.employee.employeeId = :employeeId 
        AND lr.leaveType = :leaveType
        AND lr.status IN ('APPROVED', 'AUTO_APPROVED')
        AND YEAR(lr.startDate) = :year
        """)
    Double calculateTotalDaysTakenByTypeInYear(@Param("employeeId") String employeeId, 
                                             @Param("leaveType") LeaveType leaveType, 
                                             @Param("year") Integer year);

    /**
     * Find leave requests that conflict with team coverage
     * (multiple people from same department on leave on same dates)
     * @param department the department name
     * @param startDate the start date to check
     * @param endDate the end date to check
     * @param minCount minimum number of people for it to be considered a conflict
     * @return List of conflicting leave requests
     */
    @Query("""
        SELECT lr FROM LeaveRequest lr 
        WHERE lr.employee.department = :department
        AND lr.status IN ('APPROVED', 'AUTO_APPROVED')
        AND NOT (lr.endDate < :startDate OR lr.startDate > :endDate)
        AND (
            SELECT COUNT(lr2) FROM LeaveRequest lr2 
            WHERE lr2.employee.department = :department
            AND lr2.status IN ('APPROVED', 'AUTO_APPROVED')
            AND NOT (lr2.endDate < :startDate OR lr2.startDate > :endDate)
        ) >= :minCount
        ORDER BY lr.startDate ASC
        """)
    List<LeaveRequest> findConflictingLeaveRequests(@Param("department") String department,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate,
                                                  @Param("minCount") Long minCount);

    /**
     * Find leave requests requiring urgent attention (emergency or same-day requests)
     * @return List of urgent leave requests
     */
    @Query("""
        SELECT lr FROM LeaveRequest lr 
        WHERE lr.status = 'PENDING' 
        AND (lr.isEmergencyLeave = true OR lr.startDate = CURRENT_DATE)
        ORDER BY lr.createdAt ASC
        """)
    List<LeaveRequest> findUrgentLeaveRequests();
}