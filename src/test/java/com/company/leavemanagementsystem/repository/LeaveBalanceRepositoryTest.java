package com.company.leavemanagementsystem.repository;

import com.company.leavemanagementsystem.entity.Employee;
import com.company.leavemanagementsystem.entity.LeaveBalance;
import com.company.leavemanagementsystem.entity.LeaveType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test suite for LeaveBalanceRepository.
 * Tests balance calculations, updates, and year-based operations.
 */
@DataJpaTest
@ActiveProfiles("test")
class LeaveBalanceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    private Employee employee1;
    private Employee employee2;
    private Employee employee3;
    private LeaveBalance vacationBalance1;
    private LeaveBalance sickBalance1;
    private LeaveBalance vacationBalance2;

    @BeforeEach
    void setUp() {
        // Create test employees
        employee1 = new Employee("EMP001", "Alice Smith", "alice.smith@company.com", 
                                "Engineering", LocalDate.of(2021, 3, 10));
        
        employee2 = new Employee("EMP002", "Bob Johnson", "bob.johnson@company.com", 
                                "Engineering", LocalDate.of(2021, 6, 20));
        
        employee3 = new Employee("EMP003", "Carol Davis", "carol.davis@company.com", 
                                "Marketing", LocalDate.of(2022, 1, 5));
        
        // Persist employees
        entityManager.persistAndFlush(employee1);
        entityManager.persistAndFlush(employee2);
        entityManager.persistAndFlush(employee3);

        // Create test leave balances
        vacationBalance1 = new LeaveBalance(employee1, LeaveType.VACATION, 20.0, 2024);
        vacationBalance1.setUsedDays(5.0);
        vacationBalance1.setAvailableDays(15.0);

        sickBalance1 = new LeaveBalance(employee1, LeaveType.SICK, 10.0, 2024);
        sickBalance1.setUsedDays(2.0);
        sickBalance1.setAvailableDays(8.0);

        vacationBalance2 = new LeaveBalance(employee2, LeaveType.VACATION, 20.0, 2024);
        vacationBalance2.setUsedDays(0.0);
        vacationBalance2.setAvailableDays(20.0);

        // Persist leave balances
        entityManager.persistAndFlush(vacationBalance1);
        entityManager.persistAndFlush(sickBalance1);
        entityManager.persistAndFlush(vacationBalance2);
    }

    @Test
    void testFindByEmployeeAndLeaveTypeAndYear_WhenBalanceExists_ShouldReturnBalance() {
        // When
        Optional<LeaveBalance> result = leaveBalanceRepository.findByEmployeeAndLeaveTypeAndYear(
            employee1, LeaveType.VACATION, 2024);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getTotalDays()).isEqualTo(20.0);
        assertThat(result.get().getUsedDays()).isEqualTo(5.0);
        assertThat(result.get().getAvailableDays()).isEqualTo(15.0);
    }

    @Test
    void testFindByEmployeeAndLeaveTypeAndYear_WhenBalanceDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<LeaveBalance> result = leaveBalanceRepository.findByEmployeeAndLeaveTypeAndYear(
            employee1, LeaveType.PERSONAL, 2024);
        
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testFindByEmployeeIdAndLeaveTypeAndYear_WhenBalanceExists_ShouldReturnBalance() {
        // When
        Optional<LeaveBalance> result = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(
            "EMP001", LeaveType.SICK, 2024);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getTotalDays()).isEqualTo(10.0);
        assertThat(result.get().getUsedDays()).isEqualTo(2.0);
        assertThat(result.get().getAvailableDays()).isEqualTo(8.0);
    }

    @Test
    void testFindByEmployeeIdAndLeaveTypeAndYear_WhenBalanceDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<LeaveBalance> result = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(
            "NONEXISTENT", LeaveType.VACATION, 2024);
        
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testFindByEmployee_ShouldReturnAllBalancesForEmployee() {
        // When
        List<LeaveBalance> results = leaveBalanceRepository.findByEmployee(employee1);
        
        // Then
        assertThat(results).hasSize(2);
        assertThat(results)
            .extracting(LeaveBalance::getLeaveType)
            .containsExactlyInAnyOrder(LeaveType.VACATION, LeaveType.SICK);
    }

    @Test
    void testFindByEmployeeId_ShouldReturnAllBalancesForEmployee() {
        // When
        List<LeaveBalance> results = leaveBalanceRepository.findByEmployeeId("EMP001");
        
        // Then
        assertThat(results).hasSize(2);
        assertThat(results)
            .extracting(LeaveBalance::getLeaveType)
            .containsExactlyInAnyOrder(LeaveType.VACATION, LeaveType.SICK);
    }

    @Test
    void testFindByEmployeeIdAndYear_ShouldReturnBalancesForEmployeeAndYear() {
        // When
        List<LeaveBalance> results = leaveBalanceRepository.findByEmployeeIdAndYear("EMP001", 2024);
        
        // Then
        assertThat(results).hasSize(2);
        assertThat(results)
            .extracting(LeaveBalance::getYear)
            .containsOnly(2024);
    }

    @Test
    void testFindByYear_ShouldReturnAllBalancesForYear() {
        // When
        List<LeaveBalance> results = leaveBalanceRepository.findByYear(2024);
        
        // Then
        assertThat(results).hasSize(3); // All test balances are for 2024
        assertThat(results)
            .extracting(LeaveBalance::getYear)
            .containsOnly(2024);
    }

    @Test
    void testFindByLeaveType_ShouldReturnAllBalancesForLeaveType() {
        // When
        List<LeaveBalance> results = leaveBalanceRepository.findByLeaveType(LeaveType.VACATION);
        
        // Then
        assertThat(results).hasSize(2); // vacationBalance1 and vacationBalance2
        assertThat(results)
            .extracting(LeaveBalance::getLeaveType)
            .containsOnly(LeaveType.VACATION);
    }

    @Test
    void testFindByLeaveTypeAndYear_ShouldReturnBalancesForTypeAndYear() {
        // When
        List<LeaveBalance> results = leaveBalanceRepository.findByLeaveTypeAndYear(LeaveType.VACATION, 2024);
        
        // Then
        assertThat(results).hasSize(2);
        assertThat(results)
            .extracting(LeaveBalance::getLeaveType)
            .containsOnly(LeaveType.VACATION);
        assertThat(results)
            .extracting(LeaveBalance::getYear)
            .containsOnly(2024);
    }

    @Test
    void testExistsByEmployeeIdAndLeaveTypeAndYear_WhenExists_ShouldReturnTrue() {
        // When
        boolean exists = leaveBalanceRepository.existsByEmployeeIdAndLeaveTypeAndYear(
            "EMP001", LeaveType.VACATION, 2024);
        
        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByEmployeeIdAndLeaveTypeAndYear_WhenDoesNotExist_ShouldReturnFalse() {
        // When
        boolean exists = leaveBalanceRepository.existsByEmployeeIdAndLeaveTypeAndYear(
            "EMP001", LeaveType.PERSONAL, 2024);
        
        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void testFindLowBalances_ShouldReturnBalancesBelowThreshold() {
        // Given - Create a low balance
        LeaveBalance lowBalance = new LeaveBalance(employee3, LeaveType.VACATION, 20.0, 2024);
        lowBalance.setUsedDays(18.0);
        lowBalance.setAvailableDays(2.0);
        entityManager.persistAndFlush(lowBalance);
        
        // When
        List<LeaveBalance> results = leaveBalanceRepository.findLowBalances(5.0, 2024);
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAvailableDays()).isEqualTo(2.0);
        assertThat(results.get(0).getEmployee().getEmployeeId()).isEqualTo("EMP003");
    }

    @Test
    void testFindLowBalancesByType_ShouldReturnLowBalancesForSpecificType() {
        // Given - Create a low sick balance
        LeaveBalance lowSickBalance = new LeaveBalance(employee2, LeaveType.SICK, 10.0, 2024);
        lowSickBalance.setUsedDays(8.5);
        lowSickBalance.setAvailableDays(1.5);
        entityManager.persistAndFlush(lowSickBalance);
        
        // When
        List<LeaveBalance> results = leaveBalanceRepository.findLowBalancesByType(LeaveType.SICK, 5.0, 2024);
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getLeaveType()).isEqualTo(LeaveType.SICK);
        assertThat(results.get(0).getAvailableDays()).isEqualTo(1.5);
    }

    @Test
    void testFindZeroBalances_ShouldReturnBalancesWithZeroAvailableDays() {
        // Given - Create a zero balance
        LeaveBalance zeroBalance = new LeaveBalance(employee3, LeaveType.PERSONAL, 5.0, 2024);
        zeroBalance.setUsedDays(5.0);
        zeroBalance.setAvailableDays(0.0);
        entityManager.persistAndFlush(zeroBalance);
        
        // When
        List<LeaveBalance> results = leaveBalanceRepository.findZeroBalances(2024);
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAvailableDays()).isEqualTo(0.0);
        assertThat(results.get(0).getEmployee().getEmployeeId()).isEqualTo("EMP003");
    }

    @Test
    void testFindHighBalances_ShouldReturnBalancesAboveThreshold() {
        // When
        List<LeaveBalance> results = leaveBalanceRepository.findHighBalances(18.0, 2024);
        
        // Then
        assertThat(results).hasSize(1); // Only vacationBalance2 has 20.0 available days
        assertThat(results.get(0).getAvailableDays()).isEqualTo(20.0);
        assertThat(results.get(0).getEmployee().getEmployeeId()).isEqualTo("EMP002");
    }

    @Test
    void testCalculateTotalAvailableDays_ShouldReturnSumOfAvailableDays() {
        // When
        Double totalAvailable = leaveBalanceRepository.calculateTotalAvailableDays("EMP001", 2024);
        
        // Then
        assertThat(totalAvailable).isEqualTo(23.0); // 15.0 vacation + 8.0 sick
    }

    @Test
    void testCalculateTotalUsedDays_ShouldReturnSumOfUsedDays() {
        // When
        Double totalUsed = leaveBalanceRepository.calculateTotalUsedDays("EMP001", 2024);
        
        // Then
        assertThat(totalUsed).isEqualTo(7.0); // 5.0 vacation + 2.0 sick
    }

    @Test
    @Transactional
    void testUpdateUsedDays_ShouldUpdateUsedAndAvailableDays() {
        // When
        int updatedRows = leaveBalanceRepository.updateUsedDays("EMP001", LeaveType.VACATION, 2024, 8.0);
        entityManager.flush();
        entityManager.clear();
        
        // Then
        assertThat(updatedRows).isEqualTo(1);
        
        Optional<LeaveBalance> updated = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(
            "EMP001", LeaveType.VACATION, 2024);
        assertThat(updated).isPresent();
        assertThat(updated.get().getUsedDays()).isEqualTo(8.0);
        assertThat(updated.get().getAvailableDays()).isEqualTo(12.0); // 20.0 - 8.0
    }

    @Test
    @Transactional
    void testIncrementUsedDays_ShouldAddToUsedDays() {
        // When
        int updatedRows = leaveBalanceRepository.incrementUsedDays("EMP001", LeaveType.VACATION, 2024, 3.0);
        entityManager.flush();
        entityManager.clear();
        
        // Then
        assertThat(updatedRows).isEqualTo(1);
        
        Optional<LeaveBalance> updated = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(
            "EMP001", LeaveType.VACATION, 2024);
        assertThat(updated).isPresent();
        assertThat(updated.get().getUsedDays()).isEqualTo(8.0); // 5.0 + 3.0
        assertThat(updated.get().getAvailableDays()).isEqualTo(12.0); // 20.0 - 8.0
    }

    @Test
    @Transactional
    void testDecrementUsedDays_ShouldSubtractFromUsedDays() {
        // When
        int updatedRows = leaveBalanceRepository.decrementUsedDays("EMP001", LeaveType.VACATION, 2024, 2.0);
        entityManager.flush();
        entityManager.clear();
        
        // Then
        assertThat(updatedRows).isEqualTo(1);
        
        Optional<LeaveBalance> updated = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(
            "EMP001", LeaveType.VACATION, 2024);
        assertThat(updated).isPresent();
        assertThat(updated.get().getUsedDays()).isEqualTo(3.0); // 5.0 - 2.0
        assertThat(updated.get().getAvailableDays()).isEqualTo(17.0); // 20.0 - 3.0
    }

    @Test
    @Transactional
    void testDecrementUsedDays_ShouldNotGoBelowZero() {
        // When - Try to decrement more than available
        int updatedRows = leaveBalanceRepository.decrementUsedDays("EMP001", LeaveType.VACATION, 2024, 10.0);
        entityManager.flush();
        entityManager.clear();
        
        // Then
        assertThat(updatedRows).isEqualTo(1);
        
        Optional<LeaveBalance> updated = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(
            "EMP001", LeaveType.VACATION, 2024);
        assertThat(updated).isPresent();
        assertThat(updated.get().getUsedDays()).isEqualTo(0.0); // Should not go below 0
        assertThat(updated.get().getAvailableDays()).isEqualTo(20.0); // 20.0 - 0.0
    }

    @Test
    void testFindByDepartmentAndYear_ShouldReturnBalancesForDepartment() {
        // When
        List<LeaveBalance> results = leaveBalanceRepository.findByDepartmentAndYear("Engineering", 2024);
        
        // Then
        assertThat(results).hasSize(3); // 2 for EMP001 + 1 for EMP002
        assertThat(results)
            .extracting(lb -> lb.getEmployee().getDepartment())
            .containsOnly("Engineering");
    }

    @Test
    void testCalculateDepartmentUtilization_ShouldReturnAverageUtilization() {
        // When
        Double utilization = leaveBalanceRepository.calculateDepartmentUtilization("Engineering", 2024);
        
        // Then
        // EMP001: vacation 25% (5/20), sick 20% (2/10) = avg 22.5%
        // EMP002: vacation 0% (0/20) = 0%
        // Department average: (22.5 + 0) / 2 = 11.25%
        assertThat(utilization).isCloseTo(11.25, org.assertj.core.data.Offset.offset(0.1));
    }

    @Test
    void testFindBalancesForRenewal_ShouldReturnBalancesForCurrentYear() {
        // When
        List<LeaveBalance> results = leaveBalanceRepository.findBalancesForRenewal(2024);
        
        // Then
        assertThat(results).hasSize(3); // All test balances are for 2024
        assertThat(results)
            .extracting(LeaveBalance::getYear)
            .containsOnly(2024);
    }

    @Test
    void testCountEmployeesWithBalance_ShouldReturnDistinctEmployeeCount() {
        // When
        long count = leaveBalanceRepository.countEmployeesWithBalance(2024);
        
        // Then
        assertThat(count).isEqualTo(2); // employee1 and employee2 have balances
    }

    @Test
    void testFindHighUtilizationBalances_ShouldReturnBalancesAboveThreshold() {
        // When
        List<LeaveBalance> results = leaveBalanceRepository.findHighUtilizationBalances(20.0, 2024);
        
        // Then
        assertThat(results).hasSize(1); // Only vacation balance for EMP001 (25% utilization)
        assertThat(results.get(0).getEmployee().getEmployeeId()).isEqualTo("EMP001");
        assertThat(results.get(0).getLeaveType()).isEqualTo(LeaveType.VACATION);
    }

    @Test
    void testFindLowUtilizationBalances_ShouldReturnBalancesBelowThreshold() {
        // When
        List<LeaveBalance> results = leaveBalanceRepository.findLowUtilizationBalances(10.0, 2024);
        
        // Then
        assertThat(results).hasSize(1); // Only vacation balance for EMP002 (0% utilization)
        assertThat(results.get(0).getEmployee().getEmployeeId()).isEqualTo("EMP002");
        assertThat(results.get(0).getLeaveType()).isEqualTo(LeaveType.VACATION);
    }

    @Test
    void testFindBalanceSummaryForYear_ShouldReturnBalancesWithEmployeeInfo() {
        // When
        List<LeaveBalance> results = leaveBalanceRepository.findBalanceSummaryForYear(2024);
        
        // Then
        assertThat(results).hasSize(3);
        // Verify that employee information is fetched (no lazy loading exception)
        results.forEach(balance -> {
            assertThat(balance.getEmployee().getName()).isNotNull();
            assertThat(balance.getEmployee().getDepartment()).isNotNull();
        });
    }

    @Test
    void testFindEmployeesWithoutBalance_ShouldReturnEmployeesWithoutBalanceRecords() {
        // When
        List<Employee> results = leaveBalanceRepository.findEmployeesWithoutBalance(2024);
        
        // Then
        assertThat(results).hasSize(1); // Only employee3 has no balance records
        assertThat(results.get(0).getEmployeeId()).isEqualTo("EMP003");
    }

    @Test
    @Transactional
    void testDeleteByYear_ShouldDeleteAllBalancesForYear() {
        // Given - Create balances for different year
        LeaveBalance balance2023 = new LeaveBalance(employee1, LeaveType.VACATION, 20.0, 2023);
        entityManager.persistAndFlush(balance2023);
        
        // When
        int deletedRows = leaveBalanceRepository.deleteByYear(2023);
        entityManager.flush();
        
        // Then
        assertThat(deletedRows).isEqualTo(1);
        
        List<LeaveBalance> remaining2023 = leaveBalanceRepository.findByYear(2023);
        assertThat(remaining2023).isEmpty();
        
        List<LeaveBalance> remaining2024 = leaveBalanceRepository.findByYear(2024);
        assertThat(remaining2024).hasSize(3); // 2024 balances should remain
    }

    // Edge case tests
    @Test
    void testFindByEmployeeId_WithNonExistentEmployee_ShouldReturnEmptyList() {
        // When
        List<LeaveBalance> results = leaveBalanceRepository.findByEmployeeId("NONEXISTENT");
        
        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void testFindByYear_WithNonExistentYear_ShouldReturnEmptyList() {
        // When
        List<LeaveBalance> results = leaveBalanceRepository.findByYear(2025);
        
        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void testCalculateTotalAvailableDays_WithNonExistentEmployee_ShouldReturnZero() {
        // When
        Double total = leaveBalanceRepository.calculateTotalAvailableDays("NONEXISTENT", 2024);
        
        // Then
        assertThat(total).isEqualTo(0.0);
    }

    @Test
    @Transactional
    void testUpdateUsedDays_WithNonExistentBalance_ShouldReturnZero() {
        // When
        int updatedRows = leaveBalanceRepository.updateUsedDays("NONEXISTENT", LeaveType.VACATION, 2024, 5.0);
        
        // Then
        assertThat(updatedRows).isEqualTo(0);
    }

    @Test
    void testFindByDepartmentAndYear_WithNonExistentDepartment_ShouldReturnEmptyList() {
        // When
        List<LeaveBalance> results = leaveBalanceRepository.findByDepartmentAndYear("NonExistentDept", 2024);
        
        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void testCalculateDepartmentUtilization_WithNonExistentDepartment_ShouldReturnNull() {
        // When
        Double utilization = leaveBalanceRepository.calculateDepartmentUtilization("NonExistentDept", 2024);
        
        // Then
        assertThat(utilization).isNull();
    }

    // Database constraint tests
    @Test
    void testUniqueConstraintOnEmployeeLeaveTypeYear() {
        // Given
        LeaveBalance duplicateBalance = new LeaveBalance(employee1, LeaveType.VACATION, 25.0, 2024);
        
        // When & Then - Should throw exception due to unique constraint
        try {
            entityManager.persistAndFlush(duplicateBalance);
            assertThat(false).as("Expected constraint violation").isTrue();
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("could not execute statement");
        }
    }

    // Performance test
    @Test
    void testPerformanceWithLargeDataset() {
        // Given - Create 100 additional employees with balances
        for (int i = 100; i < 200; i++) {
            Employee emp = new Employee("EMP" + String.format("%03d", i), 
                                      "Employee " + i, 
                                      "emp" + i + "@company.com", 
                                      "Engineering", 
                                      LocalDate.of(2022, 1, 1));
            entityManager.persist(emp);
            
            LeaveBalance balance = new LeaveBalance(emp, LeaveType.VACATION, 20.0, 2024);
            balance.setUsedDays((double)(i % 10)); // Vary used days
            balance.setAvailableDays(20.0 - (i % 10));
            entityManager.persist(balance);
        }
        entityManager.flush();
        
        // When - Measure performance of department query
        long startTime = System.currentTimeMillis();
        List<LeaveBalance> results = leaveBalanceRepository.findByDepartmentAndYear("Engineering", 2024);
        long endTime = System.currentTimeMillis();
        
        // Then
        assertThat(results).hasSizeGreaterThan(100);
        assertThat(endTime - startTime).isLessThan(1000); // Should complete within 1 second
    }
}