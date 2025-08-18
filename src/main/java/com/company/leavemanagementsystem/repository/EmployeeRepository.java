package com.company.leavemanagementsystem.repository;

import com.company.leavemanagementsystem.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Employee entity operations.
 * Provides basic CRUD operations and custom query methods for employee management.
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Find employee by employee ID
     * @param employeeId the unique employee identifier
     * @return Optional containing the employee if found
     */
    Optional<Employee> findByEmployeeId(String employeeId);

    /**
     * Find employee by email address
     * @param email the employee's email address
     * @return Optional containing the employee if found
     */
    Optional<Employee> findByEmail(String email);

    /**
     * Check if employee exists by employee ID
     * @param employeeId the unique employee identifier
     * @return true if employee exists, false otherwise
     */
    boolean existsByEmployeeId(String employeeId);

    /**
     * Check if employee exists by email
     * @param email the employee's email address
     * @return true if employee exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Find all employees by department
     * @param department the department name
     * @return List of employees in the specified department
     */
    List<Employee> findByDepartment(String department);

    /**
     * Find all employees by department with pagination
     * @param department the department name
     * @param pageable pagination information
     * @return Page of employees in the specified department
     */
    Page<Employee> findByDepartment(String department, Pageable pageable);

    /**
     * Find all direct subordinates of a manager
     * @param manager the manager employee
     * @return List of direct subordinates
     */
    List<Employee> findByManager(Employee manager);

    /**
     * Find all direct subordinates by manager's employee ID
     * @param managerId the manager's employee ID
     * @return List of direct subordinates
     */
    @Query("SELECT e FROM Employee e WHERE e.manager.employeeId = :managerId")
    List<Employee> findByManagerEmployeeId(@Param("managerId") String managerId);

    /**
     * Find all employees who have a specific manager (by manager ID)
     * @param managerId the manager's employee ID
     * @param pageable pagination information
     * @return Page of employees under the specified manager
     */
    @Query("SELECT e FROM Employee e WHERE e.manager.employeeId = :managerId")
    Page<Employee> findByManagerEmployeeId(@Param("managerId") String managerId, Pageable pageable);

    /**
     * Find all employees without a manager (top-level employees)
     * @return List of employees without managers
     */
    List<Employee> findByManagerIsNull();

    /**
     * Check if an employee is a manager of another employee
     * @param managerId the potential manager's employee ID
     * @param subordinateId the potential subordinate's employee ID
     * @return true if the first employee is manager of the second
     */
    @Query("SELECT COUNT(e) > 0 FROM Employee e WHERE e.employeeId = :subordinateId AND e.manager.employeeId = :managerId")
    boolean isManagerOf(@Param("managerId") String managerId, @Param("subordinateId") String subordinateId);

    /**
     * Find all employees in the management hierarchy under a specific manager
     * This includes direct and indirect subordinates
     * @param managerId the top-level manager's employee ID
     * @return List of all subordinates in the hierarchy
     */
    @Query(value = """
        WITH RECURSIVE employee_hierarchy AS (
            SELECT id, employee_id, name, email, department, joining_date, manager_id, 0 as level
            FROM employees 
            WHERE employee_id = :managerId
            
            UNION ALL
            
            SELECT e.id, e.employee_id, e.name, e.email, e.department, e.joining_date, e.manager_id, eh.level + 1
            FROM employees e
            INNER JOIN employee_hierarchy eh ON e.manager_id = eh.id
        )
        SELECT * FROM employee_hierarchy WHERE level > 0
        """, nativeQuery = true)
    List<Employee> findAllSubordinatesInHierarchy(@Param("managerId") String managerId);

    /**
     * Find the management chain for an employee (all managers up the hierarchy)
     * @param employeeId the employee's ID
     * @return List of managers in the chain from direct manager to top-level
     */
    @Query(value = """
        WITH RECURSIVE manager_hierarchy AS (
            SELECT id, employee_id, name, email, department, joining_date, manager_id, 0 as level
            FROM employees 
            WHERE employee_id = :employeeId
            
            UNION ALL
            
            SELECT e.id, e.employee_id, e.name, e.email, e.department, e.joining_date, e.manager_id, mh.level + 1
            FROM employees e
            INNER JOIN manager_hierarchy mh ON e.id = mh.manager_id
        )
        SELECT * FROM manager_hierarchy WHERE level > 0
        """, nativeQuery = true)
    List<Employee> findManagerHierarchy(@Param("employeeId") String employeeId);

    /**
     * Check if there would be a circular reference if setting a new manager
     * @param employeeId the employee who would get a new manager
     * @param newManagerId the potential new manager's ID
     * @return true if setting this manager would create a circular reference
     */
    @Query(value = """
        WITH RECURSIVE manager_check AS (
            SELECT id, employee_id, manager_id, 0 as level
            FROM employees 
            WHERE employee_id = :newManagerId
            
            UNION ALL
            
            SELECT e.id, e.employee_id, e.manager_id, mc.level + 1
            FROM employees e
            INNER JOIN manager_check mc ON e.id = mc.manager_id
            WHERE mc.level < 10
        )
        SELECT COUNT(*) > 0 FROM manager_check WHERE employee_id = :employeeId
        """, nativeQuery = true)
    boolean wouldCreateCircularReference(@Param("employeeId") String employeeId, @Param("newManagerId") String newManagerId);

    /**
     * Find employees by name containing the search term (case-insensitive)
     * @param name the search term
     * @return List of employees whose names contain the search term
     */
    @Query("SELECT e FROM Employee e WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Employee> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Find employees by name containing the search term with pagination
     * @param name the search term
     * @param pageable pagination information
     * @return Page of employees whose names contain the search term
     */
    @Query("SELECT e FROM Employee e WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Employee> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);

    /**
     * Count total employees by department
     * @param department the department name
     * @return number of employees in the department
     */
    long countByDepartment(String department);

    /**
     * Count direct subordinates of a manager
     * @param managerId the manager's employee ID
     * @return number of direct subordinates
     */
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.manager.employeeId = :managerId")
    long countSubordinates(@Param("managerId") String managerId);

    /**
     * Find employees who joined after a specific date
     * @param joiningDate the date to compare against
     * @return List of employees who joined after the specified date
     */
    @Query("SELECT e FROM Employee e WHERE e.joiningDate > :joiningDate")
    List<Employee> findEmployeesJoinedAfter(@Param("joiningDate") java.time.LocalDate joiningDate);

    /**
     * Find employees who joined before a specific date
     * @param joiningDate the date to compare against
     * @return List of employees who joined before the specified date
     */
    @Query("SELECT e FROM Employee e WHERE e.joiningDate < :joiningDate")
    List<Employee> findEmployeesJoinedBefore(@Param("joiningDate") java.time.LocalDate joiningDate);

    /**
     * Find all distinct departments
     * @return List of all unique department names
     */
    @Query("SELECT DISTINCT e.department FROM Employee e ORDER BY e.department")
    List<String> findAllDepartments();
}