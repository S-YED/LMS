package com.company.leavemanagementsystem.service;

import com.company.leavemanagementsystem.dto.CreateEmployeeRequest;
import com.company.leavemanagementsystem.dto.EmployeeResponse;
import com.company.leavemanagementsystem.dto.UpdateEmployeeRequest;
import com.company.leavemanagementsystem.entity.Employee;
import com.company.leavemanagementsystem.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for EmployeeService.
 * Tests all business logic, validation methods, and edge cases with mocking.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee testEmployee;
    private Employee testManager;
    private CreateEmployeeRequest createRequest;
    private UpdateEmployeeRequest updateRequest;

    @BeforeEach
    void setUp() {
        testManager = new Employee("MGR001", "John Manager", "john.manager@company.com", 
                                  "Engineering", LocalDate.of(2020, 1, 15));
        testManager.setId(1L);
        testManager.setCreatedAt(LocalDateTime.now().minusYears(1));
        testManager.setUpdatedAt(LocalDateTime.now().minusYears(1));

        testEmployee = new Employee("EMP001", "Alice Smith", "alice.smith@company.com", 
                                   "Engineering", LocalDate.of(2021, 3, 10));
        testEmployee.setId(2L);
        testEmployee.setManager(testManager);
        testEmployee.setCreatedAt(LocalDateTime.now().minusMonths(6));
        testEmployee.setUpdatedAt(LocalDateTime.now().minusMonths(6));

        createRequest = new CreateEmployeeRequest();
        createRequest.setEmployeeId("EMP002");
        createRequest.setName("Bob Johnson");
        createRequest.setEmail("bob.johnson@company.com");
        createRequest.setDepartment("Engineering");
        createRequest.setJoiningDate(LocalDate.of(2023, 1, 15));
        createRequest.setManagerId("MGR001");

        updateRequest = new UpdateEmployeeRequest();
        updateRequest.setName("Alice Johnson");
        updateRequest.setEmail("alice.johnson@company.com");
        updateRequest.setDepartment("Marketing");
        updateRequest.setJoiningDate(LocalDate.of(2021, 3, 10));
        updateRequest.setManagerId("MGR001");
    }

    @Test
    void testCreateEmployee_WhenValidRequest_ShouldCreateEmployee() {
        // Given
        when(employeeRepository.existsByEmployeeId("EMP002")).thenReturn(false);
        when(employeeRepository.existsByEmail("bob.johnson@company.com")).thenReturn(false);
        when(employeeRepository.findByEmployeeId("MGR001")).thenReturn(Optional.of(testManager));
        when(employeeRepository.wouldCreateCircularReference(anyString(), anyString())).thenReturn(false);
        
        Employee savedEmployee = new Employee("EMP002", "Bob Johnson", "bob.johnson@company.com", 
                                             "Engineering", LocalDate.of(2023, 1, 15));
        savedEmployee.setId(3L);
        savedEmployee.setManager(testManager);
        savedEmployee.setCreatedAt(LocalDateTime.now());
        savedEmployee.setUpdatedAt(LocalDateTime.now());
        
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);

        // When
        EmployeeResponse response = employeeService.createEmployee(createRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEmployeeId()).isEqualTo("EMP002");
        assertThat(response.getName()).isEqualTo("Bob Johnson");
        assertThat(response.getEmail()).isEqualTo("bob.johnson@company.com");
        assertThat(response.getDepartment()).isEqualTo("Engineering");
        assertThat(response.getManager()).isNotNull();
        assertThat(response.getManager().getEmployeeId()).isEqualTo("MGR001");

        verify(employeeRepository).existsByEmployeeId("EMP002");
        verify(employeeRepository).existsByEmail("bob.johnson@company.com");
        verify(employeeRepository).findByEmployeeId("MGR001");
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void testCreateEmployee_WhenDuplicateEmployeeId_ShouldThrowException() {
        // Given
        when(employeeRepository.existsByEmployeeId("EMP002")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> employeeService.createEmployee(createRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Employee ID already exists: EMP002");

        verify(employeeRepository).existsByEmployeeId("EMP002");
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void testCreateEmployee_WhenDuplicateEmail_ShouldThrowException() {
        // Given
        when(employeeRepository.existsByEmployeeId("EMP002")).thenReturn(false);
        when(employeeRepository.existsByEmail("bob.johnson@company.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> employeeService.createEmployee(createRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Email already exists: bob.johnson@company.com");

        verify(employeeRepository).existsByEmployeeId("EMP002");
        verify(employeeRepository).existsByEmail("bob.johnson@company.com");
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void testCreateEmployee_WhenManagerNotFound_ShouldThrowException() {
        // Given
        when(employeeRepository.existsByEmployeeId("EMP002")).thenReturn(false);
        when(employeeRepository.existsByEmail("bob.johnson@company.com")).thenReturn(false);
        when(employeeRepository.findByEmployeeId("MGR001")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> employeeService.createEmployee(createRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Manager not found with ID: MGR001");

        verify(employeeRepository).findByEmployeeId("MGR001");
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void testCreateEmployee_WhenCircularReference_ShouldThrowException() {
        // Given
        when(employeeRepository.existsByEmployeeId("EMP002")).thenReturn(false);
        when(employeeRepository.existsByEmail("bob.johnson@company.com")).thenReturn(false);
        when(employeeRepository.findByEmployeeId("MGR001")).thenReturn(Optional.of(testManager));
        when(employeeRepository.existsByEmployeeId("EMP002")).thenReturn(true);
        when(employeeRepository.wouldCreateCircularReference("EMP002", "MGR001")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> employeeService.createEmployee(createRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Manager assignment would create circular reference");

        verify(employeeRepository).wouldCreateCircularReference("EMP002", "MGR001");
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void testCreateEmployee_WhenSelfAssignedAsManager_ShouldThrowException() {
        // Given
        createRequest.setManagerId("EMP002"); // Same as employee ID
        when(employeeRepository.existsByEmployeeId("EMP002")).thenReturn(false);
        when(employeeRepository.existsByEmail("bob.johnson@company.com")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> employeeService.createEmployee(createRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Employee cannot be their own manager");

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void testCreateEmployee_WithoutManager_ShouldCreateEmployee() {
        // Given
        createRequest.setManagerId(null);
        when(employeeRepository.existsByEmployeeId("EMP002")).thenReturn(false);
        when(employeeRepository.existsByEmail("bob.johnson@company.com")).thenReturn(false);
        
        Employee savedEmployee = new Employee("EMP002", "Bob Johnson", "bob.johnson@company.com", 
                                             "Engineering", LocalDate.of(2023, 1, 15));
        savedEmployee.setId(3L);
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);

        // When
        EmployeeResponse response = employeeService.createEmployee(createRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getManager()).isNull();
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void testGetEmployeeByEmployeeId_WhenEmployeeExists_ShouldReturnEmployee() {
        // Given
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));

        // When
        EmployeeResponse response = employeeService.getEmployeeByEmployeeId("EMP001");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEmployeeId()).isEqualTo("EMP001");
        assertThat(response.getName()).isEqualTo("Alice Smith");
        verify(employeeRepository).findByEmployeeId("EMP001");
    }

    @Test
    void testGetEmployeeByEmployeeId_WhenEmployeeNotFound_ShouldThrowException() {
        // Given
        when(employeeRepository.findByEmployeeId("NONEXISTENT")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> employeeService.getEmployeeByEmployeeId("NONEXISTENT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Employee not found with ID: NONEXISTENT");

        verify(employeeRepository).findByEmployeeId("NONEXISTENT");
    }

    @Test
    void testGetEmployeeById_WhenEmployeeExists_ShouldReturnEmployee() {
        // Given
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(testEmployee));

        // When
        EmployeeResponse response = employeeService.getEmployeeById(2L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getEmployeeId()).isEqualTo("EMP001");
        verify(employeeRepository).findById(2L);
    }

    @Test
    void testGetEmployeeById_WhenEmployeeNotFound_ShouldThrowException() {
        // Given
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> employeeService.getEmployeeById(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Employee not found with ID: 999");

        verify(employeeRepository).findById(999L);
    }

    @Test
    void testGetAllEmployees_WithPagination_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Employee> employees = Arrays.asList(testEmployee, testManager);
        Page<Employee> employeePage = new PageImpl<>(employees, pageable, employees.size());
        when(employeeRepository.findAll(pageable)).thenReturn(employeePage);

        // When
        Page<EmployeeResponse> response = employeeService.getAllEmployees(pageable);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);
        verify(employeeRepository).findAll(pageable);
    }

    @Test
    void testGetAllEmployees_WithoutPagination_ShouldReturnAllEmployees() {
        // Given
        List<Employee> employees = Arrays.asList(testEmployee, testManager);
        when(employeeRepository.findAll()).thenReturn(employees);

        // When
        List<EmployeeResponse> response = employeeService.getAllEmployees();

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(2);
        verify(employeeRepository).findAll();
    }

    @Test
    void testUpdateEmployee_WhenValidRequest_ShouldUpdateEmployee() {
        // Given
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.findByEmail("alice.johnson@company.com")).thenReturn(Optional.empty());
        when(employeeRepository.findByEmployeeId("MGR001")).thenReturn(Optional.of(testManager));
        when(employeeRepository.wouldCreateCircularReference("EMP001", "MGR001")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);

        // When
        EmployeeResponse response = employeeService.updateEmployee("EMP001", updateRequest);

        // Then
        assertThat(response).isNotNull();
        verify(employeeRepository).findByEmployeeId("EMP001");
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void testUpdateEmployee_WhenEmployeeNotFound_ShouldThrowException() {
        // Given
        when(employeeRepository.findByEmployeeId("NONEXISTENT")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> employeeService.updateEmployee("NONEXISTENT", updateRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Employee not found with ID: NONEXISTENT");

        verify(employeeRepository).findByEmployeeId("NONEXISTENT");
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void testUpdateEmployee_WhenEmailAlreadyExists_ShouldThrowException() {
        // Given
        Employee otherEmployee = new Employee("EMP003", "Other Employee", "alice.johnson@company.com", 
                                             "HR", LocalDate.now());
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.findByEmail("alice.johnson@company.com")).thenReturn(Optional.of(otherEmployee));

        // When & Then
        assertThatThrownBy(() -> employeeService.updateEmployee("EMP001", updateRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Email already exists: alice.johnson@company.com");

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void testDeleteEmployee_WhenEmployeeExists_ShouldDeleteEmployee() {
        // Given
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.findByManagerEmployeeId("EMP001")).thenReturn(Collections.emptyList());

        // When
        employeeService.deleteEmployee("EMP001");

        // Then
        verify(employeeRepository).findByEmployeeId("EMP001");
        verify(employeeRepository).findByManagerEmployeeId("EMP001");
        verify(employeeRepository).delete(testEmployee);
    }

    @Test
    void testDeleteEmployee_WhenEmployeeNotFound_ShouldThrowException() {
        // Given
        when(employeeRepository.findByEmployeeId("NONEXISTENT")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> employeeService.deleteEmployee("NONEXISTENT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Employee not found with ID: NONEXISTENT");

        verify(employeeRepository).findByEmployeeId("NONEXISTENT");
        verify(employeeRepository, never()).delete(any(Employee.class));
    }

    @Test
    void testDeleteEmployee_WhenEmployeeHasSubordinates_ShouldThrowException() {
        // Given
        Employee subordinate = new Employee("EMP002", "Subordinate", "sub@company.com", 
                                           "Engineering", LocalDate.now());
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.findByManagerEmployeeId("EMP001")).thenReturn(Arrays.asList(subordinate));

        // When & Then
        assertThatThrownBy(() -> employeeService.deleteEmployee("EMP001"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot delete employee with active subordinates");

        verify(employeeRepository, never()).delete(any(Employee.class));
    }

    @Test
    void testGetEmployeesByDepartment_ShouldReturnDepartmentEmployees() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Employee> employees = Arrays.asList(testEmployee);
        Page<Employee> employeePage = new PageImpl<>(employees, pageable, employees.size());
        when(employeeRepository.findByDepartment("Engineering", pageable)).thenReturn(employeePage);

        // When
        Page<EmployeeResponse> response = employeeService.getEmployeesByDepartment("Engineering", pageable);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        verify(employeeRepository).findByDepartment("Engineering", pageable);
    }

    @Test
    void testGetSubordinates_WhenManagerExists_ShouldReturnSubordinates() {
        // Given
        Employee subordinate = new Employee("EMP002", "Subordinate", "sub@company.com", 
                                           "Engineering", LocalDate.now());
        when(employeeRepository.existsByEmployeeId("MGR001")).thenReturn(true);
        when(employeeRepository.findByManagerEmployeeId("MGR001")).thenReturn(Arrays.asList(subordinate));

        // When
        List<EmployeeResponse> response = employeeService.getSubordinates("MGR001");

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        verify(employeeRepository).existsByEmployeeId("MGR001");
        verify(employeeRepository).findByManagerEmployeeId("MGR001");
    }

    @Test
    void testGetSubordinates_WhenManagerNotFound_ShouldThrowException() {
        // Given
        when(employeeRepository.existsByEmployeeId("NONEXISTENT")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> employeeService.getSubordinates("NONEXISTENT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Manager not found with ID: NONEXISTENT");

        verify(employeeRepository).existsByEmployeeId("NONEXISTENT");
        verify(employeeRepository, never()).findByManagerEmployeeId(anyString());
    }

    @Test
    void testSearchEmployeesByName_ShouldReturnMatchingEmployees() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Employee> employees = Arrays.asList(testEmployee);
        Page<Employee> employeePage = new PageImpl<>(employees, pageable, employees.size());
        when(employeeRepository.findByNameContainingIgnoreCase("Alice", pageable)).thenReturn(employeePage);

        // When
        Page<EmployeeResponse> response = employeeService.searchEmployeesByName("Alice", pageable);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        verify(employeeRepository).findByNameContainingIgnoreCase("Alice", pageable);
    }

    @Test
    void testGetAllDepartments_ShouldReturnDepartmentList() {
        // Given
        List<String> departments = Arrays.asList("Engineering", "Marketing", "HR");
        when(employeeRepository.findAllDepartments()).thenReturn(departments);

        // When
        List<String> response = employeeService.getAllDepartments();

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(3);
        assertThat(response).containsExactlyInAnyOrder("Engineering", "Marketing", "HR");
        verify(employeeRepository).findAllDepartments();
    }

    @Test
    void testEmployeeExists_WhenEmployeeExists_ShouldReturnTrue() {
        // Given
        when(employeeRepository.existsByEmployeeId("EMP001")).thenReturn(true);

        // When
        boolean exists = employeeService.employeeExists("EMP001");

        // Then
        assertThat(exists).isTrue();
        verify(employeeRepository).existsByEmployeeId("EMP001");
    }

    @Test
    void testEmployeeExists_WhenEmployeeDoesNotExist_ShouldReturnFalse() {
        // Given
        when(employeeRepository.existsByEmployeeId("NONEXISTENT")).thenReturn(false);

        // When
        boolean exists = employeeService.employeeExists("NONEXISTENT");

        // Then
        assertThat(exists).isFalse();
        verify(employeeRepository).existsByEmployeeId("NONEXISTENT");
    }

    @Test
    void testGetEmployeeEntityByEmployeeId_WhenEmployeeExists_ShouldReturnEntity() {
        // Given
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));

        // When
        Employee response = employeeService.getEmployeeEntityByEmployeeId("EMP001");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEmployeeId()).isEqualTo("EMP001");
        verify(employeeRepository).findByEmployeeId("EMP001");
    }

    @Test
    void testGetEmployeeEntityByEmployeeId_WhenEmployeeNotFound_ShouldThrowException() {
        // Given
        when(employeeRepository.findByEmployeeId("NONEXISTENT")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> employeeService.getEmployeeEntityByEmployeeId("NONEXISTENT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Employee not found with ID: NONEXISTENT");

        verify(employeeRepository).findByEmployeeId("NONEXISTENT");
    }

    // Edge case tests
    @Test
    void testCreateEmployee_WithEmptyManagerId_ShouldCreateWithoutManager() {
        // Given
        createRequest.setManagerId(""); // Empty string
        when(employeeRepository.existsByEmployeeId("EMP002")).thenReturn(false);
        when(employeeRepository.existsByEmail("bob.johnson@company.com")).thenReturn(false);
        
        Employee savedEmployee = new Employee("EMP002", "Bob Johnson", "bob.johnson@company.com", 
                                             "Engineering", LocalDate.of(2023, 1, 15));
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);

        // When
        EmployeeResponse response = employeeService.createEmployee(createRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getManager()).isNull();
        verify(employeeRepository, never()).findByEmployeeId(anyString());
    }

    @Test
    void testUpdateEmployee_WithSameEmail_ShouldNotThrowException() {
        // Given
        updateRequest.setEmail("alice.smith@company.com"); // Same as current email
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.findByEmail("alice.smith@company.com")).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);

        // When
        EmployeeResponse response = employeeService.updateEmployee("EMP001", updateRequest);

        // Then
        assertThat(response).isNotNull();
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void testUpdateEmployee_WithNullManagerId_ShouldRemoveManager() {
        // Given
        updateRequest.setManagerId(null);
        when(employeeRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.findByEmail("alice.johnson@company.com")).thenReturn(Optional.empty());
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);

        // When
        EmployeeResponse response = employeeService.updateEmployee("EMP001", updateRequest);

        // Then
        assertThat(response).isNotNull();
        verify(employeeRepository).save(any(Employee.class));
        verify(employeeRepository, never()).findByEmployeeId("MGR001");
    }
}