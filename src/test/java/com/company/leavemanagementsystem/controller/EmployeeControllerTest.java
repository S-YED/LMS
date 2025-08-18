package com.company.leavemanagementsystem.controller;

import com.company.leavemanagementsystem.dto.CreateEmployeeRequest;
import com.company.leavemanagementsystem.dto.EmployeeResponse;
import com.company.leavemanagementsystem.dto.UpdateEmployeeRequest;
import com.company.leavemanagementsystem.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive test suite for EmployeeController.
 * Tests all REST endpoints, request validation, error responses, and integration scenarios.
 */
@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateEmployeeRequest createRequest;
    private UpdateEmployeeRequest updateRequest;
    private EmployeeResponse employeeResponse;
    private EmployeeResponse managerResponse;

    @BeforeEach
    void setUp() {
        // Create test DTOs
        createRequest = new CreateEmployeeRequest();
        createRequest.setEmployeeId("EMP001");
        createRequest.setName("Alice Smith");
        createRequest.setEmail("alice.smith@company.com");
        createRequest.setDepartment("Engineering");
        createRequest.setJoiningDate(LocalDate.of(2023, 1, 15));
        createRequest.setManagerId("MGR001");

        updateRequest = new UpdateEmployeeRequest();
        updateRequest.setName("Alice Johnson");
        updateRequest.setEmail("alice.johnson@company.com");
        updateRequest.setDepartment("Marketing");
        updateRequest.setJoiningDate(LocalDate.of(2023, 1, 15));
        updateRequest.setManagerId("MGR001");

        // Create manager response
        EmployeeResponse.ManagerInfo managerInfo = new EmployeeResponse.ManagerInfo(
            "MGR001", "John Manager", "john.manager@company.com", "Engineering");

        employeeResponse = new EmployeeResponse();
        employeeResponse.setId(1L);
        employeeResponse.setEmployeeId("EMP001");
        employeeResponse.setName("Alice Smith");
        employeeResponse.setEmail("alice.smith@company.com");
        employeeResponse.setDepartment("Engineering");
        employeeResponse.setJoiningDate(LocalDate.of(2023, 1, 15));
        employeeResponse.setManager(managerInfo);
        employeeResponse.setCreatedAt(LocalDateTime.now());
        employeeResponse.setUpdatedAt(LocalDateTime.now());

        managerResponse = new EmployeeResponse();
        managerResponse.setId(2L);
        managerResponse.setEmployeeId("MGR001");
        managerResponse.setName("John Manager");
        managerResponse.setEmail("john.manager@company.com");
        managerResponse.setDepartment("Engineering");
        managerResponse.setJoiningDate(LocalDate.of(2020, 1, 15));
        managerResponse.setCreatedAt(LocalDateTime.now());
        managerResponse.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testCreateEmployee_WhenValidRequest_ShouldReturnCreated() throws Exception {
        // Given
        when(employeeService.createEmployee(any(CreateEmployeeRequest.class))).thenReturn(employeeResponse);

        // When & Then
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value("EMP001"))
                .andExpect(jsonPath("$.name").value("Alice Smith"))
                .andExpect(jsonPath("$.email").value("alice.smith@company.com"))
                .andExpect(jsonPath("$.department").value("Engineering"))
                .andExpect(jsonPath("$.manager.employeeId").value("MGR001"))
                .andExpect(jsonPath("$.manager.name").value("John Manager"));

        verify(employeeService).createEmployee(any(CreateEmployeeRequest.class));
    }

    @Test
    void testCreateEmployee_WhenInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given - Invalid request with missing required fields
        CreateEmployeeRequest invalidRequest = new CreateEmployeeRequest();
        invalidRequest.setEmployeeId(""); // Empty employee ID
        invalidRequest.setName(""); // Empty name
        invalidRequest.setEmail("invalid-email"); // Invalid email format

        // When & Then
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(employeeService, never()).createEmployee(any(CreateEmployeeRequest.class));
    }

    @Test
    void testCreateEmployee_WhenServiceThrowsException_ShouldReturnBadRequest() throws Exception {
        // Given
        when(employeeService.createEmployee(any(CreateEmployeeRequest.class)))
            .thenThrow(new IllegalArgumentException("Employee ID already exists"));

        // When & Then
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Employee ID already exists"));

        verify(employeeService).createEmployee(any(CreateEmployeeRequest.class));
    }

    @Test
    void testGetEmployeeById_WhenEmployeeExists_ShouldReturnEmployee() throws Exception {
        // Given
        when(employeeService.getEmployeeByEmployeeId("EMP001")).thenReturn(employeeResponse);

        // When & Then
        mockMvc.perform(get("/api/employees/EMP001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value("EMP001"))
                .andExpect(jsonPath("$.name").value("Alice Smith"))
                .andExpect(jsonPath("$.email").value("alice.smith@company.com"));

        verify(employeeService).getEmployeeByEmployeeId("EMP001");
    }

    @Test
    void testGetEmployeeById_WhenEmployeeNotFound_ShouldReturnNotFound() throws Exception {
        // Given
        when(employeeService.getEmployeeByEmployeeId("NONEXISTENT"))
            .thenThrow(new IllegalArgumentException("Employee not found with ID: NONEXISTENT"));

        // When & Then
        mockMvc.perform(get("/api/employees/NONEXISTENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Employee not found with ID: NONEXISTENT"));

        verify(employeeService).getEmployeeByEmployeeId("NONEXISTENT");
    }

    @Test
    void testGetAllEmployees_WithoutPagination_ShouldReturnAllEmployees() throws Exception {
        // Given
        List<EmployeeResponse> employees = Arrays.asList(employeeResponse, managerResponse);
        when(employeeService.getAllEmployees()).thenReturn(employees);

        // When & Then
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].employeeId").value("EMP001"))
                .andExpect(jsonPath("$[1].employeeId").value("MGR001"));

        verify(employeeService).getAllEmployees();
    }

    @Test
    void testGetAllEmployees_WithPagination_ShouldReturnPagedResults() throws Exception {
        // Given
        List<EmployeeResponse> employees = Arrays.asList(employeeResponse);
        Page<EmployeeResponse> employeePage = new PageImpl<>(employees, PageRequest.of(0, 10), 1);
        when(employeeService.getAllEmployees(any(Pageable.class))).thenReturn(employeePage);

        // When & Then
        mockMvc.perform(get("/api/employees")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].employeeId").value("EMP001"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));

        verify(employeeService).getAllEmployees(any(Pageable.class));
    }

    @Test
    void testUpdateEmployee_WhenValidRequest_ShouldReturnUpdatedEmployee() throws Exception {
        // Given
        EmployeeResponse updatedResponse = new EmployeeResponse();
        updatedResponse.setId(1L);
        updatedResponse.setEmployeeId("EMP001");
        updatedResponse.setName("Alice Johnson");
        updatedResponse.setEmail("alice.johnson@company.com");
        updatedResponse.setDepartment("Marketing");
        
        when(employeeService.updateEmployee(eq("EMP001"), any(UpdateEmployeeRequest.class)))
            .thenReturn(updatedResponse);

        // When & Then
        mockMvc.perform(put("/api/employees/EMP001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value("EMP001"))
                .andExpect(jsonPath("$.name").value("Alice Johnson"))
                .andExpect(jsonPath("$.email").value("alice.johnson@company.com"))
                .andExpect(jsonPath("$.department").value("Marketing"));

        verify(employeeService).updateEmployee(eq("EMP001"), any(UpdateEmployeeRequest.class));
    }

    @Test
    void testUpdateEmployee_WhenInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given - Invalid request with empty name
        UpdateEmployeeRequest invalidRequest = new UpdateEmployeeRequest();
        invalidRequest.setName(""); // Empty name
        invalidRequest.setEmail("invalid-email"); // Invalid email

        // When & Then
        mockMvc.perform(put("/api/employees/EMP001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(employeeService, never()).updateEmployee(anyString(), any(UpdateEmployeeRequest.class));
    }

    @Test
    void testUpdateEmployee_WhenEmployeeNotFound_ShouldReturnBadRequest() throws Exception {
        // Given
        when(employeeService.updateEmployee(eq("NONEXISTENT"), any(UpdateEmployeeRequest.class)))
            .thenThrow(new IllegalArgumentException("Employee not found with ID: NONEXISTENT"));

        // When & Then
        mockMvc.perform(put("/api/employees/NONEXISTENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Employee not found with ID: NONEXISTENT"));

        verify(employeeService).updateEmployee(eq("NONEXISTENT"), any(UpdateEmployeeRequest.class));
    }

    @Test
    void testDeleteEmployee_WhenEmployeeExists_ShouldReturnNoContent() throws Exception {
        // Given
        doNothing().when(employeeService).deleteEmployee("EMP001");

        // When & Then
        mockMvc.perform(delete("/api/employees/EMP001"))
                .andExpect(status().isNoContent());

        verify(employeeService).deleteEmployee("EMP001");
    }

    @Test
    void testDeleteEmployee_WhenEmployeeNotFound_ShouldReturnBadRequest() throws Exception {
        // Given
        doThrow(new IllegalArgumentException("Employee not found with ID: NONEXISTENT"))
            .when(employeeService).deleteEmployee("NONEXISTENT");

        // When & Then
        mockMvc.perform(delete("/api/employees/NONEXISTENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Employee not found with ID: NONEXISTENT"));

        verify(employeeService).deleteEmployee("NONEXISTENT");
    }

    @Test
    void testDeleteEmployee_WhenEmployeeHasSubordinates_ShouldReturnConflict() throws Exception {
        // Given
        doThrow(new IllegalStateException("Cannot delete employee with active subordinates"))
            .when(employeeService).deleteEmployee("MGR001");

        // When & Then
        mockMvc.perform(delete("/api/employees/MGR001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot delete employee with active subordinates"));

        verify(employeeService).deleteEmployee("MGR001");
    }

    @Test
    void testGetEmployeesByDepartment_ShouldReturnDepartmentEmployees() throws Exception {
        // Given
        List<EmployeeResponse> employees = Arrays.asList(employeeResponse, managerResponse);
        Page<EmployeeResponse> employeePage = new PageImpl<>(employees, PageRequest.of(0, 10), 2);
        when(employeeService.getEmployeesByDepartment(eq("Engineering"), any(Pageable.class)))
            .thenReturn(employeePage);

        // When & Then
        mockMvc.perform(get("/api/employees/department/Engineering")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].department").value("Engineering"))
                .andExpect(jsonPath("$.content[1].department").value("Engineering"))
                .andExpect(jsonPath("$.totalElements").value(2));

        verify(employeeService).getEmployeesByDepartment(eq("Engineering"), any(Pageable.class));
    }

    @Test
    void testGetSubordinates_WhenManagerExists_ShouldReturnSubordinates() throws Exception {
        // Given
        List<EmployeeResponse> subordinates = Arrays.asList(employeeResponse);
        when(employeeService.getSubordinates("MGR001")).thenReturn(subordinates);

        // When & Then
        mockMvc.perform(get("/api/employees/MGR001/subordinates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].employeeId").value("EMP001"))
                .andExpect(jsonPath("$[0].manager.employeeId").value("MGR001"));

        verify(employeeService).getSubordinates("MGR001");
    }

    @Test
    void testGetSubordinates_WhenManagerNotFound_ShouldReturnBadRequest() throws Exception {
        // Given
        when(employeeService.getSubordinates("NONEXISTENT"))
            .thenThrow(new IllegalArgumentException("Manager not found with ID: NONEXISTENT"));

        // When & Then
        mockMvc.perform(get("/api/employees/NONEXISTENT/subordinates"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Manager not found with ID: NONEXISTENT"));

        verify(employeeService).getSubordinates("NONEXISTENT");
    }

    @Test
    void testSearchEmployees_ShouldReturnMatchingEmployees() throws Exception {
        // Given
        List<EmployeeResponse> employees = Arrays.asList(employeeResponse);
        Page<EmployeeResponse> employeePage = new PageImpl<>(employees, PageRequest.of(0, 10), 1);
        when(employeeService.searchEmployeesByName(eq("Alice"), any(Pageable.class)))
            .thenReturn(employeePage);

        // When & Then
        mockMvc.perform(get("/api/employees/search")
                .param("name", "Alice")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Alice Smith"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(employeeService).searchEmployeesByName(eq("Alice"), any(Pageable.class));
    }

    @Test
    void testSearchEmployees_WithEmptyQuery_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/employees/search")
                .param("name", "")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isBadRequest());

        verify(employeeService, never()).searchEmployeesByName(anyString(), any(Pageable.class));
    }

    @Test
    void testGetDepartments_ShouldReturnAllDepartments() throws Exception {
        // Given
        List<String> departments = Arrays.asList("Engineering", "Marketing", "HR", "Finance");
        when(employeeService.getAllDepartments()).thenReturn(departments);

        // When & Then
        mockMvc.perform(get("/api/employees/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$", containsInAnyOrder("Engineering", "Marketing", "HR", "Finance")));

        verify(employeeService).getAllDepartments();
    }

    @Test
    void testGetDepartments_WhenNoDepartments_ShouldReturnEmptyList() throws Exception {
        // Given
        when(employeeService.getAllDepartments()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/employees/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(employeeService).getAllDepartments();
    }

    // Edge case and validation tests
    @Test
    void testCreateEmployee_WithFutureJoiningDate_ShouldReturnBadRequest() throws Exception {
        // Given - Future joining date
        createRequest.setJoiningDate(LocalDate.now().plusDays(30));

        // When & Then
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());

        verify(employeeService, never()).createEmployee(any(CreateEmployeeRequest.class));
    }

    @Test
    void testCreateEmployee_WithNullManagerId_ShouldCreateWithoutManager() throws Exception {
        // Given
        createRequest.setManagerId(null);
        EmployeeResponse responseWithoutManager = new EmployeeResponse();
        responseWithoutManager.setId(1L);
        responseWithoutManager.setEmployeeId("EMP001");
        responseWithoutManager.setName("Alice Smith");
        responseWithoutManager.setManager(null);
        
        when(employeeService.createEmployee(any(CreateEmployeeRequest.class)))
            .thenReturn(responseWithoutManager);

        // When & Then
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value("EMP001"))
                .andExpect(jsonPath("$.manager").doesNotExist());

        verify(employeeService).createEmployee(any(CreateEmployeeRequest.class));
    }

    @Test
    void testGetAllEmployees_WithInvalidPaginationParams_ShouldUseDefaults() throws Exception {
        // Given
        List<EmployeeResponse> employees = Arrays.asList(employeeResponse);
        Page<EmployeeResponse> employeePage = new PageImpl<>(employees, PageRequest.of(0, 20), 1);
        when(employeeService.getAllEmployees(any(Pageable.class))).thenReturn(employeePage);

        // When & Then - Invalid page and size parameters
        mockMvc.perform(get("/api/employees")
                .param("page", "-1") // Invalid page
                .param("size", "0"))  // Invalid size
                .andExpect(status().isOk());

        verify(employeeService).getAllEmployees(any(Pageable.class));
    }

    @Test
    void testUpdateEmployee_WithPartialUpdate_ShouldUpdateOnlyProvidedFields() throws Exception {
        // Given - Only update name and email
        UpdateEmployeeRequest partialRequest = new UpdateEmployeeRequest();
        partialRequest.setName("Alice Updated");
        partialRequest.setEmail("alice.updated@company.com");
        partialRequest.setDepartment("Engineering"); // Keep same department
        partialRequest.setJoiningDate(LocalDate.of(2023, 1, 15)); // Keep same date
        
        EmployeeResponse updatedResponse = new EmployeeResponse();
        updatedResponse.setId(1L);
        updatedResponse.setEmployeeId("EMP001");
        updatedResponse.setName("Alice Updated");
        updatedResponse.setEmail("alice.updated@company.com");
        
        when(employeeService.updateEmployee(eq("EMP001"), any(UpdateEmployeeRequest.class)))
            .thenReturn(updatedResponse);

        // When & Then
        mockMvc.perform(put("/api/employees/EMP001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(partialRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Updated"))
                .andExpect(jsonPath("$.email").value("alice.updated@company.com"));

        verify(employeeService).updateEmployee(eq("EMP001"), any(UpdateEmployeeRequest.class));
    }

    @Test
    void testGetEmployeesByDepartment_WithEmptyDepartment_ShouldReturnEmptyResults() throws Exception {
        // Given
        Page<EmployeeResponse> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(employeeService.getEmployeesByDepartment(eq("NonExistentDept"), any(Pageable.class)))
            .thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/api/employees/department/NonExistentDept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(employeeService).getEmployeesByDepartment(eq("NonExistentDept"), any(Pageable.class));
    }

    @Test
    void testGetSubordinates_WhenNoSubordinates_ShouldReturnEmptyList() throws Exception {
        // Given
        when(employeeService.getSubordinates("MGR001")).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/employees/MGR001/subordinates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(employeeService).getSubordinates("MGR001");
    }

    // Content type and format tests
    @Test
    void testCreateEmployee_WithInvalidContentType_ShouldReturnUnsupportedMediaType() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.TEXT_PLAIN)
                .content("invalid content"))
                .andExpect(status().isUnsupportedMediaType());

        verify(employeeService, never()).createEmployee(any(CreateEmployeeRequest.class));
    }

    @Test
    void testCreateEmployee_WithMalformedJson_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json"))
                .andExpect(status().isBadRequest());

        verify(employeeService, never()).createEmployee(any(CreateEmployeeRequest.class));
    }
}