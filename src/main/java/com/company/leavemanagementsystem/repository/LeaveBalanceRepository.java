package com.company.leavemanagementsystem.repository;

import com.company.leavemanagementsystem.entity.Employee;
import com.company.leavemanagementsystem.entity.LeaveBalance;
import com.company.leavemanagementsystem.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for LeaveBalance entity operations.
 * Provides methods for balance calculations, updates, and year-based balance retrieval.
 */
@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    /**
     * Find leave balance by employee and leave type for current year
     * @param employee the employee
     * @param leaveType the leave type
     * @param year the year
     * @return Optional containing the leave balance if found
     */
    Optional<LeaveBalance> findByEmployeeAndLeaveTypeAndYear(Employee employee, LeaveType leaveType, Integer year);

    /**
     * Find leave balance by employee ID and leave type for specific year
     * @param employeeId the employee's ID
     * @param leaveType the leave type
     * @param year the year
     * @return Optional containing the leave balance if found
     */
    @Query("SELECT lb FROM LeaveBalance lb WHERE lb.employee.employeeId = :employeeId AND lb.leaveType = :leaveType AND lb.year = :year")
    Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeAndYear(@Param("employeeId") String employeeId, 
                                                             @Param("leaveType") LeaveType leaveType, 
                                                             @Param("year") Integer year);

    /**
     * Find all leave balances for a specific employee
     * @param employee the employee
     * @return List of all leave balances for the employee
     */
    List<LeaveBalance> findByEmployee(Employee employee);

    /**
     * Find all leave balances for a specific employee by employee ID
     * @param employeeId the employee's ID
     * @return List of all leave balances for the employee
     */
    @Query("SELECT lb FROM LeaveBalance lb WHERE lb.employee.employeeId = :employeeId")
    List<LeaveBalance> findByEmployeeId(@Param("employeeId") String employeeId);

    /**
     * Find all leave balances for a specific employee and year
     * @param employeeId the employee's ID
     * @param year the year
     * @return List of leave balances for the employee in the specified year
     */
    @Query("SELECT lb FROM LeaveBalance lb WHERE lb.employee.employeeId = :employeeId AND lb.year = :year")
    List<LeaveBalance> findByEmployeeIdAndYear(@Param("employeeId") String employeeId, @Param("year") Integer year);

    /**
     * Find all leave balances for a specific year
     * @param year the year
     * @return List of all leave balances for the specified year
     */
    List<LeaveBalance> findByYear(Integer year);

    /**
     * Find all leave balances for a specific leave type
     * @param leaveType the leave type
     * @return List of all leave balances for the specified leave type
     */
    List<LeaveBalance> findByLeaveType(LeaveType leaveType);

    /**
     * Find all leave balances for a specific leave type and year
     * @param leaveType the leave type
     * @param year the year
     * @return List of leave balances for the leave type in the specified year
     */
    List<LeaveBalance> findByLeaveTypeAndYear(LeaveType leaveType, Integer year);

    /**
     * Check if leave balance exists for employee, leave type, and year
     * @param employeeId the employee's ID
     * @param leaveType the leave type
     * @param year the year
     * @return true if balance exists, false otherwise
     */
    @Query("SELECT COUNT(lb) > 0 FROM LeaveBalance lb WHERE lb.employee.employeeId = :employeeId AND lb.leaveType = :leaveType AND lb.year = :year")
    boolean existsByEmployeeIdAndLeaveTypeAndYear(@Param("employeeId") String employeeId, 
                                                @Param("leaveType") LeaveType leaveType, 
                                                @Param("year") Integer year);

    /**
     * Find employees with low leave balance (less than specified days)
     * @param threshold the threshold for low balance
     * @param year the year to check
     * @return List of leave balances below the threshold
     */
    @Query("SELECT lb FROM LeaveBalance lb WHERE lb.availableDays < :threshold AND lb.year = :year")
    List<LeaveBalance> findLowBalances(@Param("threshold") Double threshold, @Param("year") Integer year);

    /**
     * Find employees with low leave balance for specific leave type
     * @param leaveType the leave type
     * @param threshold the threshold for low balance
     * @param year the year to check
     * @return List of leave balances below the threshold for the leave type
     */
    @Query("SELECT lb FROM LeaveBalance lb WHERE lb.leaveType = :leaveType AND lb.availableDays < :threshold AND lb.year = :year")
    List<LeaveBalance> findLowBalancesByType(@Param("leaveType") LeaveType leaveType, 
                                           @Param("threshold") Double threshold, 
                                           @Param("year") Integer year);

    /**
     * Find employees with zero available balance
     * @param year the year to check
     * @return List of leave balances with zero available days
     */
    @Query("SELECT lb FROM LeaveBalance lb WHERE lb.availableDays = 0 AND lb.year = :year")
    List<LeaveBalance> findZeroBalances(@Param("year") Integer year);

    /**
     * Find employees with unused leave balance (high available days)
     * @param threshold the threshold for high balance
     * @param year the year to check
     * @return List of leave balances above the threshold
     */
    @Query("SELECT lb FROM LeaveBalance lb WHERE lb.availableDays > :threshold AND lb.year = :year")
    List<LeaveBalance> findHighBalances(@Param("threshold") Double threshold, @Param("year") Integer year);

    /**
     * Calculate total available days for an employee across all leave types
     * @param employeeId the employee's ID
     * @param year the year
     * @return total available days across all leave types
     */
    @Query("SELECT COALESCE(SUM(lb.availableDays), 0) FROM LeaveBalance lb WHERE lb.employee.employeeId = :employeeId AND lb.year = :year")
    Double calculateTotalAvailableDays(@Param("employeeId") String employeeId, @Param("year") Integer year);

    /**
     * Calculate total used days for an employee across all leave types
     * @param employeeId the employee's ID
     * @param year the year
     * @return total used days across all leave types
     */
    @Query("SELECT COALESCE(SUM(lb.usedDays), 0) FROM LeaveBalance lb WHERE lb.employee.employeeId = :employeeId AND lb.year = :year")
    Double calculateTotalUsedDays(@Param("employeeId") String employeeId, @Param("year") Integer year);

    /**
     * Update used days for a specific leave balance
     * @param employeeId the employee's ID
     * @param leaveType the leave type
     * @param year the year
     * @param usedDays the new used days value
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE LeaveBalance lb SET lb.usedDays = :usedDays, lb.availableDays = lb.totalDays - :usedDays WHERE lb.employee.employeeId = :employeeId AND lb.leaveType = :leaveType AND lb.year = :year")
    int updateUsedDays(@Param("employeeId") String employeeId, 
                      @Param("leaveType") LeaveType leaveType, 
                      @Param("year") Integer year, 
                      @Param("usedDays") Double usedDays);

    /**
     * Increment used days for a specific leave balance
     * @param employeeId the employee's ID
     * @param leaveType the leave type
     * @param year the year
     * @param additionalDays the days to add to used days
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE LeaveBalance lb SET lb.usedDays = lb.usedDays + :additionalDays, lb.availableDays = lb.totalDays - (lb.usedDays + :additionalDays) WHERE lb.employee.employeeId = :employeeId AND lb.leaveType = :leaveType AND lb.year = :year")
    int incrementUsedDays(@Param("employeeId") String employeeId, 
                         @Param("leaveType") LeaveType leaveType, 
                         @Param("year") Integer year, 
                         @Param("additionalDays") Double additionalDays);

    /**
     * Decrement used days for a specific leave balance (for cancelled/rejected leaves)
     * @param employeeId the employee's ID
     * @param leaveType the leave type
     * @param year the year
     * @param daysToSubtract the days to subtract from used days
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE LeaveBalance lb SET lb.usedDays = GREATEST(0, lb.usedDays - :daysToSubtract), lb.availableDays = lb.totalDays - GREATEST(0, lb.usedDays - :daysToSubtract) WHERE lb.employee.employeeId = :employeeId AND lb.leaveType = :leaveType AND lb.year = :year")
    int decrementUsedDays(@Param("employeeId") String employeeId, 
                         @Param("leaveType") LeaveType leaveType, 
                         @Param("year") Integer year, 
                         @Param("daysToSubtract") Double daysToSubtract);

    /**
     * Find leave balances by department
     * @param department the department name
     * @param year the year
     * @return List of leave balances for employees in the department
     */
    @Query("SELECT lb FROM LeaveBalance lb WHERE lb.employee.department = :department AND lb.year = :year")
    List<LeaveBalance> findByDepartmentAndYear(@Param("department") String department, @Param("year") Integer year);

    /**
     * Calculate department-wise leave utilization
     * @param department the department name
     * @param year the year
     * @return average utilization percentage for the department
     */
    @Query("SELECT AVG((lb.usedDays / lb.totalDays) * 100) FROM LeaveBalance lb WHERE lb.employee.department = :department AND lb.year = :year AND lb.totalDays > 0")
    Double calculateDepartmentUtilization(@Param("department") String department, @Param("year") Integer year);

    /**
     * Find leave balances that need renewal (for year-end processing)
     * @param currentYear the current year
     * @return List of leave balances from the current year that need renewal
     */
    @Query("SELECT lb FROM LeaveBalance lb WHERE lb.year = :currentYear")
    List<LeaveBalance> findBalancesForRenewal(@Param("currentYear") Integer currentYear);

    /**
     * Count employees with leave balance for a specific year
     * @param year the year
     * @return number of employees with leave balance records
     */
    @Query("SELECT COUNT(DISTINCT lb.employee) FROM LeaveBalance lb WHERE lb.year = :year")
    long countEmployeesWithBalance(@Param("year") Integer year);

    /**
     * Find leave balances with high utilization (above threshold percentage)
     * @param utilizationThreshold the utilization threshold (0-100)
     * @param year the year
     * @return List of leave balances with high utilization
     */
    @Query("SELECT lb FROM LeaveBalance lb WHERE (lb.usedDays / lb.totalDays) * 100 > :utilizationThreshold AND lb.year = :year AND lb.totalDays > 0")
    List<LeaveBalance> findHighUtilizationBalances(@Param("utilizationThreshold") Double utilizationThreshold, @Param("year") Integer year);

    /**
     * Find leave balances with low utilization (below threshold percentage)
     * @param utilizationThreshold the utilization threshold (0-100)
     * @param year the year
     * @return List of leave balances with low utilization
     */
    @Query("SELECT lb FROM LeaveBalance lb WHERE (lb.usedDays / lb.totalDays) * 100 < :utilizationThreshold AND lb.year = :year AND lb.totalDays > 0")
    List<LeaveBalance> findLowUtilizationBalances(@Param("utilizationThreshold") Double utilizationThreshold, @Param("year") Integer year);

    /**
     * Get leave balance summary for reporting
     * @param year the year
     * @return List of leave balances with summary information
     */
    @Query("""
        SELECT lb FROM LeaveBalance lb 
        JOIN FETCH lb.employee e 
        WHERE lb.year = :year 
        ORDER BY e.department, e.name, lb.leaveType
        """)
    List<LeaveBalance> findBalanceSummaryForYear(@Param("year") Integer year);

    /**
     * Find employees who haven't initialized leave balance for a year
     * @param year the year to check
     * @return List of employees without leave balance records for the year
     */
    @Query("""
        SELECT e FROM Employee e 
        WHERE e.id NOT IN (
            SELECT DISTINCT lb.employee.id FROM LeaveBalance lb WHERE lb.year = :year
        )
        """)
    List<Employee> findEmployeesWithoutBalance(@Param("year") Integer year);

    /**
     * Delete leave balances for a specific year (for cleanup)
     * @param year the year to delete balances for
     * @return number of records deleted
     */
    @Modifying
    @Query("DELETE FROM LeaveBalance lb WHERE lb.year = :year")
    int deleteByYear(@Param("year") Integer year);
}