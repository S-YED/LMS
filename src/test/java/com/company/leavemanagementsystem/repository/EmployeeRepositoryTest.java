package com.company.leavemanagementsystem.repository;

import com.company.leavemanagementsystem.entity.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test suite for EmployeeRepository.
 * Tests all custom query methods, edge cases, and database constraints.
 */
@DataJpaTest
@ActiveProfiles("test")
class EmployeeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EmployeeRepository employeeRepository;

    private Employee manager;
    private Employee employee1;
    private Employee employee2;
    private Employee employee3;

    @BeforeEach
    void setUp() {
        // Create test data
        manager = new Employee("MGR001", "John Manager", "john.manager@company.com", 
                              "Engineering", LocalDate.of(2020, 1, 15));
        
        employee1 = new Employee("EMP001", "Alice Smith", "alice.smith@company.com", 
                                "Engineering", LocalDate.of(2021, 3, 10));
        employee1.setManager(manager);
        
        employee2 = new Employee("EMP002", "Bob Johnson", "bob.johnson@company.com", 
                                "Engineering", LocalDate.of(2021, 6, 20));
        employee2.setManager(manager);
        
        employee3 = new Employee("EMP003", "Carol Davis", "carol.davis@company.com", 
                                "Marketing", LocalDate.of(2022, 1, 5));
        
        // Persist test data
        entityManager.persistAndFlush(manager);
        entityManager.persistAndFlush(employee1);
        entityManager.persistAndFlush(employee2);
        entityManager.persistAndFlush(employee3);
    }

    @Test
    void testFindByEmployeeId_WhenEmployeeExists_ShouldReturnEmployee() {
        // When
        Optional<Employee> result = employeeRepository.findByEmployeeId("EMP001");
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Alice Smith");
        assertThat(result.get().getEmail()).isEqualTo("alice.smith@company.com");
    }

    @Test
    void testFindByEmployeeId_WhenEmployeeDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<Employee> result = employeeRepository.findByEmployeeId("NONEXISTENT");
        
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testFindByEmail_WhenEmailExists_ShouldReturnEmployee() {
        // When
        Optional<Employee> result = employeeRepository.findByEmail("bob.johnson@company.com");
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmployeeId()).isEqualTo("EMP002");
        assertThat(result.get().getName()).isEqualTo("Bob Johnson");
    }

    @Test
    void testFindByEmail_WhenEmailDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<Employee> result = employeeRepository.findByEmail("nonexistent@company.com");
        
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testExistsByEmployeeId_WhenEmployeeExists_ShouldReturnTrue() {
        // When
        boolean exists = employeeRepository.existsByEmployeeId("EMP001");
        
        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByEmployeeId_WhenEmployeeDoesNotExist_ShouldReturnFalse() {
        // When
        boolean exists = employeeRepository.existsByEmployeeId("NONEXISTENT");
        
        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void testExistsByEmail_WhenEmailExists_ShouldReturnTrue() {
        // When
        boolean exists = employeeRepository.existsByEmail("alice.smith@company.com");
        
        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByEmail_WhenEmailDoesNotExist_ShouldReturnFalse() {
        // When
        boolean exists = employeeRepository.existsByEmail("nonexistent@company.com");
        
        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void testFindByDepartment_ShouldReturnEmployeesInDepartment() {
        // When
        List<Employee> engineeringEmployees = employeeRepository.findByDepartment("Engineering");
        
        // Then
        assertThat(engineeringEmployees).hasSize(3); // manager + 2 employees
        assertThat(engineeringEmployees)
            .extracting(Employee::getEmployeeId)
            .containsExactlyInAnyOrder("MGR001", "EMP001", "EMP002");
    }

    @Test
    void testFindByDepartment_WithPagination_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);
        
        // When
        Page<Employee> result = employeeRepository.findByDepartment("Engineering", pageable);
        
        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void testFindByManager_ShouldReturnDirectSubordinates() {
        // When
        List<Employee> subordinates = employeeRepository.findByManager(manager);
        
        // Then
        assertThat(subordinates).hasSize(2);
        assertThat(subordinates)
            .extracting(Employee::getEmployeeId)
            .containsExactlyInAnyOrder("EMP001", "EMP002");
    }

    @Test
    void testFindByManagerEmployeeId_ShouldReturnDirectSubordinates() {
        // When
        List<Employee> subordinates = employeeRepository.findByManagerEmployeeId("MGR001");
        
        // Then
        assertThat(subordinates).hasSize(2);
        assertThat(subordinates)
            .extracting(Employee::getEmployeeId)
            .containsExactlyInAnyOrder("EMP001", "EMP002");
    }

    @Test
    void testFindByManagerEmployeeId_WithPagination_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 1);
        
        // When
        Page<Employee> result = employeeRepository.findByManagerEmployeeId("MGR001", pageable);
        
        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void testFindByManagerIsNull_ShouldReturnTopLevelEmployees() {
        // When
        List<Employee> topLevelEmployees = employeeRepository.findByManagerIsNull();
        
        // Then
        assertThat(topLevelEmployees).hasSize(2); // manager and employee3
        assertThat(topLevelEmployees)
            .extracting(Employee::getEmployeeId)
            .containsExactlyInAnyOrder("MGR001", "EMP003");
    }

    @Test
    void testIsManagerOf_WhenIsManager_ShouldReturnTrue() {
        // When
        boolean isManager = employeeRepository.isManagerOf("MGR001", "EMP001");
        
        // Then
        assertThat(isManager).isTrue();
    }

    @Test
    void testIsManagerOf_WhenNotManager_ShouldReturnFalse() {
        // When
        boolean isManager = employeeRepository.isManagerOf("EMP001", "EMP002");
        
        // Then
        assertThat(isManager).isFalse();
    }

    @Test
    void testFindByNameContainingIgnoreCase_ShouldReturnMatchingEmployees() {
        // When
        List<Employee> results = employeeRepository.findByNameContainingIgnoreCase("alice");
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEmployeeId()).isEqualTo("EMP001");
    }

    @Test
    void testFindByNameContainingIgnoreCase_WithPagination_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Employee> result = employeeRepository.findByNameContainingIgnoreCase("o", pageable);
        
        // Then
        assertThat(result.getContent()).hasSize(2); // John and Bob
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void testCountByDepartment_ShouldReturnCorrectCount() {
        // When
        long count = employeeRepository.countByDepartment("Engineering");
        
        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    void testCountSubordinates_ShouldReturnCorrectCount() {
        // When
        long count = employeeRepository.countSubordinates("MGR001");
        
        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void testFindEmployeesJoinedAfter_ShouldReturnEmployeesJoinedAfterDate() {
        // Given
        LocalDate cutoffDate = LocalDate.of(2021, 5, 1);
        
        // When
        List<Employee> results = employeeRepository.findEmployeesJoinedAfter(cutoffDate);
        
        // Then
        assertThat(results).hasSize(2); // EMP002 and EMP003
        assertThat(results)
            .extracting(Employee::getEmployeeId)
            .containsExactlyInAnyOrder("EMP002", "EMP003");
    }

    @Test
    void testFindEmployeesJoinedBefore_ShouldReturnEmployeesJoinedBeforeDate() {
        // Given
        LocalDate cutoffDate = LocalDate.of(2021, 5, 1);
        
        // When
        List<Employee> results = employeeRepository.findEmployeesJoinedBefore(cutoffDate);
        
        // Then
        assertThat(results).hasSize(2); // MGR001 and EMP001
        assertThat(results)
            .extracting(Employee::getEmployeeId)
            .containsExactlyInAnyOrder("MGR001", "EMP001");
    }

    @Test
    void testFindAllDepartments_ShouldReturnDistinctDepartments() {
        // When
        List<String> departments = employeeRepository.findAllDepartments();
        
        // Then
        assertThat(departments).hasSize(2);
        assertThat(departments).containsExactlyInAnyOrder("Engineering", "Marketing");
    }

    @Test
    void testWouldCreateCircularReference_WhenWouldCreateCircle_ShouldReturnTrue() {
        // Given - Create a potential circular reference scenario
        Employee midManager = new Employee("MID001", "Mid Manager", "mid@company.com", 
                                          "Engineering", LocalDate.of(2020, 6, 1));
        midManager.setManager(manager);
        entityManager.persistAndFlush(midManager);
        
        // When - Try to make manager report to midManager (would create circle)
        boolean wouldCreateCircle = employeeRepository.wouldCreateCircularReference("MGR001", "MID001");
        
        // Then
        assertThat(wouldCreateCircle).isTrue();
    }

    @Test
    void testWouldCreateCircularReference_WhenWouldNotCreateCircle_ShouldReturnFalse() {
        // When - Normal manager assignment
        boolean wouldCreateCircle = employeeRepository.wouldCreateCircularReference("EMP003", "MGR001");
        
        // Then
        assertThat(wouldCreateCircle).isFalse();
    }

    // Edge case tests
    @Test
    void testFindByDepartment_WithEmptyDepartment_ShouldReturnEmptyList() {
        // When
        List<Employee> results = employeeRepository.findByDepartment("NonExistentDepartment");
        
        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void testFindByManagerEmployeeId_WithNonExistentManager_ShouldReturnEmptyList() {
        // When
        List<Employee> results = employeeRepository.findByManagerEmployeeId("NONEXISTENT");
        
        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void testFindByNameContainingIgnoreCase_WithEmptyString_ShouldReturnAllEmployees() {
        // When
        List<Employee> results = employeeRepository.findByNameContainingIgnoreCase("");
        
        // Then
        assertThat(results).hasSize(4); // All employees
    }

    @Test
    void testCountByDepartment_WithNonExistentDepartment_ShouldReturnZero() {
        // When
        long count = employeeRepository.countByDepartment("NonExistentDepartment");
        
        // Then
        assertThat(count).isEqualTo(0);
    }

    @Test
    void testCountSubordinates_WithNonExistentManager_ShouldReturnZero() {
        // When
        long count = employeeRepository.countSubordinates("NONEXISTENT");
        
        // Then
        assertThat(count).isEqualTo(0);
    }

    // Performance test for large datasets
    @Test
    void testPerformanceWithLargeDataset() {
        // Given - Create 100 additional employees
        for (int i = 100; i < 200; i++) {
            Employee emp = new Employee("EMP" + String.format("%03d", i), 
                                      "Employee " + i, 
                                      "emp" + i + "@company.com", 
                                      "Engineering", 
                                      LocalDate.of(2022, 1, 1));
            emp.setManager(manager);
            entityManager.persist(emp);
        }
        entityManager.flush();
        
        // When - Measure performance of paginated query
        long startTime = System.currentTimeMillis();
        Page<Employee> result = employeeRepository.findByDepartment("Engineering", PageRequest.of(0, 20));
        long endTime = System.currentTimeMillis();
        
        // Then
        assertThat(result.getContent()).hasSize(20);
        assertThat(result.getTotalElements()).isEqualTo(103); // 3 original + 100 new
        assertThat(endTime - startTime).isLessThan(1000); // Should complete within 1 second
    }

    // Database constraint tests
    @Test
    void testUniqueConstraintOnEmployeeId() {
        // Given
        Employee duplicateEmployee = new Employee("EMP001", "Duplicate Employee", 
                                                 "duplicate@company.com", "HR", LocalDate.now());
        
        // When & Then - Should throw exception due to unique constraint
        try {
            entityManager.persistAndFlush(duplicateEmployee);
            assertThat(false).as("Expected constraint violation").isTrue();
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("could not execute statement");
        }
    }

    @Test
    void testUniqueConstraintOnEmail() {
        // Given
        Employee duplicateEmailEmployee = new Employee("EMP999", "Another Employee", 
                                                      "alice.smith@company.com", "HR", LocalDate.now());
        
        // When & Then - Should throw exception due to unique constraint
        try {
            entityManager.persistAndFlush(duplicateEmailEmployee);
            assertThat(false).as("Expected constraint violation").isTrue();
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("could not execute statement");
        }
    }
}